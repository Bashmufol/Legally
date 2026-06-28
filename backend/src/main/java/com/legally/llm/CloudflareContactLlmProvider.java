package com.legally.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * LLM provider implementation for cloudflare contact.
 */
public class CloudflareContactLlmProvider extends OpenAiChatContactLlmProvider {

    public CloudflareContactLlmProvider(
            String accountId,
            String apiKey,
            String model,
            RestClient restClient,
            ObjectMapper objectMapper) {
        super(
                "cloudflare",
                "https://api.cloudflare.com/client/v4/accounts/" + accountId + "/ai/v1",
                apiKey,
                model,
                Map.of(),
                restClient,
                objectMapper);
    }
}
