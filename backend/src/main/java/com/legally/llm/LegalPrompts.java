package com.legally.llm;

import com.legally.model.JurisdictionContext;
import com.legally.model.LegalDocumentType;

/**
 * Prompt text for legal analysis and document generation.
 */
public final class LegalPrompts {

    private LegalPrompts() {
    }

    /** analyze system instruction. */
    public static String analyzeSystemInstruction(JurisdictionContext jurisdiction) {
        return """
                You are Legally, a legal INFORMATION assistant (not a lawyer).
                Active jurisdiction: %s (country code %s, region %s).
                Rules:
                1. Answer only for the active jurisdiction above.
                2. Use current, authoritative government and court sources where possible; put real https URLs in citation.sourceUrl when you cite them.
                3. Never invent phone numbers or email addresses.
                4. Use plain English for non-lawyers.
                5. Set demandLetterEligible true only for tenancy/contract disputes where a demand letter fits.
                6. Output ONLY valid JSON (no markdown fences):
                {
                  "summary": "string",
                  "legalAnalysis": [{"point":"string","chunkId":"optional","citation":{"instrument":"string","section":"string","jurisdiction":"string","sourceUrl":"string"}}],
                  "steps": ["string"],
                  "suggestedContactTags": ["string"],
                  "demandLetterEligible": false,
                  "confidence": "high|medium|low",
                  "disclaimer": "string"
                }
                """.formatted(
                jurisdiction.displayLabel(),
                jurisdiction.getCountryCode(),
                jurisdiction.getRegionName());
    }

    /** analyze user message. */
    public static String analyzeUserMessage(
            String userMessage, String scenario, JurisdictionContext jurisdiction) {
        String messageBlock = userMessage != null && !userMessage.isBlank()
                ? userMessage
                : "(No typed message; use attached media to understand the situation.)";
        return "Scenario: " + (scenario != null ? scenario : "general")
                + "\nJurisdiction: " + jurisdiction.displayLabel()
                + " (source: " + jurisdiction.getLocationSource() + ")"
                + "\nUser message: " + messageBlock;
    }

    /** User message for text-only LLM providers, including digest of voice/video/document uploads. */
    public static String analyzeUserMessageWithMediaDigest(
            String userMessage,
            String scenario,
            JurisdictionContext jurisdiction,
            String mediaDigest) {
        String base = analyzeUserMessage(userMessage, scenario, jurisdiction);
        if (mediaDigest == null || mediaDigest.isBlank()) {
            return base;
        }
        return base + "\n\nAttached evidence (from uploads or voice recording):\n" + mediaDigest;
    }

    /** media digest prompt. */
    public static String mediaDigestPrompt(
            String userMessage, String scenario, JurisdictionContext jurisdiction) {
        String messageBlock = userMessage != null && !userMessage.isBlank()
                ? userMessage
                : "(No typed message.)";
        return """
                You are preparing a factual digest of attached evidence for a legal information system.
                Jurisdiction: %s
                Scenario: %s
                User's typed message (if any): %s

                Inspect ALL attached files (images, video, audio/voice recording, PDF).
                Output plain text ONLY with these sections:
                1) MEDIA TYPES — list each attachment type (e.g. voice note, video, photo, PDF).
                2) FACTS — objective facts visible or audible (quotes, dates, names, locations, actions). \
                For audio, include a short transcript of relevant speech. For video/images, describe what is shown.
                3) LEGAL ISSUE — one paragraph on what legal question this evidence suggests.

                Do not give legal advice or cite statutes. Do not invent content not present in the media.
                """.formatted(
                jurisdiction.displayLabel(),
                scenario != null ? scenario : "general",
                messageBlock);
    }

    /** Used after google_search when the model returns prose instead of JSON. */
    public static String jsonReformatUserMessage(String researchText, JurisdictionContext jurisdiction) {
        return """
                Convert the legal research below into ONLY valid JSON matching the schema in the system instruction.
                Preserve every real https URL in citation.sourceUrl. Do not add markdown fences or commentary.

                Jurisdiction: %s

                Research to convert:
                %s
                """.formatted(jurisdiction.displayLabel(), researchText);
    }

    /** legal document prompt. */
    public static String legalDocumentPrompt(LegalDocumentDraftRequest request) {
        LegalDocumentType documentType = request.documentType();
        JurisdictionContext jurisdiction = request.jurisdiction();
        String docInstructions = documentInstructions(documentType);
        String parties = "";
        if (request.partyAName() != null && !request.partyAName().isBlank()) {
            parties += "Party A / first party: " + request.partyAName() + "\n";
        }
        if (request.partyBName() != null && !request.partyBName().isBlank()) {
            parties += "Party B / second party: " + request.partyBName() + "\n";
        }
        String extras = request.additionalDetails() != null && !request.additionalDetails().isBlank()
                ? "\nAdditional requirements:\n" + request.additionalDetails()
                : "";

        return """
                You are Legally, drafting a formal legal document template (NOT legal advice).

                Document type: %s
                Document title: %s
                Jurisdiction: %s (country code %s, region %s)

                %s

                Rules:
                1. Draft the complete document in clear formal English appropriate for the stated jurisdiction.
                2. Apply obligations and rights that fit the jurisdiction under current law.
                3. Use placeholders in square brackets only where user-specific data is missing (e.g. [PARTY A NAME], [DATE], [PROPERTY ADDRESS]).
                4. Include standard sections: title, parties, recitals/background, operative terms, signatures, witness lines if customary.
                5. Add a short footer line: "Generated by Legally. Review with a licensed lawyer before execution."
                6. Output ONLY the document text (no JSON, no markdown code fences).

                %s
                User facts and requirements:
                %s
                %s
                """.formatted(
                documentType.name(),
                request.title(),
                jurisdiction.displayLabel(),
                jurisdiction.getCountryCode(),
                jurisdiction.getRegionCode(),
                docInstructions,
                parties,
                request.facts(),
                extras);
    }

    private static String documentInstructions(LegalDocumentType type) {
        return switch (type) {
            case DEMAND_LETTER -> "Structure as a pre-action demand letter: date, parties, facts, legal basis, demanded remedy, deadline, signature.";
            case RENT_AGREEMENT -> "Structure as a residential tenancy/lease: premises, term, rent, deposit, repairs, termination, governing law clause.";
            case LAND_PURCHASE -> "Structure as land sale agreement: property description, purchase price, deposit, title obligations, completion, warranties, dispute resolution.";
            case PRENUPTIAL -> "Structure as prenuptial agreement: parties, disclosure, separate property, spousal support waiver/limitation, estate rights, governing law; note enforceability varies.";
            case EMPLOYMENT_CONTRACT -> "Structure as employment agreement: role, duties, compensation, benefits, confidentiality, termination, notice, governing law.";
            case GENERAL_CONTRACT -> "Structure as a general commercial contract with definitions, obligations, payment, term, breach, remedies, governing law.";
            case NDA -> "Structure as mutual or unilateral NDA: confidential information definition, obligations, exclusions, term, remedies.";
            case POWER_OF_ATTORNEY -> "Structure as limited or general power of attorney appropriate to the jurisdiction; include scope, duration, revocation.";
            case AFFIDAVIT -> "Structure as affidavit/sworn statement: deponent details, numbered paragraphs of facts, oath block, signature.";
            case OTHER -> "Draft the custom legal document described by the user with appropriate formal structure for the jurisdiction.";
        };
    }

    /** strip code fences. */
    public static String stripCodeFences(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("```", "").trim();
    }
}
