package com.legally.model.dto;

import java.util.ArrayList;
import java.util.List;

public class ConsultRequest {

    private String message;

    private String scenario;
    private List<MediaRef> media = new ArrayList<>();

    private String countryCode;
    private String countryName;
    private String regionCode;
    private String regionName;
    private String locationSource;
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
