package com.legally.llm;

import com.legally.model.ContactCard;
import com.legally.model.JurisdictionContext;
import com.legally.config.LlmChainQualifiers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MultiLlmContactResearchService {

    private static final Logger log = LoggerFactory.getLogger(MultiLlmContactResearchService.class);

    private final List<ContactLlmProvider> providers;

    public MultiLlmContactResearchService(
            @Qualifier(LlmChainQualifiers.CONTACT) List<ContactLlmProvider> providers) {
        this.providers = providers;
    }

    public List<ContactCard> findContacts(
            JurisdictionContext jurisdiction,
            String scenario,
            String userMessage,
            List<String> suggestedTags,
            String legalSummary) {

        for (ContactLlmProvider provider : providers) {
            if (!provider.isConfigured()) {
                log.debug("Contact provider {} skipped (not configured)", provider.id());
                continue;
            }
            log.info("Trying contact research provider: {}", provider.id());
            try {
                var outcome = provider.findContacts(
                        jurisdiction, scenario, userMessage, suggestedTags, legalSummary);
                if (outcome.isPresent() && !outcome.get().isEmpty()) {
                    log.info("Contact research succeeded via provider {} ({} contact(s))",
                            provider.id(), outcome.get().size());
                    return outcome.get();
                }
                log.debug("Contact provider {} returned no contacts", provider.id());
            } catch (Exception e) {
                log.warn("Contact provider {} failed: {}", provider.id(), e.getMessage());
            }
        }

        log.info("All contact LLM providers exhausted for {} — no contacts", jurisdiction.displayLabel());
        return List.of();
    }
}
