package com.legally.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.legally.model.ContactCard;
import com.legally.model.JurisdictionContext;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * OpenAI-compatible chat API for contact discovery (no live web search on fallback providers).
 */
public class OpenAiChatContactLlmProvider extends AbstractOpenAiChatProvider implements ContactLlmProvider {

    public OpenAiChatContactLlmProvider(
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
                LoggerFactory.getLogger(OpenAiChatContactLlmProvider.class));
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
    /** Runs contact research across configured LLM providers. */
    public Optional<List<ContactCard>> findContacts(
            JurisdictionContext jurisdiction,
            String scenario,
            String userMessage,
            List<String> suggestedTags,
            String legalSummary) {
        if (!isConfigured() || shouldSkipOpenRouter()) {
            return Optional.empty();
        }

        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("temperature", 0.2);
            body.put("messages", List.of(
                    Map.of(
                            "role", "system",
                            "content", ContactResearchPrompts.systemInstructionKnowledgeOnly(jurisdiction)),
                    Map.of(
                            "role", "user",
                            "content", ContactResearchPrompts.userMessage(
                                    userMessage, scenario, jurisdiction, suggestedTags, legalSummary))));
            if (usesStructuredResponseFormat()) {
                body.put("response_format", responseFormatForProvider("contact_response"));
            }

            String text = extractAssistantText(postChatCompletion(body));
            if (text.isBlank()) {
                return Optional.empty();
            }

            List<ContactCard> contacts = ContactResponseParser.parseContacts(objectMapper, text);
            if (contacts.isEmpty()) {
                log.debug("{} contact research returned JSON but no contacts passed validation", providerId);
                return Optional.empty();
            }
            return Optional.of(contacts);
        } catch (Exception e) {
            if (isOpenRouter() && LlmHttpErrors.isRateLimited(e)) {
                handleOpenRouterRateLimit("contact research");
            } else {
                log.warn("{} contact research failed: {}", providerId, e.getMessage());
            }
            return Optional.empty();
        }
    }
}
