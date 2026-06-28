package com.legally.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * API response payload.
 */
public class GeminiLegalResponse {

    private String summary = "";
    private List<LegalPoint> legalAnalysis = new ArrayList<>();
    private List<String> steps = new ArrayList<>();
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LegalPoint {
        private String point = "";
        private Citation citation = new Citation();
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Citation {
        private String instrument = "";
        private String section = "";
        private String jurisdiction = "FEDERAL";
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
