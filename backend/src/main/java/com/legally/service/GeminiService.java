package com.legally.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legally.config.LegallyProperties;
import com.legally.llm.LlmResponseParser;
import com.legally.llm.GeminiQuotaCircuitBreaker;
import com.legally.model.JurisdictionContext;
import com.legally.model.LawChunk;
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

    /**
     * Returned when SerpApi and Gemini google_search grounding both fail to produce citable law.
     */
    public GeminiLegalResponse buildNoInformationResponse(
            String userMessage, String scenario, JurisdictionContext jurisdiction) throws Exception {
        GeminiLegalResponse r = new GeminiLegalResponse();
        r.setSummary(
                "We could not find any legal information relating to your situation for "
                        + jurisdiction.displayLabel()
                        + " at the moment. Please try again later.");
        r.setLegalAnalysis(List.of());
        r.setConfidence("low");
        r.setDisclaimer(defaultDisclaimer(jurisdiction));
        r.setSuggestedContactTags(List.of("legal_aid"));
        r.setDemandLetterEligible(false);
        r.setSteps(suggestNextSteps(userMessage, scenario, jurisdiction));
        return r;
    }

    public GeminiLegalResponse buildJurisdictionUnresolvedResponse() {
        GeminiLegalResponse r = new GeminiLegalResponse();
        r.setSummary(
                "We could not determine your location (country and state or region). "
                        + "A clear jurisdiction is required before Legally can provide legal guidance "
                        + "for the laws that apply to you.");
        r.setLegalAnalysis(List.of());
        r.setConfidence("low");
        r.setDisclaimer(
                "This is general information, not legal advice. Laws vary by location; "
                        + "confirm your jurisdiction before relying on any guidance.");
        r.setSuggestedContactTags(List.of());
        r.setDemandLetterEligible(false);
        r.setSteps(List.of(
                "Allow device location in your browser when prompted, then submit again.",
                "Type your country and state or region in the description (for example, Nigeria, Kwara State).",
                "If you use a voice note or upload, clearly say or show where the issue is taking place.",
                "After your location is set, resubmit your question for a full legal analysis."));
        return r;
    }

    public GeminiLegalResponse buildMediaProcessingFailedResponse(JurisdictionContext jurisdiction) {
        String area = jurisdiction != null && jurisdiction.displayLabel() != null
                ? jurisdiction.displayLabel()
                : "your area";
        GeminiLegalResponse r = new GeminiLegalResponse();
        r.setSummary(
                "We could not process your uploaded voice, video, or document for "
                        + area
                        + ". The file may be unsupported, unreadable, or temporarily unavailable to process.");
        r.setLegalAnalysis(List.of());
        r.setConfidence("low");
        r.setDisclaimer(defaultDisclaimer(jurisdiction));
        r.setSuggestedContactTags(List.of("legal_aid"));
        r.setDemandLetterEligible(false);
        r.setSteps(List.of(
                "Add a short typed description of your situation and key facts, then submit again.",
                "If you used audio or video, try a shorter recording or re-record in a quiet place.",
                "If you uploaded a scan or PDF, try a clearer photo or a text-based PDF if possible.",
                "You can combine typed text with a different attachment format and retry.",
                "If urgent, consult a licensed lawyer in " + jurisdiction.getCountryName() + "."));
        return r;
    }

    private List<String> suggestNextSteps(
            String userMessage, String scenario, JurisdictionContext jurisdiction) {
        if (GeminiQuotaCircuitBreaker.isOpen()) {
            return defaultNoInfoSteps(jurisdiction);
        }
        String apiKey = properties.getGemini().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return defaultNoInfoSteps(jurisdiction);
        }

        String prompt = """
                The user asked about a legal situation but no official sources could be retrieved.
                Do NOT state what the law is. Do NOT invent statutes, cases, or phone numbers.
                Return ONLY a JSON array of 3-5 short practical next steps (strings) for a non-lawyer in %s.
                Scenario hint: %s
                User message: %s
                """.formatted(
                jurisdiction.displayLabel(),
                scenario != null ? scenario : "general",
                userMessage != null && !userMessage.isBlank() ? userMessage : "(not provided)");

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of(
                        "temperature", 0.3,
                        "responseMimeType", "application/json"));

        try {
            String responseBody = callGemini(apiKey, body);
            JsonNode root = objectMapper.readTree(responseBody);
            String text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText("");
            text = text.replace("```json", "").replace("```", "").trim();
            JsonNode arr = objectMapper.readTree(text);
            if (arr.isArray() && !arr.isEmpty()) {
                List<String> steps = new ArrayList<>();
                for (JsonNode node : arr) {
                    String s = node.asText("").trim();
                    if (!s.isBlank()) {
                        steps.add(s);
                    }
                }
                if (!steps.isEmpty()) {
                    return steps;
                }
            }
        } catch (Exception e) {
            if (com.legally.llm.LlmHttpErrors.isQuotaExceeded(e)) {
                GeminiQuotaCircuitBreaker.open();
                log.debug("Skipping Gemini no-info step suggestions (quota exceeded)");
            } else {
                log.warn("Could not generate no-info suggestions: {}", e.getMessage());
            }
        }
        return defaultNoInfoSteps(jurisdiction);
    }

    private List<String> defaultNoInfoSteps(JurisdictionContext jurisdiction) {
        return List.of(
                "Try again in a few minutes; AI services may be temporarily unavailable.",
                "Search your government's justice or legal-aid website directly for current guidance.",
                "Consult a licensed lawyer in " + jurisdiction.getCountryName() + " before taking irreversible action.",
                "Keep written records, photos, and dates of everything related to your situation.");
    }

    /**
     * Summarize only from backend-filtered official web excerpts.
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
            r.setSummary("Web research is not available. Configure GEMINI_API_KEY for "
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
        body.put("generationConfig", Map.of("temperature", 0.2));

        try {
            String responseBody = callGemini(apiKey, body);
            JsonNode root = objectMapper.readTree(responseBody);
            StringBuilder textBuilder = new StringBuilder();
            for (JsonNode part : root.path("candidates").path(0).path("content").path("parts")) {
                if (part.has("text")) {
                    textBuilder.append(part.path("text").asText(""));
                }
            }
            GeminiLegalResponse parsed = LlmResponseParser.parseJsonResponse(
                    objectMapper, textBuilder.toString(), jurisdiction);
            parsed.setDisclaimer(webDisclaimer(jurisdiction) + " Sources retrieved via Google Search; verify URLs.");
            if (parsed.getConfidence() == null || parsed.getConfidence().isBlank()) {
                parsed.setConfidence("medium");
            }
            return parsed;
        } catch (Exception e) {
            log.warn("Gemini google_search grounding failed: {}", e.getMessage());
            GeminiLegalResponse r = new GeminiLegalResponse();
            r.setSummary("Could not complete web research for " + jurisdiction.displayLabel()
                    + ". Configure GEMINI_API_KEY or retry later.");
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
        if (GeminiQuotaCircuitBreaker.isOpen()) {
            return Optional.empty();
        }
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
                Determine if the user EXPLICITLY asks for legal information under a different country or state/province
                than their device location. Do NOT guess or infer location from stereotypes.
                Device location (do not override unless user clearly says otherwise): %s, %s (code %s / %s).

                Inspect the user message AND any attached files (images, PDFs, audio, video).
                Examples of explicit override: "under French law", "in Lagos", "Kwara State", "California law".
                NOT explicit: generic legal questions with no place named.

                If a specific country (and optional state/province) is clearly requested by the user, return ONLY JSON:
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
            String countryCode = parsed.path("countryCode").asText("").trim().toUpperCase(Locale.ROOT);
            if (countryCode.isBlank() || "INT".equals(countryCode)) {
                return Optional.empty();
            }
            JurisdictionContext ctx = new JurisdictionContext();
            ctx.setCountryCode(countryCode);
            ctx.setCountryName(parsed.path("countryName").asText(countryCode));
            String regionCode = parsed.path("regionCode").asText("").trim().toUpperCase(Locale.ROOT);
            String regionName = parsed.path("regionName").asText("").trim();
            if ("GENERAL".equalsIgnoreCase(regionCode) || "General".equalsIgnoreCase(regionName)) {
                regionCode = "";
                regionName = "";
            }
            ctx.setRegionCode(regionCode);
            ctx.setRegionName(regionName);
            return Optional.of(ctx);
        } catch (Exception e) {
            if (com.legally.llm.LlmHttpErrors.isQuotaExceeded(e)) {
                GeminiQuotaCircuitBreaker.open();
            }
            log.warn("Gemini jurisdiction detection failed: {}", e.getMessage());
            return Optional.empty();
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
                5. Output ONLY valid JSON (include sourceUrl in each citation).
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
