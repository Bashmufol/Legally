package com.legally.llm;

import com.legally.model.ContactCard;
import com.legally.model.JurisdictionContext;

import java.util.List;
import java.util.Optional;

/**
 * Pluggable contact discovery backend. Returns empty when the provider cannot produce contacts.
 */
public interface ContactLlmProvider {

    String id();

    boolean isConfigured();

    /**
     * Find NGOs, government bodies, and related contacts for the user's situation and jurisdiction.
     */
    Optional<List<ContactCard>> findContacts(
            JurisdictionContext jurisdiction,
            String scenario,
            String userMessage,
            List<String> suggestedTags,
            String legalSummary);
}
