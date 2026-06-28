package com.legally.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legally.config.LegallyProperties;
import com.legally.model.JurisdictionContext;
import com.legally.model.dto.ConsultRequest;
import com.legally.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Summarizes uploaded images, video, audio, and PDFs via Gemini so text-only providers in the
 * multi-LLM chain can still reason about evidence.
 */
@Service
public class LegalMediaDigestService {

    private static final Logger log = LoggerFactory.getLogger(LegalMediaDigestService.class);

    private final LegallyProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final StorageService storageService;

    public LegalMediaDigestService(
            LegallyProperties properties,
            RestClient restClient,
            ObjectMapper objectMapper,
            StorageService storageService) {
        this.properties = properties;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.storageService = storageService;
    }

    public boolean isAvailable() {
        String key = properties.getGemini().getApiKey();
        return key != null && !key.isBlank();
    }

    /** build digest. */
    public Optional<String> buildDigest(
            String userMessage,
            String scenario,
            JurisdictionContext jurisdiction,
            List<ConsultRequest.MediaRef> media) {
        if (media == null || media.isEmpty()) {
            return Optional.empty();
        }
        if (!isAvailable()) {
            log.warn("Media digest skipped: GEMINI_API_KEY not configured");
            return Optional.empty();
        }
        if (GeminiQuotaCircuitBreaker.isOpen()) {
            log.info("Media digest skipped: Gemini quota circuit breaker open for this request");
            return Optional.empty();
        }

        try {
            String apiKey = properties.getGemini().getApiKey();
            String prompt = LegalPrompts.mediaDigestPrompt(userMessage, scenario, jurisdiction);
            List<Map<String, Object>> parts = LlmMediaAttachment.buildParts(prompt, media, storageService);

            Map<String, Object> body = Map.of(
                    "contents", List.of(Map.of("role", "user", "parts", parts)),
                    "generationConfig", Map.of("temperature", 0.2));

            String responseBody = callGemini(apiKey, body);
            JsonNode root = objectMapper.readTree(responseBody);
            String text = extractText(root);
            if (text.isBlank()) {
                return Optional.empty();
            }
            log.info("Built media digest for multi-LLM chain ({} attachment(s), {} chars)",
                    media.size(), text.length());
            return Optional.of(text);
        } catch (Exception e) {
            if (LlmHttpErrors.isQuotaExceeded(e)) {
                GeminiQuotaCircuitBreaker.open();
                log.warn("Media digest skipped (Gemini quota exceeded)");
            } else {
                log.warn("Media digest failed: {}", e.getMessage());
            }
            return Optional.empty();
        }
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
