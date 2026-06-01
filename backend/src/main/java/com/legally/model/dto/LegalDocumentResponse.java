package com.legally.model.dto;

public class LegalDocumentResponse {

    private String documentType;
    private String title;
    private String content;
    private String disclaimer;
    private String jurisdictionCountry;
    private String jurisdictionRegion;
    private String locationSource;

    public LegalDocumentResponse() {
    }

    public LegalDocumentResponse(
            String documentType,
            String title,
            String content,
            String disclaimer,
            String jurisdictionCountry,
            String jurisdictionRegion,
            String locationSource) {
        this.documentType = documentType;
        this.title = title;
        this.content = content;
        this.disclaimer = disclaimer;
        this.jurisdictionCountry = jurisdictionCountry;
        this.jurisdictionRegion = jurisdictionRegion;
        this.locationSource = locationSource;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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
}
