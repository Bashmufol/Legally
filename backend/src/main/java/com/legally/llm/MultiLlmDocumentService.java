package com.legally.llm;

import com.legally.config.LlmChainQualifiers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
/**
 * Generates legal documents through the legal LLM provider chain.
 */
public class MultiLlmDocumentService {

    private static final Logger log = LoggerFactory.getLogger(MultiLlmDocumentService.class);

    private final List<LegalLlmProvider> providers;

    public MultiLlmDocumentService(@Qualifier(LlmChainQualifiers.LEGAL) List<LegalLlmProvider> providers) {
        this.providers = providers;
    }

    /** Generates content for the request. */
    public String generate(LegalDocumentDraftRequest request) {
        for (LegalLlmProvider provider : providers) {
            if (!provider.isConfigured()) {
                continue;
            }
            try {
                Optional<String> content = provider.generateLegalDocument(request);
                if (content.isPresent() && !content.get().isBlank()) {
                    log.info("Legal document drafted via provider {}", provider.id());
                    return content.get();
                }
            } catch (Exception e) {
                log.warn("Provider {} document generation failed: {}", provider.id(), e.getMessage());
            }
        }

        log.warn("All LLM providers failed for document {}; using static template", request.documentType());
        return LegalDocumentFallback.generate(request);
    }
}
