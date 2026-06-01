package com.legally.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legally.config.LegallyProperties;
import com.legally.model.JurisdictionContext;
import com.legally.model.LawChunk;
import com.legally.model.dto.ConsultRequest;
import com.legally.model.dto.GeminiLegalResponse;
import com.legally.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.*;

public class GeminiLegalLlmProvider implements LegalLlmProvider {

    private static final Logger log = LoggerFactory.getLogger(GeminiLegalLlmProvider.class);

    private final LegallyProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final StorageService storageService;

    public GeminiLegalLlmProvider(
            LegallyProperties properties,
            RestClient restClient,
            ObjectMapper objectMapper,
            StorageService storageService) {
        this.properties = properties;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.storageService = storageService;
    }

    @Override
    public String id() {
        return "gemini";
    }

    @Override
    public boolean isConfigured() {
        String key = properties.getGemini().getApiKey();
        return key != null && !key.isBlank();
    }

    @Override
    public boolean supportsNativeMultimodal() {
        return true;
    }

    @Override
    public Optional<LlmAnalysisOutcome> analyze(
            String userMessage,
            String scenario,
            JurisdictionContext jurisdiction,
            List<ConsultRequest.MediaRef> media) {
        if (!isConfigured()) {
            return Optional.empty();
        }
        if (GeminiQuotaCircuitBreaker.isOpen()) {
            log.info("Gemini legal analysis skipped: quota circuit breaker open for this request");
            return Optional.empty();
        }

        try {
            GeminiLegalResponse parsed = callWithGoogleSearch(userMessage, scenario, jurisdiction, media);
            if (!LlmResponseParser.hasSubstantiveLegalContent(parsed)) {
                return Optional.empty();
            }
            List<LawChunk> sources = LlmResponseParser.sourcesFromCitations(parsed, jurisdiction, "gemini");
            return Optional.of(new LlmAnalysisOutcome(parsed, sources, id()));
        } catch (Exception e) {
            if (LlmHttpErrors.isQuotaExceeded(e)) {
                GeminiQuotaCircuitBreaker.open();
                log.warn("Gemini legal analysis skipped (quota exceeded); trying next provider");
            } else {
                log.warn("Gemini legal analysis failed: {}", e.getMessage());
            }
            return Optional.empty();
        }
    }

    @Override
    public Optional<String> generateLegalDocument(LegalDocumentDraftRequest request) {
        if (!isConfigured()) {
            return Optional.empty();
        }
        try {
            String apiKey = properties.getGemini().getApiKey();
            String prompt = LegalPrompts.legalDocumentPrompt(request);
            Map<String, Object> body = Map.of(
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                    "generationConfig", Map.of("temperature", 0.25));

            String responseBody = callGemini(apiKey, body);
            JsonNode root = objectMapper.readTree(responseBody);
            String text = LegalPrompts.stripCodeFences(extractTextFromGeminiResponse(root));
            if (text.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(text);
        } catch (Exception e) {
            log.warn("Gemini document generation failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private GeminiLegalResponse callWithGoogleSearch(
            String userMessage,
            String scenario,
            JurisdictionContext jurisdiction,
            List<ConsultRequest.MediaRef> media) throws Exception {

        String apiKey = properties.getGemini().getApiKey();
        String textPrompt = LegalPrompts.analyzeUserMessage(userMessage, scenario, jurisdiction)
                + "\n\nSearch for official government and court sources for this jurisdiction. Cite sourceUrl for each point."
                + "\n\nIMPORTANT: Your final answer must be ONLY the JSON object from the system instruction (no prose before or after).";
        List<Map<String, Object>> parts = LlmMediaAttachment.buildParts(textPrompt, media, storageService);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("systemInstruction", Map.of("parts", List.of(Map.of("text", LegalPrompts.analyzeSystemInstruction(jurisdiction)))));
        body.put("contents", List.of(Map.of("role", "user", "parts", parts)));
        body.put("tools", List.of(Map.of("google_search", Map.of())));
        // Cannot combine google_search with responseMimeType application/json
        body.put("generationConfig", Map.of("temperature", 0.2));

        String responseBody = callGemini(apiKey, body);
        JsonNode root = objectMapper.readTree(responseBody);
        String text = extractTextFromGeminiResponse(root);
        if (text.isBlank()) {
            throw new IllegalStateException("Empty Gemini response");
        }
        GeminiLegalResponse parsed;
        try {
            parsed = LlmResponseParser.parseJsonResponse(objectMapper, text, jurisdiction);
        } catch (Exception parseEx) {
            log.debug("Gemini google_search returned non-JSON ({} chars), reformatting: {}",
                    text.length(), parseEx.getMessage());
            try {
                text = callJsonReformat(apiKey, userMessage, scenario, jurisdiction, text);
                parsed = LlmResponseParser.parseJsonResponse(objectMapper, text, jurisdiction);
            } catch (Exception reformatEx) {
                if (LlmHttpErrors.isQuotaExceeded(reformatEx)) {
                    throw reformatEx;
                }
                throw parseEx;
            }
        }
        if (parsed.getDisclaimer() == null || parsed.getDisclaimer().isBlank()) {
            parsed.setDisclaimer("Legally provides general legal information only, not legal advice. "
                    + "Sources retrieved via Google Search; verify URLs. Consult a licensed lawyer in "
                    + jurisdiction.getCountryName() + ".");
        }
        return parsed;
    }

    /**
     * Second pass without google_search so we can force {@code application/json} mime type.
     */
    private String callJsonReformat(
            String apiKey,
            String userMessage,
            String scenario,
            JurisdictionContext jurisdiction,
            String researchText) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("systemInstruction", Map.of(
                "parts", List.of(Map.of("text", LegalPrompts.analyzeSystemInstruction(jurisdiction)))));
        body.put("contents", List.of(Map.of(
                "role", "user",
                "parts", List.of(Map.of(
                        "text", LegalPrompts.jsonReformatUserMessage(researchText, jurisdiction)
                                + "\n\nOriginal question context:\n"
                                + LegalPrompts.analyzeUserMessage(userMessage, scenario, jurisdiction))))));
        body.put("generationConfig", Map.of(
                "temperature", 0.1,
                "responseMimeType", "application/json"));

        String responseBody = callGemini(apiKey, body);
        JsonNode root = objectMapper.readTree(responseBody);
        String text = extractTextFromGeminiResponse(root);
        if (text.isBlank()) {
            throw new IllegalStateException("Empty Gemini JSON reformat response");
        }
        return text;
    }

    private String extractTextFromGeminiResponse(JsonNode root) {
        StringBuilder sb = new StringBuilder();
        for (JsonNode part : root.path("candidates").path(0).path("content").path("parts")) {
            if (part.has("text")) {
                sb.append(part.path("text").asText(""));
            }
        }
        return sb.toString().trim();
    }

    private String callGemini(String apiKey, Map<String, Object> body) {
        String model = properties.getGemini().getModel();
        if (model == null || model.isBlank()) {
            model = "gemini-2.5-flash";
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
}
