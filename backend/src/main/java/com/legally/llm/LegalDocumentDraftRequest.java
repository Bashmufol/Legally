package com.legally.llm;

import com.legally.model.JurisdictionContext;
import com.legally.model.LegalDocumentType;

/**
 * Input for legal document generation.
 */
public record LegalDocumentDraftRequest(
        LegalDocumentType documentType,
        String title,
        String facts,
        String additionalDetails,
        String partyAName,
        String partyBName,
        JurisdictionContext jurisdiction) {}
