package com.legally.llm;

import com.legally.model.JurisdictionContext;

import java.util.List;
import java.util.stream.Collectors;

public final class ContactResearchPrompts {

    private ContactResearchPrompts() {
    }

    public static String systemInstructionWithWebSearch(JurisdictionContext jurisdiction) {
        return """
                You are Legally's contact research assistant. Find REAL organizations and officials who can help with the user's legal situation.
                Active jurisdiction: %s (country %s, region %s).

                Search the web for current, authoritative contact pages. Include:
                - Government bodies (courts, ministries, police complaints, land registry, tenancy tribunals)
                - NGOs and legal aid organizations
                - Bar associations, ombudsmen, human-rights commissions
                - Reputable legal aid clinics or licensed practitioners when clearly relevant

                Rules:
                1. Every entry MUST have a non-empty name and a sourceUrl (https:// preferred; http:// or official domain accepted).
                2. Include phone numbers, emails, or social handles when they appear on the cited page — do not invent numbers.
                3. If you only find an official homepage or contact page, include it with empty phones/emails/social — that is valid.
                4. Social values may be full https URLs or official @handles (e.g. @NHRCNigeria).
                5. Return 3–8 contacts when possible (at least 2 if you can), most relevant first. Only return an empty contacts array if you truly find nothing relevant.
                6. Output ONLY valid JSON (no markdown):
                {
                  "contacts": [
                    {
                      "name": "Organization or person name",
                      "role": "What they do for this situation",
                      "organizationType": "government|ngo|organization|legal_practitioner|other",
                      "tags": ["legal_aid","police"],
                      "phones": ["+234..."],
                      "emails": ["info@..."],
                      "social": {"X": "https://...", "Facebook": "https://..."},
                      "sourceUrl": "https://official...",
                      "notes": "Optional: hours, verify before calling"
                    }
                  ]
                }
                """.formatted(
                jurisdiction.displayLabel(),
                jurisdiction.getCountryCode(),
                jurisdiction.getRegionName());
    }

    /** For providers without live web search (Groq, OpenRouter, Mistral, etc.). */
    public static String systemInstructionKnowledgeOnly(JurisdictionContext jurisdiction) {
        return """
                You are Legally's contact research assistant. Find REAL organizations who can help with the user's legal situation.
                Active jurisdiction: %s (country %s, region %s).

                You do NOT have live web access. List well-known government bodies, legal aid councils, bar associations, ombudsmen, and NGOs for this jurisdiction that plausibly help with the scenario.

                Rules:
                1. Every entry MUST have a non-empty name and a sourceUrl (https:// official homepage or contact page you are confident exists).
                2. Add phones, emails, or social only when you are confident they are published — otherwise leave those arrays empty.
                3. Homepage-only entries (name + sourceUrl, no phone/email) are encouraged when you know the organization but not exact numbers.
                4. Return 3–8 contacts when possible (at least 2). Do not return an empty array unless no relevant body exists for this jurisdiction.
                5. Output ONLY valid JSON (no markdown):
                {
                  "contacts": [
                    {
                      "name": "Organization or person name",
                      "role": "What they do for this situation",
                      "organizationType": "government|ngo|organization|legal_practitioner|other",
                      "tags": ["legal_aid","police"],
                      "phones": [],
                      "emails": [],
                      "social": {},
                      "sourceUrl": "https://official...",
                      "notes": "Verify current details on the official site before contacting"
                    }
                  ]
                }
                """.formatted(
                jurisdiction.displayLabel(),
                jurisdiction.getCountryCode(),
                jurisdiction.getRegionName());
    }

    public static String userMessage(
            String userMessage,
            String scenario,
            JurisdictionContext jurisdiction,
            List<String> suggestedTags,
            String legalSummary) {
        String messageBlock = userMessage != null && !userMessage.isBlank()
                ? userMessage
                : "(No typed message.)";
        String tags = suggestedTags == null || suggestedTags.isEmpty()
                ? "(none)"
                : suggestedTags.stream().collect(Collectors.joining(", "));
        String summaryBlock = legalSummary != null && !legalSummary.isBlank()
                ? "\nLegal analysis summary (for context):\n" + legalSummary
                : "";

        return """
                Scenario: %s
                Jurisdiction: %s
                Suggested contact categories: %s
                User message: %s
                %s

                List official and NGO contacts in this jurisdiction for this situation.
                Each contact needs at least name and sourceUrl. Add phone, email, or social when you know them; otherwise use the official website link only.
                """.formatted(
                scenario != null ? scenario : "general",
                jurisdiction.displayLabel(),
                tags,
                messageBlock,
                summaryBlock);
    }

    public static String jsonReformatUserMessage(String researchText, JurisdictionContext jurisdiction) {
        return """
                Convert the contact research below into ONLY valid JSON matching the schema in the system instruction.
                Preserve every sourceUrl. Keep entries even when phones/emails/social are empty.

                Jurisdiction: %s

                Research to convert:
                %s
                """.formatted(jurisdiction.displayLabel(), researchText);
    }
}
