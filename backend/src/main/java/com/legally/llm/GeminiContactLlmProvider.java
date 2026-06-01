package com.legally.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legally.config.LegallyProperties;
import com.legally.model.ContactCard;
import com.legally.model.JurisdictionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GeminiContactLlmProvider implements ContactLlmProvider {

    private static final Logger log = LoggerFactory.getLogger(GeminiContactLlmProvider.class);

    private final LegallyProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GeminiContactLlmProvider(
            LegallyProperties properties, RestClient restClient, ObjectMapper objectMapper) {
        this.properties = properties;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
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
    public Optional<List<ContactCard>> findContacts(
            JurisdictionContext jurisdiction,
            String scenario,
            String userMessage,
            List<String> suggestedTags,
            String legalSummary) {
        if (!isConfigured()) {
            return Optional.empty();
        }
        if (GeminiQuotaCircuitBreaker.isOpen()) {
            log.info("Gemini contact research skipped: quota circuit breaker open for this request");
            return Optional.empty();
        }

        try {
            String apiKey = properties.getGemini().getApiKey();
            String text = callWithGoogleSearch(apiKey, jurisdiction, scenario, userMessage, suggestedTags, legalSummary);
            List<ContactCard> contacts;
            try {
                contacts = ContactResponseParser.parseContacts(objectMapper, text);
            } catch (Exception parseEx) {
                log.debug("Gemini contact google_search was not JSON ({} chars), reformatting: {}",
                        text.length(), parseEx.getMessage());
                text = callJsonReformat(apiKey, jurisdiction, scenario, userMessage, suggestedTags, legalSummary, text);
                contacts = ContactResponseParser.parseContacts(objectMapper, text);
            }
            if (contacts.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(contacts);
        } catch (Exception e) {
            if (LlmHttpErrors.isQuotaExceeded(e)) {
                GeminiQuotaCircuitBreaker.open();
                log.warn("Gemini contact research quota exceeded: {}", e.getMessage());
            } else {
                log.warn("Gemini contact research failed: {}", e.getMessage());
            }
            return Optional.empty();
        }
    }

    private String callWithGoogleSearch(
            String apiKey,
            JurisdictionContext jurisdiction,
            String scenario,
            String userMessage,
            List<String> suggestedTags,
            String legalSummary) throws Exception {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("systemInstruction", Map.of(
                "parts", List.of(Map.of("text", ContactResearchPrompts.systemInstructionWithWebSearch(jurisdiction)))));
        body.put("contents", List.of(Map.of(
                "role", "user",
                "parts", List.of(Map.of(
                        "text", ContactResearchPrompts.userMessage(
                                userMessage, scenario, jurisdiction, suggestedTags, legalSummary)
                                + "\n\nIMPORTANT: Final answer must be ONLY the JSON object (no prose).")))));
        body.put("tools", List.of(Map.of("google_search", Map.of())));
        body.put("generationConfig", Map.of("temperature", 0.2));

        String responseBody = callGemini(apiKey, body);
        JsonNode root = objectMapper.readTree(responseBody);
        String text = extractText(root);
        if (text.isBlank()) {
            throw new IllegalStateException("Empty Gemini contact response");
        }
        return text;
    }

    private String callJsonReformat(
            String apiKey,
            JurisdictionContext jurisdiction,
            String scenario,
            String userMessage,
            List<String> suggestedTags,
            String legalSummary,
            String researchText) throws Exception {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("systemInstruction", Map.of(
                "parts", List.of(Map.of("text", ContactResearchPrompts.systemInstructionWithWebSearch(jurisdiction)))));
        body.put("contents", List.of(Map.of(
                "role", "user",
                "parts", List.of(Map.of(
                        "text", ContactResearchPrompts.jsonReformatUserMessage(researchText, jurisdiction)
                                + "\n\nOriginal request:\n"
                                + ContactResearchPrompts.userMessage(
                                        userMessage, scenario, jurisdiction, suggestedTags, legalSummary))))));
        body.put("generationConfig", Map.of(
                "temperature", 0.1,
                "responseMimeType", "application/json"));

        String responseBody = callGemini(apiKey, body);
        JsonNode root = objectMapper.readTree(responseBody);
        String text = extractText(root);
        if (text.isBlank()) {
            throw new IllegalStateException("Empty Gemini contact JSON reformat");
        }
        return text;
    }

    private String extractText(JsonNode root) {
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
