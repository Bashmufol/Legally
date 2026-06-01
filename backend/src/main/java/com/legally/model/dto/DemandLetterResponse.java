package com.legally.model.dto;

public class DemandLetterResponse {

    private String letter;
    private String disclaimer;

    public DemandLetterResponse() {
    }

    public DemandLetterResponse(String letter, String disclaimer) {
        this.letter = letter;
        this.disclaimer = disclaimer;
    }

    public String getLetter() {
        return letter;
    }

    public void setLetter(String letter) {
        this.letter = letter;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }
}
