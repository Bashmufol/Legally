package com.legally.model.dto;

import jakarta.validation.constraints.NotBlank;

public class DemandLetterRequest {

    @NotBlank
    private String facts;

    private String recipientName;
    private String senderName;
    private String scenario = "tenancy";

    public String getFacts() {
        return facts;
    }

    public void setFacts(String facts) {
        this.facts = facts;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getScenario() {
        return scenario;
    }

    public void setScenario(String scenario) {
        this.scenario = scenario;
    }
}
