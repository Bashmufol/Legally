package com.legally.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.legally.llm.*;
import com.legally.service.StorageService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builds ordered legal and contact LLM provider chains from {@code LLM_PROVIDER_ORDER}.
 */
@Configuration
public class LlmChainConfiguration {

    @Bean
    /** gemini legal llm provider. */
    public GeminiLegalLlmProvider geminiLegalLlmProvider(
            GeminiApiClient geminiApiClient,
            StorageService storageService,
            ObjectMapper objectMapper) {
        return new GeminiLegalLlmProvider(geminiApiClient, storageService, objectMapper);
    }

    @Bean
    /** gemini contact llm provider. */
    public GeminiContactLlmProvider geminiContactLlmProvider(
            GeminiApiClient geminiApiClient, ObjectMapper objectMapper) {
        return new GeminiContactLlmProvider(geminiApiClient, objectMapper);
    }

    @Bean(name = LlmChainQualifiers.LEGAL)
    @Qualifier(LlmChainQualifiers.LEGAL)
    /** legal llm providers. */
    public List<LegalLlmProvider> legalLlmProviders(
            LegallyProperties properties,
            GeminiLegalLlmProvider gemini,
            RestClient restClient,
            ObjectMapper objectMapper) {
        return buildLegalChain(properties, gemini, restClient, objectMapper);
    }

    @Bean(name = LlmChainQualifiers.CONTACT)
    @Qualifier(LlmChainQualifiers.CONTACT)
    /** contact llm providers. */
    public List<ContactLlmProvider> contactLlmProviders(
            LegallyProperties properties,
            GeminiContactLlmProvider gemini,
            RestClient restClient,
            ObjectMapper objectMapper) {
        return buildContactChain(properties, gemini, restClient, objectMapper);
    }

    static List<LegalLlmProvider> buildLegalChain(
            LegallyProperties properties,
            GeminiLegalLlmProvider gemini,
            RestClient restClient,
            ObjectMapper objectMapper) {
        List<LegalLlmProvider> ordered = new ArrayList<>();
        LegallyProperties.Llm llm = properties.getLlm();

        for (String rawId : llm.getProviderOrder().split(",")) {
            String id = rawId.trim().toLowerCase();
            if (id.isEmpty()) {
                continue;
            }
            switch (id) {
                case "gemini" -> ordered.add(gemini);
                case "groq" -> addOpenAiLegal(ordered, "groq", llm.getGroq(), restClient, objectMapper);
                case "openrouter" -> addOpenAiLegal(ordered, "openrouter", llm.getOpenrouter(), restClient, objectMapper);
                case "mistral" -> addOpenAiLegal(ordered, "mistral", llm.getMistral(), restClient, objectMapper);
                case "cloudflare" -> addCloudflareLegal(ordered, llm.getCloudflare(), restClient, objectMapper);
                case "huggingface" -> addHuggingFaceLegal(ordered, llm.getHuggingface(), restClient, objectMapper);
                default -> {
                }
            }
        }

        if (ordered.isEmpty()) {
            ordered.add(gemini);
        }
        return List.copyOf(ordered);
    }

    static List<ContactLlmProvider> buildContactChain(
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
                case "groq" -> addOpenAiContact(ordered, "groq", llm.getGroq(), restClient, objectMapper);
                case "openrouter" -> addOpenAiContact(ordered, "openrouter", llm.getOpenrouter(), restClient, objectMapper);
                case "mistral" -> addOpenAiContact(ordered, "mistral", llm.getMistral(), restClient, objectMapper);
                case "cloudflare" -> addCloudflareContact(ordered, llm.getCloudflare(), restClient, objectMapper);
                case "huggingface" -> addHuggingFaceContact(ordered, llm.getHuggingface(), restClient, objectMapper);
                default -> {
                }
            }
        }

        if (ordered.isEmpty()) {
            ordered.add(gemini);
        }
        return List.copyOf(ordered);
    }

    private static void addOpenAiLegal(
            List<LegalLlmProvider> ordered,
            String id,
            LegallyProperties.LlmEndpoint endpoint,
            RestClient restClient,
            ObjectMapper objectMapper) {
        if (!endpoint.isConfigured()) {
            return;
        }
        ordered.add(new OpenAiChatLegalLlmProvider(
                id,
                endpoint.getBaseUrl(),
                endpoint.getApiKey(),
                endpoint.getModel(),
                openRouterHeaders(id),
                restClient,
                objectMapper));
    }

    private static void addOpenAiContact(
            List<ContactLlmProvider> ordered,
            String id,
            LegallyProperties.LlmEndpoint endpoint,
            RestClient restClient,
            ObjectMapper objectMapper) {
        if (!endpoint.isConfigured()) {
            return;
        }
        ordered.add(new OpenAiChatContactLlmProvider(
                id,
                endpoint.getBaseUrl(),
                endpoint.getApiKey(),
                endpoint.getModel(),
                openRouterHeaders(id),
                restClient,
                objectMapper));
    }

    private static void addCloudflareLegal(
            List<LegalLlmProvider> ordered,
            LegallyProperties.LlmEndpoint endpoint,
            RestClient restClient,
            ObjectMapper objectMapper) {
        if (!endpoint.isConfigured() || endpoint.getAccountId() == null || endpoint.getAccountId().isBlank()) {
            return;
        }
        ordered.add(new CloudflareLegalLlmProvider(
                endpoint.getAccountId(), endpoint.getApiKey(), endpoint.getModel(), restClient, objectMapper));
    }

    private static void addCloudflareContact(
            List<ContactLlmProvider> ordered,
            LegallyProperties.LlmEndpoint endpoint,
            RestClient restClient,
            ObjectMapper objectMapper) {
        if (!endpoint.isConfigured() || endpoint.getAccountId() == null || endpoint.getAccountId().isBlank()) {
            return;
        }
        ordered.add(new CloudflareContactLlmProvider(
                endpoint.getAccountId(), endpoint.getApiKey(), endpoint.getModel(), restClient, objectMapper));
    }

    private static void addHuggingFaceLegal(
            List<LegalLlmProvider> ordered,
            LegallyProperties.LlmEndpoint endpoint,
            RestClient restClient,
            ObjectMapper objectMapper) {
        if (!endpoint.isConfigured()) {
            return;
        }
        ordered.add(new HuggingFaceLegalLlmProvider(
                endpoint.getApiKey(), endpoint.getModel(), restClient, objectMapper));
    }

    private static void addHuggingFaceContact(
            List<ContactLlmProvider> ordered,
            LegallyProperties.LlmEndpoint endpoint,
            RestClient restClient,
            ObjectMapper objectMapper) {
        if (!endpoint.isConfigured()) {
            return;
        }
        ordered.add(new HuggingFaceContactLlmProvider(
                endpoint.getApiKey(), endpoint.getModel(), restClient, objectMapper));
    }

    private static Map<String, String> openRouterHeaders(String providerId) {
        if (!"openrouter".equals(providerId)) {
            return Map.of();
        }
        return Map.of(
                "HTTP-Referer", "https://legally.app",
                "Referer", "https://legally.app",
                "X-Title", "Legally");
    }
}
