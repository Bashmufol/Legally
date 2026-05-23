package com.legally.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legally.config.LegallyProperties;
import com.legally.model.LawChunk;
import com.legally.model.dto.ConsultRequest;
import com.legally.model.dto.GeminiLegalResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.*;

@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    private static final String DISCLAIMER =
            "Legally provides general legal information only, not legal advice. Consult a licensed Nigerian lawyer for your specific case.";

    private final LegallyProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final StorageService storageService;

    public GeminiService(
            LegallyProperties properties,
            RestClient restClient,
            ObjectMapper objectMapper,
            StorageService storageService) {
        this.properties = properties;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.storageService = storageService;
    }

    public GeminiLegalResponse analyze(
            String userMessage,
            String scenario,
            List<LawChunk> chunks,
            List<ConsultRequest.MediaRef> media) throws Exception {

        String apiKey = properties.getGemini().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return fallbackResponse(userMessage, chunks);
        }

        String prompt = buildUserPrompt(userMessage, scenario, chunks);
        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of("text", prompt));

        if (media != null) {
            for (ConsultRequest.MediaRef ref : media) {
                attachMedia(parts, ref);
            }
        }

        Map<String, Object> body = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", systemInstruction()))),
                "contents", List.of(Map.of("role", "user", "parts", parts)),
                "generationConfig", Map.of(
                        "temperature", 0.2,
                        "responseMimeType", "application/json"
                )
        );

        try {
            String responseBody = callGemini(apiKey, body);
            return parseResponse(responseBody, chunks);
        } catch (ResourceAccessException e) {
            log.warn("Gemini API unreachable (network/DNS): {}", e.getMessage());
            return networkFallbackResponse(userMessage, chunks);
        } catch (RestClientResponseException e) {
            log.warn("Gemini API error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode().value() == 404) {
                return networkFallbackResponse(userMessage, chunks,
                        "Gemini model not found. Set GEMINI_MODEL=gemini-2.0-flash in backend/.env and restart.");
            }
            throw e;
        }
    }

    public String generateDemandLetter(String facts, String scenario, List<LawChunk> chunks) throws Exception {
        String apiKey = properties.getGemini().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return defaultDemandLetter(facts);
        }

        String prompt = """
                Draft a formal pre-action demand letter for a Nigerian %s dispute.
                Use plain formal English. Include: date placeholder, parties, facts, legal basis from corpus only, demanded remedy, 14-day deadline, and signature block.
                Facts: %s

                Corpus excerpts:
                %s
                """.formatted(scenario, facts, formatChunks(chunks));

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of("temperature", 0.3)
        );

        try {
            String responseBody = callGemini(apiKey, body);
            JsonNode root = objectMapper.readTree(responseBody);
            return root.path("candidates").path(0).path("content").path("parts").path(0).path("text")
                    .asText(defaultDemandLetter(facts));
        } catch (ResourceAccessException e) {
            log.warn("Gemini unreachable for demand letter: {}", e.getMessage());
            return defaultDemandLetter(facts);
        }
    }

    private String callGemini(String apiKey, Map<String, Object> body) {
        String model = properties.getGemini().getModel();
        if (model == null || model.isBlank()) {
            model = "gemini-2.0-flash";
        }
        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + model + ":generateContent?key=" + apiKey;

        return restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);
    }

    private void attachMedia(List<Map<String, Object>> parts, ConsultRequest.MediaRef ref) {
        try {
            byte[] bytes = storageService.readBytes(ref.getUrl(), ref.getStorageType());
            String mime = ref.getMimeType() != null ? ref.getMimeType() : "application/octet-stream";
            String base64 = Base64.getEncoder().encodeToString(bytes);
            parts.add(Map.of("inlineData", Map.of("mimeType", mime, "data", base64)));
        } catch (Exception e) {
            parts.add(Map.of("text", "[Could not load attached media: " + ref.getUrl() + "]"));
        }
    }

    private GeminiLegalResponse parseResponse(String responseBody, List<LawChunk> chunks) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        String text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText("");
        text = text.replace("```json", "").replace("```", "").trim();
        GeminiLegalResponse parsed = objectMapper.readValue(text, GeminiLegalResponse.class);
        if (parsed.getDisclaimer() == null || parsed.getDisclaimer().isBlank()) {
            parsed.setDisclaimer(DISCLAIMER);
        }
        validateCitations(parsed, chunks);
        return parsed;
    }

    private void validateCitations(GeminiLegalResponse parsed, List<LawChunk> chunks) {
        Set<String> validIds = new HashSet<>();
        Map<String, LawChunk> byInstrument = new HashMap<>();
        for (LawChunk c : chunks) {
            validIds.add(c.getId());
            byInstrument.put(c.getInstrument() + "|" + c.getSection(), c);
        }

        List<GeminiLegalResponse.LegalPoint> filtered = new ArrayList<>();
        for (GeminiLegalResponse.LegalPoint point : parsed.getLegalAnalysis()) {
            if (point.getChunkId() != null && validIds.contains(point.getChunkId())) {
                filtered.add(point);
                continue;
            }
            String key = point.getCitation().getInstrument() + "|" + point.getCitation().getSection();
            if (byInstrument.containsKey(key)) {
                point.setChunkId(byInstrument.get(key).getId());
                filtered.add(point);
            }
        }
        parsed.setLegalAnalysis(filtered);
    }

    private GeminiLegalResponse networkFallbackResponse(String userMessage, List<LawChunk> chunks) {
        return networkFallbackResponse(userMessage, chunks,
                "Could not reach Google Gemini (network or DNS). Showing corpus-based guidance. "
                        + "Check your internet connection, DNS, firewall/VPN, then retry.");
    }

    private GeminiLegalResponse networkFallbackResponse(
            String userMessage, List<LawChunk> chunks, String summaryPrefix) {
        GeminiLegalResponse r = fallbackResponse(userMessage, chunks);
        r.setSummary(summaryPrefix + " " + r.getSummary());
        r.setConfidence("low");
        return r;
    }

    private GeminiLegalResponse fallbackResponse(String userMessage, List<LawChunk> chunks) {
        GeminiLegalResponse r = new GeminiLegalResponse();
        r.setSummary("Below is information from the Legally legal corpus relevant to your query.");
        r.setDisclaimer(DISCLAIMER);
        r.setConfidence("medium");
        r.setSuggestedContactTags(List.of("legal_aid"));

        List<GeminiLegalResponse.LegalPoint> points = new ArrayList<>();
        for (LawChunk c : chunks.stream().limit(4).toList()) {
            GeminiLegalResponse.LegalPoint p = new GeminiLegalResponse.LegalPoint();
            p.setPoint(c.getTitle() + ": " + abbreviate(c.getText(), 200));
            p.setChunkId(c.getId());
            GeminiLegalResponse.Citation cit = new GeminiLegalResponse.Citation();
            cit.setInstrument(c.getInstrument());
            cit.setSection(c.getSection());
            cit.setJurisdiction(c.getJurisdiction());
            p.setCitation(cit);
            points.add(p);
        }
        r.setLegalAnalysis(points);
        r.setSteps(List.of(
                "Document everything (dates, names, photos, videos).",
                "Send a written complaint to the relevant authority.",
                "Seek Legal Aid or a qualified lawyer if the matter escalates.",
                "Do not sign documents or pay bribes under pressure."
        ));

        String lower = userMessage.toLowerCase(Locale.ROOT);
        if (lower.contains("police") || lower.contains("phone")) {
            r.setSuggestedContactTags(List.of("police", "legal_aid"));
        } else if (lower.contains("rent") || lower.contains("tenant")) {
            r.setSuggestedContactTags(List.of("tenancy", "legal_aid", "tenant"));
            r.setDemandLetterEligible(true);
        } else if (lower.contains("land")) {
            r.setSuggestedContactTags(List.of("land", "kwara_government"));
        }

        return r;
    }

    private String systemInstruction() {
        return """
                You are Legally, a Nigerian legal INFORMATION assistant (not a lawyer).
                Rules:
                1. Only cite laws from the provided retrievedChunks JSON. Each legal point MUST include chunkId matching a chunk id.
                2. Use plain English accessible to non-lawyers.
                3. Never invent phone numbers or contacts — only return suggestedContactTags from: police, tenant, tenancy, land, legal_aid, kwara_government, fundamental_rights.
                4. Set demandLetterEligible true only for tenancy/contract disputes.
                5. Output ONLY valid JSON matching this schema:
                {
                  "summary": "string",
                  "legalAnalysis": [{"point":"string","chunkId":"string","citation":{"instrument":"string","section":"string","jurisdiction":"FEDERAL|KWARA"}}],
                  "steps": ["string"],
                  "suggestedContactTags": ["string"],
                  "demandLetterEligible": false,
                  "confidence": "high|medium|low",
                  "disclaimer": "string"
                }
                """;
    }

    private String buildUserPrompt(String userMessage, String scenario, List<LawChunk> chunks) {
        return "Scenario: " + (scenario != null ? scenario : "general")
                + "\nUser message: " + userMessage
                + "\n\nretrievedChunks:\n" + formatChunks(chunks);
    }

    private String formatChunks(List<LawChunk> chunks) {
        StringBuilder sb = new StringBuilder();
        for (LawChunk c : chunks) {
            sb.append("- id: ").append(c.getId())
                    .append(" | ").append(c.getJurisdiction())
                    .append(" | ").append(c.getInstrument())
                    .append(" ").append(c.getSection())
                    .append("\n  ").append(c.getText()).append("\n");
        }
        return sb.toString();
    }

    private String abbreviate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }

    private String defaultDemandLetter(String facts) {
        return """
                [DATE]

                To: [LANDLORD / COUNTERPARTY NAME]
                From: [YOUR FULL NAME]
                Re: FORMAL DEMAND — Tenancy / Contract Breach

                Dear Sir/Madam,

                I write regarding the following facts:
                %s

                Your conduct constitutes a breach of our agreement and applicable Nigerian law. I demand that you [specify remedy: revert unlawful rent increase / cease eviction threats / remedy breach] within FOURTEEN (14) days of receipt of this letter.

                If you fail to comply, I will pursue all available civil remedies without further notice, including court action and complaints to relevant authorities.

                Yours faithfully,

                [YOUR NAME]
                [PHONE / EMAIL]

                ---
                %s
                """.formatted(facts, DISCLAIMER);
    }
}
