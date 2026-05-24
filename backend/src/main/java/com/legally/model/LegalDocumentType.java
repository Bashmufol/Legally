package com.legally.model;

import java.util.Locale;

public enum LegalDocumentType {
    DEMAND_LETTER("Demand Letter"),
    RENT_AGREEMENT("Residential Rent / Lease Agreement"),
    LAND_PURCHASE("Land Purchase Agreement"),
    PRENUPTIAL("Prenuptial Agreement"),
    EMPLOYMENT_CONTRACT("Employment Contract"),
    GENERAL_CONTRACT("General Contract Agreement"),
    NDA("Non-Disclosure Agreement (NDA)"),
    POWER_OF_ATTORNEY("Power of Attorney"),
    AFFIDAVIT("Affidavit / Sworn Statement"),
    OTHER("Custom Legal Document");

    private final String displayName;

    LegalDocumentType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static LegalDocumentType fromApiValue(String raw) {
        if (raw == null || raw.isBlank()) {
            return OTHER;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        try {
            return LegalDocumentType.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            return OTHER;
        }
    }

    public String corpusScenario() {
        return switch (this) {
            case DEMAND_LETTER, RENT_AGREEMENT -> "tenancy";
            case LAND_PURCHASE -> "land";
            case PRENUPTIAL -> "family";
            case EMPLOYMENT_CONTRACT -> "employment";
            case GENERAL_CONTRACT, NDA, POWER_OF_ATTORNEY -> "business_contract";
            case AFFIDAVIT -> "general";
            case OTHER -> "general";
        };
    }
}
