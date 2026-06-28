package com.legally.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.legally.model.JurisdictionContext;
import com.legally.model.LawChunk;
import com.legally.model.dto.ConsultRequest;
import com.legally.model.dto.GeminiLegalResponse;
import com.legally.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Gemini provider for legal analysis with native multimodal input and Google Search grounding.
 */
public class GeminiLegalLlmProvider implements LegalLlmProvider {

    private static final Logger log = LoggerFactory.getLogger(GeminiLegalLlmProvider.class);

    private final GeminiApiClient geminiApiClient;
    private final StorageService storageService;
    private final ObjectMapper objectMapper;

    public GeminiLegalLlmProvider(
            GeminiApiClient geminiApiClient, StorageService storageService, ObjectMapper objectMapper) {
        this.geminiApiClient = geminiApiClient;
        this.storageService = storageService;
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
    /** True when raw media bytes can be sent to this provider. */
    public boolean supportsNativeMultimodal() {
        return true;
    }

    @Override
    /** Calls the provider for legal analysis. */
    public Optional<LlmAnalysisOutcome> analyze(
            String userMessage,
            String scenario,
            JurisdictionContext jurisdiction,
            List<ConsultRequest.MediaRef> media) {
        if (!isConfigured()) {
            return Optional.empty();
        }
        if (GeminiQuotaCircuitBreaker.isOpen()) {
            log.info("Gemini legal analysis skipped: quota circuit breaker open for this request");
            return Optional.empty();
        }

        try {
            GeminiLegalResponse parsed = callWithGoogleSearch(userMessage, scenario, jurisdiction, media);
            if (!LlmResponseParser.hasSubstantiveLegalContent(parsed)) {
                return Optional.empty();
            }
            List<LawChunk> sources = LlmResponseParser.sourcesFromCitations(parsed, jurisdiction, id());
            return Optional.of(new LlmAnalysisOutcome(parsed, sources, id()));
        } catch (Exception e) {
            if (LlmHttpErrors.isQuotaExceeded(e)) {
                GeminiQuotaCircuitBreaker.open();
                log.warn("Gemini legal analysis skipped (quota exceeded); trying next provider");
            } else {
                log.warn("Gemini legal analysis failed: {}", e.getMessage());
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
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                    "generationConfig", Map.of("temperature", 0.25));

            String responseBody = geminiApiClient.generateContent(body);
            String text = LegalPrompts.stripCodeFences(geminiApiClient.extractText(responseBody));
            if (text.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(text);
        } catch (Exception e) {
            log.warn("Gemini document generation failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private GeminiLegalResponse callWithGoogleSearch(
            String userMessage,
            String scenario,
            JurisdictionContext jurisdiction,
            List<ConsultRequest.MediaRef> media) throws Exception {

        String textPrompt = LegalPrompts.analyzeUserMessage(userMessage, scenario, jurisdiction)
                + "\n\nSearch for official government and court sources for this jurisdiction. Cite sourceUrl for each point."
                + "\n\nIMPORTANT: Your final answer must be ONLY the JSON object from the system instruction (no prose before or after).";
        List<Map<String, Object>> parts = LlmMediaAttachment.buildParts(textPrompt, media, storageService);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("systemInstruction", Map.of("parts", List.of(Map.of("text", LegalPrompts.analyzeSystemInstruction(jurisdiction)))));
        body.put("contents", List.of(Map.of("role", "user", "parts", parts)));
        body.put("tools", List.of(Map.of("google_search", Map.of())));
        body.put("generationConfig", Map.of("temperature", 0.2));

        String responseBody = geminiApiClient.generateContent(body);
        String text = geminiApiClient.extractText(responseBody);
        if (text.isBlank()) {
            throw new IllegalStateException("Empty Gemini response");
        }

        GeminiLegalResponse parsed;
        try {
            parsed = LlmResponseParser.parseJsonResponse(objectMapper, text, jurisdiction);
        } catch (Exception parseEx) {
            log.debug("Gemini google_search returned non-JSON ({} chars), reformatting: {}",
                    text.length(), parseEx.getMessage());
            try {
                text = callJsonReformat(userMessage, scenario, jurisdiction, text);
                parsed = LlmResponseParser.parseJsonResponse(objectMapper, text, jurisdiction);
            } catch (Exception reformatEx) {
                if (LlmHttpErrors.isQuotaExceeded(reformatEx)) {
                    throw reformatEx;
                }
                throw parseEx;
            }
        }
        if (parsed.getDisclaimer() == null || parsed.getDisclaimer().isBlank()) {
            parsed.setDisclaimer("Legally provides general legal information only, not legal advice. "
                    + "Sources retrieved via Google Search; verify URLs. Consult a licensed lawyer in "
                    + jurisdiction.getCountryName() + ".");
        }
        return parsed;
    }

    /** Second pass without google_search so responseMimeType application/json can be used. */
    private String callJsonReformat(
            String userMessage,
            String scenario,
            JurisdictionContext jurisdiction,
            String researchText) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("systemInstruction", Map.of(
                "parts", List.of(Map.of("text", LegalPrompts.analyzeSystemInstruction(jurisdiction)))));
        body.put("contents", List.of(Map.of(
                "role", "user",
                "parts", List.of(Map.of(
                        "text", LegalPrompts.jsonReformatUserMessage(researchText, jurisdiction)
                                + "\n\nOriginal question context:\n"
                                + LegalPrompts.analyzeUserMessage(userMessage, scenario, jurisdiction))))));
        body.put("generationConfig", Map.of(
                "temperature", 0.1,
                "responseMimeType", "application/json"));

        String responseBody = geminiApiClient.generateContent(body);
        String text = geminiApiClient.extractText(responseBody);
        if (text.isBlank()) {
            throw new IllegalStateException("Empty Gemini JSON reformat response");
        }
        return text;
    }
}
