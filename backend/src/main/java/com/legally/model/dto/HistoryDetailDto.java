package com.legally.model.dto;

/**
 * API response payload.
 */
public class HistoryDetailDto {

    private String id;
    private String scenario;
    private String question;
    private String createdAt;
    private ConsultResponse response;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getScenario() {
        return scenario;
    }

    public void setScenario(String scenario) {
        this.scenario = scenario;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public ConsultResponse getResponse() {
        return response;
    }

    public void setResponse(ConsultResponse response) {
        this.response = response;
    }
}
