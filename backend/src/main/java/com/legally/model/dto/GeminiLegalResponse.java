package com.legally.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Structured legal analysis JSON parsed from LLM output (Gemini or fallback providers).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeminiLegalResponse {

    private String summary = "";

    /** Individual legal points, each with an optional citation block. */
    private List<LegalPoint> legalAnalysis = new ArrayList<>();

    private List<String> steps = new ArrayList<>();

    /** Hints passed to contact research (e.g. legal_aid, police). */
    private List<String> suggestedContactTags = new ArrayList<>();

    private boolean demandLetterEligible;
    private String confidence = "medium";
    private String disclaimer = "";

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<LegalPoint> getLegalAnalysis() {
        return legalAnalysis;
    }

    public void setLegalAnalysis(List<LegalPoint> legalAnalysis) {
        this.legalAnalysis = legalAnalysis;
    }

    public List<String> getSteps() {
        return steps;
    }

    public void setSteps(List<String> steps) {
        this.steps = steps;
    }

    public List<String> getSuggestedContactTags() {
        return suggestedContactTags;
    }

    public void setSuggestedContactTags(List<String> suggestedContactTags) {
        this.suggestedContactTags = suggestedContactTags;
    }

    public boolean isDemandLetterEligible() {
        return demandLetterEligible;
    }

    public void setDemandLetterEligible(boolean demandLetterEligible) {
        this.demandLetterEligible = demandLetterEligible;
    }

    public String getConfidence() {
        return confidence;
    }

    public void setConfidence(String confidence) {
        this.confidence = confidence;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }

    /** One legal point with citation metadata for the client and source enrichment. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LegalPoint {
        private String point = "";
        private Citation citation = new Citation();

        /** Links this point to a {@link com.legally.model.LawChunk} id when present. */
        private String chunkId;

        public String getPoint() {
            return point;
        }

        public void setPoint(String point) {
            this.point = point;
        }

        public Citation getCitation() {
            return citation;
        }

        public void setCitation(Citation citation) {
            this.citation = citation;
        }

        public String getChunkId() {
            return chunkId;
        }

        public void setChunkId(String chunkId) {
            this.chunkId = chunkId;
        }
    }

    /** Statute or web source reference attached to a legal point. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Citation {
        private String instrument = "";
        private String section = "";
        private String jurisdiction = "FEDERAL";

        /** Official URL; may be filled from {@link com.legally.model.LawChunk} after parsing. */
        private String sourceUrl;

        public String getInstrument() {
            return instrument;
        }

        public void setInstrument(String instrument) {
            this.instrument = instrument;
        }

        public String getSection() {
            return section;
        }

        public void setSection(String section) {
            this.section = section;
        }

        public String getJurisdiction() {
            return jurisdiction;
        }

        public void setJurisdiction(String jurisdiction) {
            this.jurisdiction = jurisdiction;
        }

        public String getSourceUrl() {
            return sourceUrl;
        }

        public void setSourceUrl(String sourceUrl) {
            this.sourceUrl = sourceUrl;
        }
    }
}
