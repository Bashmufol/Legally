package com.legally.service;

import com.legally.llm.MultiLlmContactResearchService;
import com.legally.model.ContactCard;
import com.legally.model.JurisdictionContext;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Discovers contacts via the multi-LLM chain (same order as {@code LLM_PROVIDER_ORDER}).
 */
@Service
public class ContactResearchService {

    private final MultiLlmContactResearchService multiLlmContactResearch;

    public ContactResearchService(MultiLlmContactResearchService multiLlmContactResearch) {
        this.multiLlmContactResearch = multiLlmContactResearch;
    }

    /** Runs contact research across configured LLM providers. */
    public List<ContactCard> findContacts(
            JurisdictionContext jurisdiction,
            String scenario,
            String userMessage,
            List<String> suggestedContactTags,
            String legalSummary) {
        return multiLlmContactResearch.findContacts(
                jurisdiction, scenario, userMessage, suggestedContactTags, legalSummary);
    }
}
