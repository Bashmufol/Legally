package com.legally.model.dto;

import com.legally.model.LawChunk;

import java.util.List;

public class LegalResearchResult {

    private final GeminiLegalResponse response;
    private final List<LawChunk> sources;
    private final boolean informationFound;

    public LegalResearchResult(
            GeminiLegalResponse response, List<LawChunk> sources, boolean informationFound) {
        this.response = response;
        this.sources = sources;
        this.informationFound = informationFound;
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
}
