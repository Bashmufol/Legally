package com.legally.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legally.model.JurisdictionContext;
import com.legally.model.dto.ConsultRequest;
import com.legally.model.dto.GeminiLegalResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * OpenAI-compatible chat API (Groq, OpenRouter, Mistral, etc.).
 */
public class OpenAiChatLegalLlmProvider implements LegalLlmProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAiChatLegalLlmProvider.class);

    private final String providerId;
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final Map<String, String> extraHeaders;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public OpenAiChatLegalLlmProvider(
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
    public Optional<LlmAnalysisOutcome> analyze(
            String userMessage,
            String scenario,
            JurisdictionContext jurisdiction,
            List<ConsultRequest.MediaRef> media) {
        if (!isConfigured()) {
            return Optional.empty();
        }
        if (isOpenRouter() && OpenRouterRateLimitCircuitBreaker.isOpen()) {
            log.info("OpenRouter legal analysis skipped: rate-limit circuit breaker open for this request");
            return Optional.empty();
        }

        try {
            // Orchestrator pre-formats the user turn when a media digest was merged in.
            String userContent = userMessage != null && userMessage.startsWith("Scenario:")
                    ? userMessage
                    : LegalPrompts.analyzeUserMessage(userMessage, scenario, jurisdiction);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("temperature", 0.2);
            body.put("messages", List.of(
                    Map.of("role", "system", "content", LegalPrompts.analyzeSystemInstruction(jurisdiction)),
                    Map.of("role", "user", "content", userContent)));
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

            GeminiLegalResponse parsed = LlmResponseParser.parseJsonResponse(objectMapper, text, jurisdiction);
            if (!LlmResponseParser.hasSubstantiveLegalContent(parsed)) {
                return Optional.empty();
            }
            return Optional.of(new LlmAnalysisOutcome(
                    parsed,
                    LlmResponseParser.sourcesFromCitations(parsed, jurisdiction, providerId),
                    providerId));
        } catch (Exception e) {
            if (isOpenRouter() && LlmHttpErrors.isRateLimited(e)) {
                OpenRouterRateLimitCircuitBreaker.open();
                log.warn("OpenRouter legal analysis skipped (rate limited); trying next provider");
            } else if (LlmHttpErrors.isQuotaExceeded(e)) {
                log.warn("{} legal analysis skipped (quota/rate limit)", providerId);
            } else {
                log.warn("{} legal analysis failed: {}", providerId, e.getMessage());
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
                            "name", "legal_response",
                            "schema", Map.of("type", "object")));
        }
        return Map.of("type", "json_object");
    }

    @Override
    public Optional<String> generateLegalDocument(LegalDocumentDraftRequest request) {
        if (!isConfigured()) {
            return Optional.empty();
        }
        try {
            String prompt = LegalPrompts.legalDocumentPrompt(request);
            Map<String, Object> body = Map.of(
                    "model", model,
                    "temperature", 0.25,
                    "messages", List.of(
                            Map.of("role", "user", "content", prompt)));

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
            text = LegalPrompts.stripCodeFences(text);
            if (text.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(text);
        } catch (Exception e) {
            log.warn("{} document generation failed: {}", providerId, e.getMessage());
            return Optional.empty();
        }
    }
}
