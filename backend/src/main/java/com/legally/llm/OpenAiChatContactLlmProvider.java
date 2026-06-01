package com.legally.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legally.model.ContactCard;
import com.legally.model.JurisdictionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * OpenAI-compatible chat API for contact discovery (no live web search).
 */
public class OpenAiChatContactLlmProvider implements ContactLlmProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAiChatContactLlmProvider.class);

    private final String providerId;
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final Map<String, String> extraHeaders;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public OpenAiChatContactLlmProvider(
            String providerId,
            String baseUrl,
            String apiKey,
            String model,
            Map<String, String> extraHeaders,
            RestClient restClient,
            ObjectMapper objectMapper) {
        this.providerId = providerId;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.extraHeaders = extraHeaders != null ? extraHeaders : Map.of();
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public String id() {
        return providerId;
    }

    @Override
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && model != null && !model.isBlank();
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
        if (isOpenRouter() && OpenRouterRateLimitCircuitBreaker.isOpen()) {
            log.info("OpenRouter contact research skipped: rate-limit circuit breaker open for this request");
            return Optional.empty();
        }

        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("temperature", 0.2);
            body.put("messages", List.of(
                    Map.of(
                            "role", "system",
                            "content", ContactResearchPrompts.systemInstructionKnowledgeOnly(jurisdiction)),
                    Map.of(
                            "role", "user",
                            "content", ContactResearchPrompts.userMessage(
                                    userMessage, scenario, jurisdiction, suggestedTags, legalSummary))));
            if (usesStructuredResponseFormat()) {
                body.put("response_format", responseFormatForProvider());
            }

            RestClient.RequestBodySpec spec = restClient.post()
                    .uri(baseUrl + "/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey);
            for (Map.Entry<String, String> h : extraHeaders.entrySet()) {
                spec = spec.header(h.getKey(), h.getValue());
            }

            String responseBody = spec.body(body).retrieve().body(String.class);
            JsonNode root = objectMapper.readTree(responseBody);
            String text = root.path("choices").path(0).path("message").path("content").asText("");
            if (text.isBlank()) {
                return Optional.empty();
            }

            List<ContactCard> contacts = ContactResponseParser.parseContacts(objectMapper, text);
            if (contacts.isEmpty()) {
                log.debug("{} contact research returned JSON but no contacts passed validation", providerId);
                return Optional.empty();
            }
            return Optional.of(contacts);
        } catch (Exception e) {
            if (isOpenRouter() && LlmHttpErrors.isRateLimited(e)) {
                OpenRouterRateLimitCircuitBreaker.open();
                log.warn("OpenRouter contact research skipped (rate limited); trying next provider");
            } else {
                log.warn("{} contact research failed: {}", providerId, e.getMessage());
            }
            return Optional.empty();
        }
    }

    private boolean isOpenRouter() {
        return "openrouter".equalsIgnoreCase(providerId);
    }

    private boolean usesStructuredResponseFormat() {
        return !isOpenRouter();
    }

    private Map<String, Object> responseFormatForProvider() {
        if ("cloudflare".equalsIgnoreCase(providerId)) {
            return Map.of(
                    "type", "json_schema",
                    "json_schema", Map.of(
                            "name", "contact_response",
                            "schema", Map.of("type", "object")));
        }
        return Map.of("type", "json_object");
    }
}
