package com.legally.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.legally.llm.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configuration
public class ContactLlmConfiguration {

    @Bean
    public GeminiContactLlmProvider geminiContactLlmProvider(
            LegallyProperties properties, RestClient restClient, ObjectMapper objectMapper) {
        return new GeminiContactLlmProvider(properties, restClient, objectMapper);
    }

    @Bean(name = LlmChainQualifiers.CONTACT)
    @Qualifier(LlmChainQualifiers.CONTACT)
    public List<ContactLlmProvider> contactLlmProviders(
            LegallyProperties properties,
            GeminiContactLlmProvider gemini,
            RestClient restClient,
            ObjectMapper objectMapper) {

        List<ContactLlmProvider> ordered = new ArrayList<>();
        LegallyProperties.Llm llm = properties.getLlm();

        for (String rawId : llm.getProviderOrder().split(",")) {
            String id = rawId.trim().toLowerCase();
            if (id.isEmpty()) {
                continue;
            }
            switch (id) {
                case "gemini" -> ordered.add(gemini);
                case "groq" -> addOpenAiProvider(ordered, "groq", llm.getGroq(), restClient, objectMapper);
                case "openrouter" -> addOpenAiProvider(ordered, "openrouter", llm.getOpenrouter(), restClient, objectMapper);
                case "mistral" -> addOpenAiProvider(ordered, "mistral", llm.getMistral(), restClient, objectMapper);
                case "cloudflare" -> {
                    LegallyProperties.LlmEndpoint cf = llm.getCloudflare();
                    if (cf.isConfigured() && cf.getAccountId() != null && !cf.getAccountId().isBlank()) {
                        ordered.add(new CloudflareContactLlmProvider(
                                cf.getAccountId(), cf.getApiKey(), cf.getModel(), restClient, objectMapper));
                    }
                }
                case "huggingface" -> {
                    LegallyProperties.LlmEndpoint hf = llm.getHuggingface();
                    if (hf.isConfigured()) {
                        ordered.add(new HuggingFaceContactLlmProvider(
                                hf.getApiKey(), hf.getModel(), restClient, objectMapper));
                    }
                }
                default -> {
                }
            }
        }

        if (ordered.isEmpty()) {
            ordered.add(gemini);
        }
        return List.copyOf(ordered);
    }

    private void addOpenAiProvider(
            List<ContactLlmProvider> ordered,
            String id,
            LegallyProperties.LlmEndpoint endpoint,
            RestClient restClient,
            ObjectMapper objectMapper) {
        if (!endpoint.isConfigured()) {
            return;
        }
        Map<String, String> headers = Map.of();
        if ("openrouter".equals(id)) {
            headers = Map.of(
                    "HTTP-Referer", "https://legally.app",
                    "Referer", "https://legally.app",
                    "X-Title", "Legally");
        }
        ordered.add(new OpenAiChatContactLlmProvider(
                id,
                endpoint.getBaseUrl(),
                endpoint.getApiKey(),
                endpoint.getModel(),
                headers,
                restClient,
                objectMapper));
    }
}
