package com.legally.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legally.config.LegallyProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * HTTP client for the Gemini generateContent API.
 */
@Component
public class GeminiApiClient {

    private final LegallyProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GeminiApiClient(LegallyProperties properties, RestClient restClient, ObjectMapper objectMapper) {
        this.properties = properties;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    /** True when API key and model are present. */
    public boolean isConfigured() {
        String key = properties.getGemini().getApiKey();
        return key != null && !key.isBlank();
    }

    /** api key. */
    public String apiKey() {
        return properties.getGemini().getApiKey();
    }

    /** POST to Gemini generateContent and return the raw JSON body. */
    public String generateContent(Map<String, Object> body) {
        String model = properties.getGemini().getModel();
        if (model == null || model.isBlank()) {
            model = "gemini-2.5-flash";
        }
        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + model + ":generateContent?key=" + apiKey();

        return restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);
    }

    /** Reads assistant text from a Gemini response payload. */
    public String extractText(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        return extractText(root);
    }

    /** Reads assistant text from a Gemini response payload. */
    public String extractText(JsonNode root) {
        StringBuilder text = new StringBuilder();
        for (JsonNode part : root.path("candidates").path(0).path("content").path("parts")) {
            if (part.has("text")) {
                text.append(part.path("text").asText(""));
            }
        }
        return text.toString().trim();
    }
}
