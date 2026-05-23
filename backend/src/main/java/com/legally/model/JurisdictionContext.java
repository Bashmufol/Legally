package com.legally.model;

/**
 * Resolved country + region for legal analysis and corpus retrieval.
 */
public class JurisdictionContext {

    public enum LocationSource {
        device,
        input_override,
        manual,
        default_fallback
    }

    private String countryCode = "INT";
    private String countryName = "International";
    private String regionCode = "GENERAL";
    private String regionName = "General";
    private LocationSource locationSource = LocationSource.default_fallback;
    private boolean corpusLimited;

    public JurisdictionContext() {
    }

    public JurisdictionContext(
            String countryCode,
            String countryName,
            String regionCode,
            String regionName,
            LocationSource locationSource,
            boolean corpusLimited) {
        this.countryCode = countryCode;
        this.countryName = countryName;
        this.regionCode = regionCode;
        this.regionName = regionName;
        this.locationSource = locationSource;
        this.corpusLimited = corpusLimited;
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

    public LocationSource getLocationSource() {
        return locationSource;
    }

    public void setLocationSource(LocationSource locationSource) {
        this.locationSource = locationSource;
    }

    public boolean isCorpusLimited() {
        return corpusLimited;
    }

    public void setCorpusLimited(boolean corpusLimited) {
        this.corpusLimited = corpusLimited;
    }

    public String displayLabel() {
        if (regionCode != null && !regionCode.isBlank() && !"GENERAL".equalsIgnoreCase(regionCode)) {
            return countryName + ", " + regionName;
        }
        return countryName;
    }
}
