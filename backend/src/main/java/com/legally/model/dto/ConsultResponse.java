package com.legally.model.dto;

import com.legally.model.ContactCard;
import com.legally.model.LawChunk;

import java.util.ArrayList;
import java.util.List;

/**
 * POST /api/consult response body.
 */
public class ConsultResponse {

    private String summary;
    private List<GeminiLegalResponse.LegalPoint> legalAnalysis = new ArrayList<>();
    private List<String> steps = new ArrayList<>();
    private List<ContactCard> contacts = new ArrayList<>();
    private List<LawChunk> sources = new ArrayList<>();
    private boolean demandLetterEligible;
    private String confidence;
    private String disclaimer;
    private String jurisdictionCountry;
    private String jurisdictionRegion;
    private String locationSource;
    private boolean corpusLimited;

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<GeminiLegalResponse.LegalPoint> getLegalAnalysis() {
        return legalAnalysis;
    }

    public void setLegalAnalysis(List<GeminiLegalResponse.LegalPoint> legalAnalysis) {
        this.legalAnalysis = legalAnalysis;
    }

    public List<String> getSteps() {
        return steps;
    }

    public void setSteps(List<String> steps) {
        this.steps = steps;
    }

    public List<ContactCard> getContacts() {
        return contacts;
    }

    public void setContacts(List<ContactCard> contacts) {
        this.contacts = contacts;
    }

    public List<LawChunk> getSources() {
        return sources;
    }

    public void setSources(List<LawChunk> sources) {
        this.sources = sources;
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

    public String getJurisdictionCountry() {
        return jurisdictionCountry;
    }

    public void setJurisdictionCountry(String jurisdictionCountry) {
        this.jurisdictionCountry = jurisdictionCountry;
    }

    public String getJurisdictionRegion() {
        return jurisdictionRegion;
    }

    public void setJurisdictionRegion(String jurisdictionRegion) {
        this.jurisdictionRegion = jurisdictionRegion;
    }

    public String getLocationSource() {
        return locationSource;
    }

    public void setLocationSource(String locationSource) {
        this.locationSource = locationSource;
    }

    public boolean isCorpusLimited() {
        return corpusLimited;
    }

    public void setCorpusLimited(boolean corpusLimited) {
        this.corpusLimited = corpusLimited;
    }
}
