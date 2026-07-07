package com.legally.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Contact research via Cloudflare Workers AI (OpenAI-compatible endpoint).
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
