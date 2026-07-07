package com.legally.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Legal analysis via Hugging Face router (OpenAI-compatible endpoint).
 */
public class HuggingFaceLegalLlmProvider extends OpenAiChatLegalLlmProvider {

    public HuggingFaceLegalLlmProvider(String apiKey, String model, RestClient restClient, ObjectMapper objectMapper) {
        super(
                "huggingface",
                "https://router.huggingface.co/v1",
                apiKey,
                model,
                Map.of(),
                restClient,
                objectMapper);
    }
}
