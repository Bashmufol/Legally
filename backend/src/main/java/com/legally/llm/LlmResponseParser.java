package com.legally.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legally.model.JurisdictionContext;
import com.legally.model.LawChunk;
import com.legally.model.dto.GeminiLegalResponse;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

public final class LlmResponseParser {

    private LlmResponseParser() {
    }

    public static boolean containsJsonObject(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return false;
        }
        String text = rawText.replace("```json", "").replace("```", "").trim();
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        return start >= 0 && end > start;
    }

    public static GeminiLegalResponse parseJsonResponse(
            ObjectMapper objectMapper, String rawText, JurisdictionContext jurisdiction) throws Exception {
        String text = rawText.replace("```json", "").replace("```", "").trim();
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("Response contained no JSON object");
        }
        text = text.substring(start, end + 1);
        GeminiLegalResponse parsed = objectMapper.readValue(text, GeminiLegalResponse.class);
        if (parsed.getDisclaimer() == null || parsed.getDisclaimer().isBlank()) {
            parsed.setDisclaimer(defaultDisclaimer(jurisdiction));
        }
        return parsed;
    }

    public static List<LawChunk> sourcesFromCitations(
            GeminiLegalResponse response, JurisdictionContext jurisdiction, String providerPrefix) {
        String country = jurisdiction.getCountryCode() != null && !jurisdiction.getCountryCode().isBlank()
                ? jurisdiction.getCountryCode().toUpperCase(Locale.ROOT)
                : "UN";
        String region = jurisdiction.getRegionCode() != null
                ? jurisdiction.getRegionCode().toUpperCase(Locale.ROOT)
                : "GENERAL";

        List<LawChunk> chunks = new ArrayList<>();
        int i = 0;
        for (GeminiLegalResponse.LegalPoint point : response.getLegalAnalysis()) {
            if (point.getCitation() == null || point.getCitation().getSourceUrl() == null
                    || point.getCitation().getSourceUrl().isBlank()) {
                continue;
            }
            String url = point.getCitation().getSourceUrl();
            LawChunk c = new LawChunk();
            c.setId(providerPrefix + "-" + i++);
            c.setCountryCode(country);
            c.setRegionCode(region);
            c.setJurisdiction(country);
            c.setInstrument(point.getCitation().getInstrument());
            c.setSection(point.getCitation().getSection());
            c.setTitle(point.getCitation().getInstrument());
            c.setText(point.getPoint());
            c.setSourceUrl(url);
            chunks.add(c);
        }
        return chunks;
    }

    public static boolean hasSubstantiveLegalContent(GeminiLegalResponse ai) {
        if (ai == null || ai.getLegalAnalysis() == null || ai.getLegalAnalysis().isEmpty()) {
            return false;
        }
        return ai.getLegalAnalysis().stream().anyMatch(point -> {
            if (point.getPoint() != null && !point.getPoint().isBlank()) {
                return true;
            }
            return point.getCitation() != null
                    && point.getCitation().getSourceUrl() != null
                    && !point.getCitation().getSourceUrl().isBlank();
        });
    }

    private static String defaultDisclaimer(JurisdictionContext jurisdiction) {
        return "Legally provides general legal information only, not legal advice. "
                + "Consult a licensed lawyer in " + jurisdiction.getCountryName() + " for your specific case.";
    }

    public static String stableChunkId(String url) {
        byte[] hash = url.getBytes(StandardCharsets.UTF_8);
        String hex = HexFormat.of().formatHex(hash, 0, Math.min(8, hash.length));
        return "src-" + hex;
    }
}
