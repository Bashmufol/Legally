package com.legally.model.dto;

import com.legally.model.LawChunk;

import java.util.List;

public class LegalResearchResult {

    private final GeminiLegalResponse response;
    private final List<LawChunk> sources;
    private final boolean informationFound;
    /** When false, consult should not run contact research (e.g. media could not be read at all). */
    private final boolean suggestContacts;

    public LegalResearchResult(
            GeminiLegalResponse response, List<LawChunk> sources, boolean informationFound) {
        this(response, sources, informationFound, informationFound);
    }

    public LegalResearchResult(
            GeminiLegalResponse response,
            List<LawChunk> sources,
            boolean informationFound,
            boolean suggestContacts) {
        this.response = response;
        this.sources = sources;
        this.informationFound = informationFound;
        this.suggestContacts = suggestContacts;
    }

    public GeminiLegalResponse getResponse() {
        return response;
    }

    public List<LawChunk> getSources() {
        return sources;
    }

    public boolean isInformationFound() {
        return informationFound;
    }

    public boolean isSuggestContacts() {
        return suggestContacts;
    }
}
