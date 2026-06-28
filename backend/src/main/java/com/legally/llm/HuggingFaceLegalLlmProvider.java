package com.legally.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * LLM provider implementation for hugging face legal.
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
