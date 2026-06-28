package com.legally.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.legally.model.ContactCard;
import com.legally.model.JurisdictionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Gemini provider for contact research with Google Search grounding.
 */
public class GeminiContactLlmProvider implements ContactLlmProvider {

    private static final Logger log = LoggerFactory.getLogger(GeminiContactLlmProvider.class);

    private final GeminiApiClient geminiApiClient;
    private final ObjectMapper objectMapper;

    public GeminiContactLlmProvider(GeminiApiClient geminiApiClient, ObjectMapper objectMapper) {
        this.geminiApiClient = geminiApiClient;
        this.objectMapper = objectMapper;
    }

    @Override
    /** Provider identifier matching LLM_PROVIDER_ORDER entries. */
    public String id() {
        return "gemini";
    }

    @Override
    /** True when API key and model are present. */
    public boolean isConfigured() {
        return geminiApiClient.isConfigured();
    }

    @Override
    /** Runs contact research across configured LLM providers. */
    public Optional<List<ContactCard>> findContacts(
            JurisdictionContext jurisdiction,
            String scenario,
            String userMessage,
            List<String> suggestedTags,
            String legalSummary) {
        if (!isConfigured()) {
            return Optional.empty();
        }
        if (GeminiQuotaCircuitBreaker.isOpen()) {
            log.info("Gemini contact research skipped: quota circuit breaker open for this request");
            return Optional.empty();
        }

        try {
            String text = callWithGoogleSearch(jurisdiction, scenario, userMessage, suggestedTags, legalSummary);
            List<ContactCard> contacts;
            try {
                contacts = ContactResponseParser.parseContacts(objectMapper, text);
            } catch (Exception parseEx) {
                log.debug("Gemini contact google_search was not JSON ({} chars), reformatting: {}",
                        text.length(), parseEx.getMessage());
                text = callJsonReformat(jurisdiction, scenario, userMessage, suggestedTags, legalSummary, text);
                contacts = ContactResponseParser.parseContacts(objectMapper, text);
            }
            if (contacts.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(contacts);
        } catch (Exception e) {
            if (LlmHttpErrors.isQuotaExceeded(e)) {
                GeminiQuotaCircuitBreaker.open();
                log.warn("Gemini contact research quota exceeded: {}", e.getMessage());
            } else {
                log.warn("Gemini contact research failed: {}", e.getMessage());
            }
            return Optional.empty();
        }
    }

    private String callWithGoogleSearch(
            JurisdictionContext jurisdiction,
            String scenario,
            String userMessage,
            List<String> suggestedTags,
            String legalSummary) throws Exception {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("systemInstruction", Map.of(
                "parts", List.of(Map.of("text", ContactResearchPrompts.systemInstructionWithWebSearch(jurisdiction)))));
        body.put("contents", List.of(Map.of(
                "role", "user",
                "parts", List.of(Map.of(
                        "text", ContactResearchPrompts.userMessage(
                                userMessage, scenario, jurisdiction, suggestedTags, legalSummary)
                                + "\n\nIMPORTANT: Final answer must be ONLY the JSON object (no prose).")))));
        body.put("tools", List.of(Map.of("google_search", Map.of())));
        body.put("generationConfig", Map.of("temperature", 0.2));

        String responseBody = geminiApiClient.generateContent(body);
        String text = geminiApiClient.extractText(responseBody);
        if (text.isBlank()) {
            throw new IllegalStateException("Empty Gemini contact response");
        }
        return text;
    }

    private String callJsonReformat(
            JurisdictionContext jurisdiction,
            String scenario,
            String userMessage,
            List<String> suggestedTags,
            String legalSummary,
            String researchText) throws Exception {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("systemInstruction", Map.of(
                "parts", List.of(Map.of("text", ContactResearchPrompts.systemInstructionWithWebSearch(jurisdiction)))));
        body.put("contents", List.of(Map.of(
                "role", "user",
                "parts", List.of(Map.of(
                        "text", ContactResearchPrompts.jsonReformatUserMessage(researchText, jurisdiction)
                                + "\n\nOriginal request:\n"
                                + ContactResearchPrompts.userMessage(
                                        userMessage, scenario, jurisdiction, suggestedTags, legalSummary))))));
        body.put("generationConfig", Map.of(
                "temperature", 0.1,
                "responseMimeType", "application/json"));

        String responseBody = geminiApiClient.generateContent(body);
        String text = geminiApiClient.extractText(responseBody);
        if (text.isBlank()) {
            throw new IllegalStateException("Empty Gemini contact JSON reformat");
        }
        return text;
    }
}
