package com.legally.config;

import com.legally.llm.LegalLlmProvider;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class LegalLlmProvidersLogger {

    private static final Logger log = LoggerFactory.getLogger(LegalLlmProvidersLogger.class);

    private final List<LegalLlmProvider> providers;
    private final LegallyProperties properties;

    public LegalLlmProvidersLogger(
            @Qualifier(LlmChainQualifiers.LEGAL) List<LegalLlmProvider> providers,
            LegallyProperties properties) {
        this.providers = providers;
        this.properties = properties;
    }

    @PostConstruct
    void logProviders() {
        LegallyProperties.Llm llm = properties.getLlm();
        log.info("LLM_PROVIDER_ORDER from config: {}", llm.getProviderOrder());
        log.info("LLM provider credentials at startup (empty apiKey = not loaded from .env):");
        for (String rawId : llm.getProviderOrder().split(",")) {
            String id = rawId.trim().toLowerCase();
            if (id.isEmpty()) {
                continue;
            }
            switch (id) {
                case "gemini" -> log.info(
                        "  - gemini: apiKey={}",
                        mask(properties.getGemini().getApiKey()));
                case "groq" -> logEndpoint("groq", llm.getGroq(), false);
                case "openrouter" -> logEndpoint("openrouter", llm.getOpenrouter(), false);
                case "mistral" -> logEndpoint("mistral", llm.getMistral(), false);
                case "cloudflare" -> logEndpoint("cloudflare", llm.getCloudflare(), true);
                case "huggingface" -> logEndpoint("huggingface", llm.getHuggingface(), false);
                default -> log.warn("  - {}: unknown provider id in LLM_PROVIDER_ORDER", id);
            }
        }

        String ids = providers.stream().map(LegalLlmProvider::id).collect(Collectors.joining(" → "));
        log.info("Legal LLM fallback chain ({} active providers): {}", providers.size(), ids);
        for (LegalLlmProvider p : providers) {
            log.info("  - {} in chain, configured={}", p.id(), p.isConfigured());
        }

        List<String> orderedIds = Arrays.stream(llm.getProviderOrder().split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .toList();
        if (providers.size() < orderedIds.size()) {
            log.warn(
                    "Some providers in LLM_PROVIDER_ORDER were skipped at startup. Usual causes: missing "
                            + "api key or model, or cloudflare missing CLOUDFLARE_ACCOUNT_ID.");
        }
    }

    private void logEndpoint(String id, LegallyProperties.LlmEndpoint endpoint, boolean needsAccountId) {
        boolean configured = endpoint.isConfigured();
        if (needsAccountId) {
            configured = configured && endpoint.getAccountId() != null && !endpoint.getAccountId().isBlank();
        }
        log.info(
                "  - {}: apiKey={}, model={}, accountId={}, configured={}",
                id,
                mask(endpoint.getApiKey()),
                blankToDash(endpoint.getModel()),
                needsAccountId ? mask(endpoint.getAccountId()) : "n/a",
                configured);
    }

    private static String mask(String value) {
        if (value == null || value.isBlank()) {
            return "(empty)";
        }
        if (value.length() <= 8) {
            return "***";
        }
        return value.substring(0, 4) + "…" + value.substring(value.length() - 4);
    }

    private static String blankToDash(String value) {
        return value == null || value.isBlank() ? "(empty)" : value;
    }
}
