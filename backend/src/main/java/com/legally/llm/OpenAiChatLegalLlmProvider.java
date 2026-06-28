package com.legally.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.legally.model.JurisdictionContext;
import com.legally.model.dto.ConsultRequest;
import com.legally.model.dto.GeminiLegalResponse;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * OpenAI-compatible chat API for legal analysis (Groq, OpenRouter, Mistral, and similar providers).
 */
public class OpenAiChatLegalLlmProvider extends AbstractOpenAiChatProvider implements LegalLlmProvider {

    public OpenAiChatLegalLlmProvider(
            String providerId,
            String baseUrl,
            String apiKey,
            String model,
            Map<String, String> extraHeaders,
            RestClient restClient,
            ObjectMapper objectMapper) {
        super(
                providerId,
                baseUrl,
                apiKey,
                model,
                extraHeaders,
                restClient,
                objectMapper,
                LoggerFactory.getLogger(OpenAiChatLegalLlmProvider.class));
    }

    @Override
    /** Provider identifier matching LLM_PROVIDER_ORDER entries. */
    public String id() {
        return providerId;
    }

    @Override
    /** True when API key and model are present. */
    public boolean isConfigured() {
        return isApiConfigured();
    }

    @Override
    /** Calls the provider for legal analysis. */
    public Optional<LlmAnalysisOutcome> analyze(
            String userMessage,
            String scenario,
            JurisdictionContext jurisdiction,
            List<ConsultRequest.MediaRef> media) {
        if (!isConfigured() || shouldSkipOpenRouter()) {
            return Optional.empty();
        }

        try {
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
                body.put("response_format", responseFormatForProvider("legal_response"));
            }

            String text = extractAssistantText(postChatCompletion(body));
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
                handleOpenRouterRateLimit("legal analysis");
            } else if (LlmHttpErrors.isQuotaExceeded(e)) {
                log.warn("{} legal analysis skipped (quota/rate limit)", providerId);
            } else {
                log.warn("{} legal analysis failed: {}", providerId, e.getMessage());
            }
            return Optional.empty();
        }
    }

    @Override
    /** Drafts a legal document when this provider supports it. */
    public Optional<String> generateLegalDocument(LegalDocumentDraftRequest request) {
        if (!isConfigured()) {
            return Optional.empty();
        }
        try {
            String prompt = LegalPrompts.legalDocumentPrompt(request);
            Map<String, Object> body = Map.of(
                    "model", model,
                    "temperature", 0.25,
                    "messages", List.of(Map.of("role", "user", "content", prompt)));

            String text = extractAssistantText(postChatCompletion(body));
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
