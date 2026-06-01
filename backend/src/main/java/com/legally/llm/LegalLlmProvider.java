package com.legally.llm;

import com.legally.model.JurisdictionContext;
import com.legally.model.LawChunk;
import com.legally.model.dto.ConsultRequest;
import com.legally.model.dto.GeminiLegalResponse;

import java.util.List;
import java.util.Optional;

/**
 * Pluggable legal analysis backend. Returns empty when the provider cannot answer.
 */
public interface LegalLlmProvider {

    String id();

    boolean isConfigured();

    /**
     * Whether this provider accepts raw media bytes in the request. Text-only providers use a
     * media digest prepared by {@link LegalMediaDigestService}.
     */
    default boolean supportsNativeMultimodal() {
        return false;
    }

    /**
     * Produce structured legal analysis for a consultation (may use web search where supported).
     */
    Optional<LlmAnalysisOutcome> analyze(
            String userMessage,
            String scenario,
            JurisdictionContext jurisdiction,
            List<ConsultRequest.MediaRef> media);

    /**
     * Draft a legal document for the given jurisdiction. Empty if this provider cannot generate.
     */
    default Optional<String> generateLegalDocument(LegalDocumentDraftRequest request) {
        return Optional.empty();
    }
}
