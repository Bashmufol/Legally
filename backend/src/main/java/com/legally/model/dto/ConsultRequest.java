package com.legally.model.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.ArrayList;
import java.util.List;

public class ConsultRequest {

    @NotBlank
    private String message;

    private String scenario;
    private List<MediaRef> media = new ArrayList<>();

    /** ISO 3166-1 alpha-2 (e.g. NG, US) or INT for international fallback. */
    private String countryCode;
    private String countryName;
    /** State/province code or name (e.g. KWARA, CA). */
    private String regionCode;
    private String regionName;
    /** device | input_override | manual */
    private String locationSource;
    /** True when user manually set country/region in the UI. */
    private Boolean jurisdictionOverride;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getScenario() {
        return scenario;
    }

    public void setScenario(String scenario) {
        this.scenario = scenario;
    }

    public List<MediaRef> getMedia() {
        return media;
    }

    public void setMedia(List<MediaRef> media) {
        this.media = media;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    public String getRegionCode() {
        return regionCode;
    }

    public void setRegionCode(String regionCode) {
        this.regionCode = regionCode;
    }

    public String getRegionName() {
        return regionName;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }

    public String getLocationSource() {
        return locationSource;
    }

    public void setLocationSource(String locationSource) {
        this.locationSource = locationSource;
    }

    public Boolean getJurisdictionOverride() {
        return jurisdictionOverride;
    }

    public void setJurisdictionOverride(Boolean jurisdictionOverride) {
        this.jurisdictionOverride = jurisdictionOverride;
    }

    public static class MediaRef {
        private String url;
        private String mimeType;
        private String storageType;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getMimeType() {
            return mimeType;
        }

        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }

        public String getStorageType() {
            return storageType;
        }

        public void setStorageType(String storageType) {
            this.storageType = storageType;
        }
    }
}
