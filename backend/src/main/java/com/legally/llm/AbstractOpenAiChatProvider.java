package com.legally.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Shared HTTP setup for OpenAI-compatible chat APIs (Groq, OpenRouter, Mistral, Cloudflare, Hugging Face).
 */
abstract class AbstractOpenAiChatProvider {

    protected final Logger log;
    protected final String providerId;
    protected final String baseUrl;
    protected final String apiKey;
    protected final String model;
    protected final Map<String, String> extraHeaders;
    protected final RestClient restClient;
    protected final ObjectMapper objectMapper;

    protected AbstractOpenAiChatProvider(
            String providerId,
            String baseUrl,
            String apiKey,
            String model,
            Map<String, String> extraHeaders,
            RestClient restClient,
            ObjectMapper objectMapper,
            Logger log) {
        this.providerId = providerId;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.extraHeaders = extraHeaders != null ? extraHeaders : Map.of();
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.log = log;
    }

    protected boolean isApiConfigured() {
        return apiKey != null && !apiKey.isBlank() && model != null && !model.isBlank();
    }

    protected boolean isOpenRouter() {
        return "openrouter".equalsIgnoreCase(providerId);
    }

    /** Skip further OpenRouter calls in this HTTP request after a 429. */
    protected boolean shouldSkipOpenRouter() {
        if (isOpenRouter() && OpenRouterRateLimitCircuitBreaker.isOpen()) {
            log.info("OpenRouter skipped: rate-limit circuit breaker open for this request");
            return true;
        }
        return false;
    }

    /** uses structured response format. */
    protected boolean usesStructuredResponseFormat() {
        return !isOpenRouter();
    }

    /** response format for provider. */
    protected Map<String, Object> responseFormatForProvider(String schemaName) {
        if ("cloudflare".equalsIgnoreCase(providerId)) {
            return Map.of(
                    "type", "json_schema",
                    "json_schema", Map.of(
                            "name", schemaName,
                            "schema", Map.of("type", "object")));
        }
        return Map.of("type", "json_object");
    }

    /** post chat completion. */
    protected String postChatCompletion(Map<String, Object> body) {
        RestClient.RequestBodySpec spec = restClient.post()
                .uri(baseUrl + "/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + apiKey);
        for (Map.Entry<String, String> header : extraHeaders.entrySet()) {
            spec = spec.header(header.getKey(), header.getValue());
        }
        return spec.body(body).retrieve().body(String.class);
    }

    /** extract assistant text. */
    protected String extractAssistantText(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        return root.path("choices").path(0).path("message").path("content").asText("");
    }

    /** handle open router rate limit. */
    protected void handleOpenRouterRateLimit(String taskLabel) {
        OpenRouterRateLimitCircuitBreaker.open();
        log.warn("OpenRouter {} skipped (rate limited); trying next provider", taskLabel);
    }
}
