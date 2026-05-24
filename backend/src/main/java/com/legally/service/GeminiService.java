package com.legally.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legally.config.LegallyProperties;
import com.legally.model.JurisdictionContext;
import com.legally.model.LawChunk;
import com.legally.model.LegalDocumentType;
import com.legally.model.WebLegalSource;
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
            JurisdictionContext jurisdiction,
            List<LawChunk> chunks,
            List<ConsultRequest.MediaRef> media) throws Exception {

        String apiKey = properties.getGemini().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return fallbackResponse(userMessage, jurisdiction, chunks);
        }

        String prompt = buildUserPrompt(userMessage, scenario, jurisdiction, chunks);
        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of("text", prompt));

        if (media != null) {
            for (ConsultRequest.MediaRef ref : media) {
                attachMedia(parts, ref);
            }
        }

        Map<String, Object> body = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", systemInstruction(jurisdiction)))),
                "contents", List.of(Map.of("role", "user", "parts", parts)),
                "generationConfig", Map.of(
                        "temperature", 0.2,
                        "responseMimeType", "application/json"
                )
        );

        try {
            String responseBody = callGemini(apiKey, body);
            return parseResponse(responseBody, jurisdiction, chunks);
        } catch (ResourceAccessException e) {
            log.warn("Gemini API unreachable (network/DNS): {}", e.getMessage());
            return networkFallbackResponse(userMessage, jurisdiction, chunks);
        } catch (RestClientResponseException e) {
            log.warn("Gemini API error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode().value() == 404) {
                return networkFallbackResponse(userMessage, jurisdiction, chunks,
                        "Gemini model not found. Set GEMINI_MODEL=gemini-2.0-flash in backend/.env and restart.");
            }
            throw e;
        }
    }

    /**
     * Non-Nigeria path: summarize only from backend-filtered official web excerpts.
     */
    public GeminiLegalResponse analyzeFromWebSources(
            String userMessage,
            String scenario,
            JurisdictionContext jurisdiction,
            List<WebLegalSource> webSources,
            List<ConsultRequest.MediaRef> media) throws Exception {

        String apiKey = properties.getGemini().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return webFallbackResponse(userMessage, jurisdiction, webSources);
        }

        String prompt = buildWebUserPrompt(userMessage, scenario, jurisdiction, webSources);
        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of("text", prompt));
        if (media != null) {
            for (ConsultRequest.MediaRef ref : media) {
                attachMedia(parts, ref);
            }
        }

        Map<String, Object> body = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", webSystemInstruction(jurisdiction)))),
                "contents", List.of(Map.of("role", "user", "parts", parts)),
                "generationConfig", Map.of(
                        "temperature", 0.2,
                        "responseMimeType", "application/json"
                )
        );

        try {
            String responseBody = callGemini(apiKey, body);
            return parseWebResponse(responseBody, jurisdiction, webSources);
        } catch (ResourceAccessException e) {
            log.warn("Gemini API unreachable for web research: {}", e.getMessage());
            return webFallbackResponse(userMessage, jurisdiction, webSources);
        }
    }

    /**
     * One-line question for history when the user only submitted voice or uploads.
     */
    public String extractQuestionFromMedia(List<ConsultRequest.MediaRef> media) throws Exception {
        String apiKey = properties.getGemini().getApiKey();
        if (apiKey == null || apiKey.isBlank() || media == null || media.isEmpty()) {
            return "";
        }

        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of("text", """
                The user submitted media without typing a question. In ONE short sentence (max 25 words), \
                state the legal question or situation they are asking about. Output plain text only, no quotes."""));
        for (ConsultRequest.MediaRef ref : media) {
            attachMedia(parts, ref);
        }

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("role", "user", "parts", parts)),
                "generationConfig", Map.of("temperature", 0.1, "maxOutputTokens", 80));

        try {
            String responseBody = callGemini(apiKey, body);
            JsonNode root = objectMapper.readTree(responseBody);
            String text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText("");
            return text != null ? text.trim() : "";
        } catch (Exception e) {
            log.warn("Could not extract question from media: {}", e.getMessage());
            return "";
        }
    }

    /**
     * Fallback when SerpApi returns no hits: Gemini Google Search grounding with strict official-source rules.
     */
    public GeminiLegalResponse analyzeWithGoogleSearchGrounding(
            String userMessage,
            String scenario,
            JurisdictionContext jurisdiction,
            List<ConsultRequest.MediaRef> media) throws Exception {

        String apiKey = properties.getGemini().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            GeminiLegalResponse r = new GeminiLegalResponse();
            r.setSummary("Web research is not available. Configure GEMINI_API_KEY and SERPAPI_API_KEY for "
                    + jurisdiction.getCountryName() + ".");
            r.setDisclaimer(webDisclaimer(jurisdiction));
            r.setConfidence("low");
            r.setSuggestedContactTags(List.of("legal_aid"));
            return r;
        }

        String prompt = buildWebGroundingUserPrompt(userMessage, scenario, jurisdiction);
        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of("text", prompt));
        if (media != null) {
            for (ConsultRequest.MediaRef ref : media) {
                attachMedia(parts, ref);
            }
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("systemInstruction", Map.of("parts", List.of(Map.of("text", webGroundingSystemInstruction(jurisdiction)))));
        body.put("contents", List.of(Map.of("role", "user", "parts", parts)));
        body.put("tools", List.of(Map.of("google_search", Map.of())));
        body.put("generationConfig", Map.of(
                "temperature", 0.2,
                "responseMimeType", "application/json"
        ));

        try {
            String responseBody = callGemini(apiKey, body);
            GeminiLegalResponse parsed = parseWebResponse(responseBody, jurisdiction, List.of());
            parsed.setDisclaimer(webDisclaimer(jurisdiction) + " Sources retrieved via Google Search; verify URLs.");
            if (parsed.getConfidence() == null || parsed.getConfidence().isBlank()) {
                parsed.setConfidence("medium");
            }
            return parsed;
        } catch (Exception e) {
            log.warn("Gemini google_search grounding failed: {}", e.getMessage());
            GeminiLegalResponse r = new GeminiLegalResponse();
            r.setSummary("Could not complete web research for " + jurisdiction.displayLabel()
                    + ". Configure SERPAPI_API_KEY or retry later.");
            r.setDisclaimer(webDisclaimer(jurisdiction));
            r.setConfidence("low");
            r.setSuggestedContactTags(List.of("legal_aid"));
            return r;
        }
    }

    public Optional<JurisdictionContext> detectJurisdictionFromInputs(
            String userMessage,
            List<ConsultRequest.MediaRef> media,
            JurisdictionContext deviceBaseline) throws Exception {
        String apiKey = properties.getGemini().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }

        boolean hasMessage = userMessage != null && !userMessage.isBlank();
        boolean hasMedia = media != null && !media.isEmpty();
        if (!hasMessage && !hasMedia) {
            return Optional.empty();
        }

        String prompt = """
                Determine if the user explicitly wants legal information under a specific country or state/province.
                Device location (default only when nothing explicit in inputs): %s, %s (code %s / %s).

                Inspect the user message AND any attached files (images, PDFs, audio, video).
                Examples: "under French law", "According to Nigerian law", "in California", "German inheritance rules",
                a lease or document naming a jurisdiction, spoken mention of a country in audio/video.

                If a specific country (and optional state/province) is clearly requested, return ONLY JSON:
                {"explicit":true,"countryCode":"ISO3166-1-alpha-2","countryName":"Full name","regionCode":"REGION_CODE","regionName":"Region name or General"}

                If the question is generic with no country/state specified, return ONLY: {"explicit":false}

                User message:
                %s
                """.formatted(
                deviceBaseline.getCountryName(),
                deviceBaseline.getRegionName(),
                deviceBaseline.getCountryCode(),
                deviceBaseline.getRegionCode(),
                hasMessage ? userMessage : "(no text; rely on attached files only)");

        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of("text", prompt));
        if (hasMedia) {
            for (ConsultRequest.MediaRef ref : media) {
                attachMedia(parts, ref);
            }
        }

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("role", "user", "parts", parts)),
                "generationConfig", Map.of(
                        "temperature", 0.0,
                        "responseMimeType", "application/json"
                )
        );

        try {
            String responseBody = callGemini(apiKey, body);
            JsonNode root = objectMapper.readTree(responseBody);
            String text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText("");
            text = text.replace("```json", "").replace("```", "").trim();
            JsonNode parsed = objectMapper.readTree(text);
            if (!parsed.path("explicit").asBoolean(false)) {
                return Optional.empty();
            }
            JurisdictionContext ctx = new JurisdictionContext();
            ctx.setCountryCode(parsed.path("countryCode").asText("INT").toUpperCase(Locale.ROOT));
            ctx.setCountryName(parsed.path("countryName").asText(ctx.getCountryCode()));
            String regionCode = parsed.path("regionCode").asText("GENERAL");
            String regionName = parsed.path("regionName").asText("General");
            ctx.setRegionCode(regionCode.toUpperCase(Locale.ROOT));
            ctx.setRegionName(regionName);
            return Optional.of(ctx);
        } catch (Exception e) {
            log.warn("Gemini jurisdiction detection failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public String generateDemandLetter(
            String facts, String scenario, JurisdictionContext jurisdiction, List<LawChunk> chunks) throws Exception {
        return generateLegalDocument(
                LegalDocumentType.DEMAND_LETTER,
                LegalDocumentType.DEMAND_LETTER.getDisplayName(),
                facts,
                null,
                null,
                null,
                jurisdiction,
                chunks);
    }

    public String generateLegalDocument(
            LegalDocumentType documentType,
            String title,
            String facts,
            String additionalDetails,
            String partyAName,
            String partyBName,
            JurisdictionContext jurisdiction,
            List<LawChunk> chunks) throws Exception {
        String apiKey = properties.getGemini().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return fallbackLegalDocument(documentType, title, facts, jurisdiction);
        }

        String prompt = buildLegalDocumentPrompt(
                documentType,
                title,
                facts,
                additionalDetails,
                partyAName,
                partyBName,
                jurisdiction,
                chunks);

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of("temperature", 0.25)
        );

        try {
            String responseBody = callGemini(apiKey, body);
            JsonNode root = objectMapper.readTree(responseBody);
            String text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text")
                    .asText("");
            if (text.isBlank()) {
                return fallbackLegalDocument(documentType, title, facts, jurisdiction);
            }
            return stripCodeFences(text);
        } catch (ResourceAccessException e) {
            log.warn("Gemini unreachable for legal document: {}", e.getMessage());
            return fallbackLegalDocument(documentType, title, facts, jurisdiction);
        }
    }

    private String buildLegalDocumentPrompt(
            LegalDocumentType documentType,
            String title,
            String facts,
            String additionalDetails,
            String partyAName,
            String partyBName,
            JurisdictionContext jurisdiction,
            List<LawChunk> chunks) {
        String docInstructions = documentInstructions(documentType);
        String parties = "";
        if (partyAName != null && !partyAName.isBlank()) {
            parties += "Party A / first party: " + partyAName + "\n";
        }
        if (partyBName != null && !partyBName.isBlank()) {
            parties += "Party B / second party: " + partyBName + "\n";
        }
        String extras = additionalDetails != null && !additionalDetails.isBlank()
                ? "\nAdditional requirements:\n" + additionalDetails
                : "";

        return """
                You are Legally, drafting a formal legal document template (NOT legal advice).
                
                Document type: %s
                Document title: %s
                Jurisdiction: %s, %s (country code %s, region %s)
                
                %s
                
                Rules:
                1. Draft the complete document in clear formal English appropriate for the stated jurisdiction.
                2. Ground obligations and rights in the provided corpus excerpts where relevant; cite section references inline in parentheses.
                3. Use placeholders in square brackets only where user-specific data is missing (e.g. [PARTY A NAME], [DATE], [PROPERTY ADDRESS]).
                4. Include standard sections: title, parties, recitals/background, operative terms, signatures, witness lines if customary.
                5. Add a short footer line: "Generated by Legally. Review with a licensed lawyer before execution."
                6. Output ONLY the document text (no JSON, no markdown code fences).
                
                %s
                User facts and requirements:
                %s
                %s
                
                Corpus excerpts (use for legal grounding):
                %s
                """.formatted(
                documentType.name(),
                title,
                jurisdiction.getCountryName(),
                jurisdiction.getRegionName(),
                jurisdiction.getCountryCode(),
                jurisdiction.getRegionCode(),
                docInstructions,
                parties,
                facts,
                extras,
                formatChunks(chunks));
    }

    private String documentInstructions(LegalDocumentType type) {
        return switch (type) {
            case DEMAND_LETTER -> "Structure as a pre-action demand letter: date, parties, facts, legal basis, demanded remedy, deadline, signature.";
            case RENT_AGREEMENT -> "Structure as a residential tenancy/lease: premises, term, rent, deposit, repairs, termination, governing law clause.";
            case LAND_PURCHASE -> "Structure as land sale agreement: property description, purchase price, deposit, title obligations, completion, warranties, dispute resolution.";
            case PRENUPTIAL -> "Structure as prenuptial agreement: parties, disclosure, separate property, spousal support waiver/limitation, estate rights, governing law; note enforceability varies.";
            case EMPLOYMENT_CONTRACT -> "Structure as employment agreement: role, duties, compensation, benefits, confidentiality, termination, notice, governing law.";
            case GENERAL_CONTRACT -> "Structure as a general commercial contract with definitions, obligations, payment, term, breach, remedies, governing law.";
            case NDA -> "Structure as mutual or unilateral NDA: confidential information definition, obligations, exclusions, term, remedies.";
            case POWER_OF_ATTORNEY -> "Structure as limited or general power of attorney appropriate to the jurisdiction; include scope, duration, revocation.";
            case AFFIDAVIT -> "Structure as affidavit/sworn statement: deponent details, numbered paragraphs of facts, oath block, signature.";
            case OTHER -> "Draft the custom legal document described by the user with appropriate formal structure for the jurisdiction.";
        };
    }

    private String stripCodeFences(String text) {
        return text.replace("```", "").trim();
    }

    private String fallbackLegalDocument(
            LegalDocumentType documentType, String title, String facts, JurisdictionContext jurisdiction) {
        if (documentType == LegalDocumentType.DEMAND_LETTER) {
            return defaultDemandLetter(facts);
        }
        return """
                %s
                Jurisdiction: %s (%s)
                
                [PARTY A NAME] ("Party A")
                [PARTY B NAME] ("Party B")
                
                BACKGROUND
                %s
                
                OPERATIVE TERMS
                1. [Insert key terms based on the facts above and local law.]
                2. This template was generated without AI. Configure GEMINI_API_KEY for a full draft.
                
                GOVERNING LAW
                This agreement shall be governed by the laws of %s.
                
                SIGNATURES
                
                _________________________          _________________________
                Party A                            Party B
                Date: [DATE]                       Date: [DATE]
                
                ---
                Generated by Legally. Review with a licensed lawyer before execution.
                """.formatted(
                title.toUpperCase(Locale.ROOT),
                jurisdiction.displayLabel(),
                documentType.getDisplayName(),
                facts,
                jurisdiction.getCountryName());
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

    private GeminiLegalResponse parseResponse(
            String responseBody, JurisdictionContext jurisdiction, List<LawChunk> chunks) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        String text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText("");
        text = text.replace("```json", "").replace("```", "").trim();
        GeminiLegalResponse parsed = objectMapper.readValue(text, GeminiLegalResponse.class);
        if (parsed.getDisclaimer() == null || parsed.getDisclaimer().isBlank()) {
            parsed.setDisclaimer(defaultDisclaimer(jurisdiction));
        }
        validateCitations(parsed, chunks);
        return parsed;
    }

    private String defaultDisclaimer(JurisdictionContext jurisdiction) {
        return "Legally provides general legal information only, not legal advice. "
                + "Consult a licensed lawyer in " + jurisdiction.getCountryName() + " for your specific case.";
    }

    private GeminiLegalResponse parseWebResponse(
            String responseBody, JurisdictionContext jurisdiction, List<WebLegalSource> webSources)
            throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        String text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText("");
        text = text.replace("```json", "").replace("```", "").trim();
        GeminiLegalResponse parsed = objectMapper.readValue(text, GeminiLegalResponse.class);
        if (parsed.getDisclaimer() == null || parsed.getDisclaimer().isBlank()) {
            parsed.setDisclaimer(webDisclaimer(jurisdiction));
        }
        if (!webSources.isEmpty()) {
            validateWebCitations(parsed, webSources);
            attachWebSourceUrls(parsed, webSources, jurisdiction);
        }
        return parsed;
    }

    private void validateWebCitations(GeminiLegalResponse parsed, List<WebLegalSource> webSources) {
        Set<String> validIds = new HashSet<>();
        Map<String, WebLegalSource> byId = new HashMap<>();
        for (WebLegalSource s : webSources) {
            validIds.add(s.getId());
            byId.put(s.getId(), s);
        }

        List<GeminiLegalResponse.LegalPoint> filtered = new ArrayList<>();
        for (GeminiLegalResponse.LegalPoint point : parsed.getLegalAnalysis()) {
            if (point.getChunkId() != null && validIds.contains(point.getChunkId())) {
                filtered.add(point);
                continue;
            }
            if (point.getCitation() != null && point.getCitation().getSourceUrl() != null) {
                for (WebLegalSource s : webSources) {
                    if (s.getUrl().equals(point.getCitation().getSourceUrl())) {
                        point.setChunkId(s.getId());
                        filtered.add(point);
                        break;
                    }
                }
            }
        }
        parsed.setLegalAnalysis(filtered);
    }

    private void attachWebSourceUrls(
            GeminiLegalResponse parsed, List<WebLegalSource> webSources, JurisdictionContext jurisdiction) {
        Map<String, WebLegalSource> byId = new HashMap<>();
        for (WebLegalSource s : webSources) {
            byId.put(s.getId(), s);
        }
        String jurisdictionLabel = jurisdiction.getCountryCode() != null
                ? jurisdiction.getCountryCode()
                : "WEB";
        for (GeminiLegalResponse.LegalPoint point : parsed.getLegalAnalysis()) {
            if (point.getCitation() == null) {
                point.setCitation(new GeminiLegalResponse.Citation());
            }
            WebLegalSource src = point.getChunkId() != null ? byId.get(point.getChunkId()) : null;
            if (src != null) {
                point.getCitation().setSourceUrl(src.getUrl());
                if (point.getCitation().getInstrument() == null || point.getCitation().getInstrument().isBlank()) {
                    point.getCitation().setInstrument(src.getTitle());
                }
                if (point.getCitation().getSection() == null || point.getCitation().getSection().isBlank()) {
                    point.getCitation().setSection("Official web source");
                }
                if (point.getCitation().getJurisdiction() == null || point.getCitation().getJurisdiction().isBlank()) {
                    point.getCitation().setJurisdiction(jurisdictionLabel);
                }
            }
        }
    }

    private GeminiLegalResponse webFallbackResponse(
            String userMessage, JurisdictionContext jurisdiction, List<WebLegalSource> webSources) {
        GeminiLegalResponse r = new GeminiLegalResponse();
        r.setDisclaimer(webDisclaimer(jurisdiction));
        r.setConfidence("low");
        r.setSuggestedContactTags(List.of("legal_aid"));

        if (webSources.isEmpty()) {
            r.setSummary("No official web sources could be loaded for " + jurisdiction.displayLabel()
                    + ". Configure GEMINI_API_KEY and SERPAPI_API_KEY, or refine your question.");
            r.setSteps(List.of(
                    "Rephrase your question with the country name (e.g. United States).",
                    "Consult a licensed lawyer in " + jurisdiction.getCountryName() + ".",
                    "Use government or court websites directly to verify the law."));
            return r;
        }

        r.setSummary("Summary from official web excerpts for " + jurisdiction.displayLabel()
                + " (AI unavailable).");
        List<GeminiLegalResponse.LegalPoint> points = new ArrayList<>();
        for (WebLegalSource s : webSources.stream().limit(4).toList()) {
            GeminiLegalResponse.LegalPoint p = new GeminiLegalResponse.LegalPoint();
            p.setChunkId(s.getId());
            p.setPoint(abbreviate(s.getExcerpt(), 280));
            GeminiLegalResponse.Citation cit = new GeminiLegalResponse.Citation();
            cit.setInstrument(s.getTitle());
            cit.setSection("Official web source");
            cit.setJurisdiction(jurisdiction.getCountryCode());
            cit.setSourceUrl(s.getUrl());
            p.setCitation(cit);
            points.add(p);
        }
        r.setLegalAnalysis(points);
        r.setSteps(List.of(
                "Verify each linked source on the official website.",
                "Consult a licensed lawyer in " + jurisdiction.getCountryName() + " for your situation.",
                "Document facts and keep copies of any agreements or notices."));
        return r;
    }

    private String webDisclaimer(JurisdictionContext jurisdiction) {
        return "Legally provides general legal information from official web sources only, not legal advice. "
                + "Sources are filtered for government and authoritative sites but may be incomplete or outdated. "
                + "Consult a licensed lawyer in " + jurisdiction.getCountryName() + " for your specific case.";
    }

    private String webSystemInstruction(JurisdictionContext jurisdiction) {
        return """
                You are Legally, a legal INFORMATION assistant (not a lawyer).
                Active jurisdiction: %s (country code %s, region %s).
                Rules:
                1. Answer ONLY using facts from the provided webSources JSON excerpts. Do not invent statutes or cases.
                2. Each legal point MUST include chunkId matching a webSources id and citation.sourceUrl from that source.
                3. If webSources are insufficient, say so clearly and set confidence to low.
                4. Use plain English. Never invent phone numbers. suggestedContactTags: police, tenant, tenancy, land, legal_aid, kwara_government, fundamental_rights.
                5. Output ONLY valid JSON:
                {
                  "summary": "string",
                  "legalAnalysis": [{"point":"string","chunkId":"string","citation":{"instrument":"string","section":"string","jurisdiction":"string","sourceUrl":"string"}}],
                  "steps": ["string"],
                  "suggestedContactTags": ["string"],
                  "demandLetterEligible": false,
                  "confidence": "high|medium|low",
                  "disclaimer": "string"
                }
                """.formatted(
                jurisdiction.displayLabel(),
                jurisdiction.getCountryCode(),
                jurisdiction.getRegionName());
    }

    private String webGroundingSystemInstruction(JurisdictionContext jurisdiction) {
        return """
                You are Legally, a legal INFORMATION assistant (not a lawyer).
                Active jurisdiction: %s (country code %s, region %s).
                Use Google Search to find CURRENT law from OFFICIAL sources only: government (.gov), courts, legislatures, official legal databases.
                Do NOT rely on blogs, forums, or news opinion pieces.
                Rules:
                1. Cite specific sources in citation.sourceUrl when possible.
                2. chunkId may be omitted if no webSources list; always include sourceUrl in citations.
                3. If law varies by state/province, mention that clearly.
                4. Never invent phone numbers. suggestedContactTags: legal_aid, police, tenant, tenancy, land.
                5. Output ONLY valid JSON with the same schema as corpus mode (include sourceUrl in each citation).
                """.formatted(
                jurisdiction.displayLabel(),
                jurisdiction.getCountryCode(),
                jurisdiction.getRegionName());
    }

    private String buildWebUserPrompt(
            String userMessage, String scenario, JurisdictionContext jurisdiction, List<WebLegalSource> webSources) {
        String messageBlock = userMessage != null && !userMessage.isBlank()
                ? userMessage
                : "(No typed message; use attached media if any.)";
        return "Scenario: " + (scenario != null ? scenario : "general")
                + "\nJurisdiction: " + jurisdiction.getCountryName() + " / " + jurisdiction.getRegionName()
                + "\nUser message: " + messageBlock
                + "\n\nwebSources:\n" + formatWebSources(webSources);
    }

    private String buildWebGroundingUserPrompt(
            String userMessage, String scenario, JurisdictionContext jurisdiction) {
        String messageBlock = userMessage != null && !userMessage.isBlank()
                ? userMessage
                : "(No typed message; use attached media if any.)";
        return "Scenario: " + (scenario != null ? scenario : "general")
                + "\nJurisdiction: " + jurisdiction.getCountryName() + " / " + jurisdiction.getRegionName()
                + "\nUser message: " + messageBlock
                + "\n\nSearch for official government and court sources for this jurisdiction only.";
    }

    private String formatWebSources(List<WebLegalSource> sources) {
        StringBuilder sb = new StringBuilder();
        for (WebLegalSource s : sources) {
            sb.append("- id: ").append(s.getId())
                    .append(" | url: ").append(s.getUrl())
                    .append(" | title: ").append(s.getTitle())
                    .append("\n  excerpt: ").append(s.getExcerpt()).append("\n");
        }
        return sb.toString();
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

    private GeminiLegalResponse networkFallbackResponse(
            String userMessage, JurisdictionContext jurisdiction, List<LawChunk> chunks) {
        return networkFallbackResponse(userMessage, jurisdiction, chunks,
                "Could not reach Google Gemini (network or DNS). Showing corpus-based guidance. "
                        + "Check your internet connection, DNS, firewall/VPN, then retry.");
    }

    private GeminiLegalResponse networkFallbackResponse(
            String userMessage, JurisdictionContext jurisdiction, List<LawChunk> chunks, String summaryPrefix) {
        GeminiLegalResponse r = fallbackResponse(userMessage, jurisdiction, chunks);
        r.setSummary(summaryPrefix + " " + r.getSummary());
        r.setConfidence("low");
        return r;
    }

    private GeminiLegalResponse fallbackResponse(
            String userMessage, JurisdictionContext jurisdiction, List<LawChunk> chunks) {
        GeminiLegalResponse r = new GeminiLegalResponse();
        r.setSummary("Below is information from the Legally legal corpus relevant to your query in "
                + jurisdiction.displayLabel() + ".");
        r.setDisclaimer(defaultDisclaimer(jurisdiction));
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

    private String systemInstruction(JurisdictionContext jurisdiction) {
        String corpusNote = jurisdiction.isCorpusLimited()
                ? " The retrieved corpus may be limited for this country; apply general principles from chunks and clearly note when local statutes are not in the corpus."
                : "";
        return """
                You are Legally, a global legal INFORMATION assistant (not a lawyer).
                Active jurisdiction: %s (country code %s, region %s).
                Rules:
                1. Analyze under the active jurisdiction above (already resolved from device location or explicit mentions in text/media).
                2. Only cite laws from the provided retrievedChunks JSON. Each legal point MUST include chunkId matching a chunk id.
                3. Use plain English accessible to non-lawyers. Prefer commas and short sentences; avoid em dashes unless truly needed.
                4. Never invent phone numbers or contacts. Only return suggestedContactTags from: police, tenant, tenancy, land, legal_aid, kwara_government, fundamental_rights.
                5. Set demandLetterEligible true only for tenancy/contract disputes where a demand letter is appropriate.
                6. Output ONLY valid JSON matching this schema:
                {
                  "summary": "string",
                  "legalAnalysis": [{"point":"string","chunkId":"string","citation":{"instrument":"string","section":"string","jurisdiction":"string"}}],
                  "steps": ["string"],
                  "suggestedContactTags": ["string"],
                  "demandLetterEligible": false,
                  "confidence": "high|medium|low",
                  "disclaimer": "string"
                }
                %s
                """.formatted(
                jurisdiction.displayLabel(),
                jurisdiction.getCountryCode(),
                jurisdiction.getRegionName(),
                corpusNote);
    }

    private String buildUserPrompt(
            String userMessage, String scenario, JurisdictionContext jurisdiction, List<LawChunk> chunks) {
        String messageBlock = userMessage != null && !userMessage.isBlank()
                ? userMessage
                : "(No typed message. The user submitted voice recordings and/or uploaded files only. "
                        + "Analyze all attached media to understand their situation and answer fully.)";
        return "Scenario: " + (scenario != null ? scenario : "general")
                + "\nResolved jurisdiction (may be overridden by explicit mentions in message/media): "
                + jurisdiction.getCountryName() + " / " + jurisdiction.getRegionName()
                + " (source: " + jurisdiction.getLocationSource() + ")"
                + "\nUser message: " + messageBlock
                + "\n\nIf attached images, video, or audio mention a location, extract country and region and apply that jurisdiction."
                + "\n\nretrievedChunks:\n" + formatChunks(chunks);
    }

    private String formatChunks(List<LawChunk> chunks) {
        StringBuilder sb = new StringBuilder();
        for (LawChunk c : chunks) {
            sb.append("- id: ").append(c.getId())
                    .append(" | ").append(c.getCountryCode()).append("/").append(c.getRegionCode())
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
                Re: FORMAL DEMAND - Tenancy / Contract Breach

                Dear Sir/Madam,

                I write regarding the following facts:
                %s

                Your conduct constitutes a breach of our agreement and applicable law. I demand that you [specify remedy: revert unlawful rent increase / cease eviction threats / remedy breach] within FOURTEEN (14) days of receipt of this letter.

                If you fail to comply, I will pursue all available civil remedies without further notice, including court action and complaints to relevant authorities.

                Yours faithfully,

                [YOUR NAME]
                [PHONE / EMAIL]

                ---
                %s
                """.formatted(facts, defaultDisclaimer(new JurisdictionContext()));
    }
}
