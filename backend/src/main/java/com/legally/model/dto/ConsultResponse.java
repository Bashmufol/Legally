package com.legally.model.dto;

import com.legally.model.ContactCard;
import com.legally.model.LawChunk;

import java.util.ArrayList;
import java.util.List;

/**
 * POST /api/consult response body returned to the web client.
 */
public class ConsultResponse {

    /** Plain-language overview of the legal situation. */
    private String summary;

    /** Legal points with citations, same structure as the LLM JSON output. */
    private List<GeminiLegalResponse.LegalPoint> legalAnalysis = new ArrayList<>();

    /** Practical next steps for the user. */
    private List<String> steps = new ArrayList<>();

    /** NGOs and agencies when contact research ran successfully. */
    private List<ContactCard> contacts = new ArrayList<>();

    /** Cited sources mapped from LLM chunk ids. */
    private List<LawChunk> sources = new ArrayList<>();

    /** True when the scenario may support generating a demand letter. */
    private boolean demandLetterEligible;

    /** low, medium, or high based on LLM self-assessment. */
    private String confidence;

    private String disclaimer;

    /** Resolved country name shown in the UI. */
    private String jurisdictionCountry;

    /** Resolved state or region; omitted when only country is known. */
    private String jurisdictionRegion;

    /** How jurisdiction was resolved (device, input_override, etc.). */
    private String locationSource;

    /** Legacy flag; always false in the current API. */
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
