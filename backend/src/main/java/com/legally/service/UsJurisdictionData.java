package com.legally.service;

import com.legally.model.JurisdictionContext;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/** US states and territories for jurisdiction extraction from user text. */
final class UsJurisdictionData {

    private static final Map<String, String> STATE_ALIASES = buildStateAliases();
    private static final Map<String, String> CITY_ALIASES = buildCityAliases();
    private static final Map<String, String> CODE_TO_NAME = buildCodeToName();

    private UsJurisdictionData() {
    }

    static Optional<JurisdictionContext> extractFromText(String lowerText) {
        Optional<JurisdictionContext> state = matchAliases(lowerText, STATE_ALIASES);
        if (state.isPresent()) {
            return state;
        }
        return matchAliases(lowerText, CITY_ALIASES);
    }

    static String displayName(String regionCode) {
        if (regionCode == null || regionCode.isBlank()) {
            return "";
        }
        return CODE_TO_NAME.getOrDefault(regionCode.toUpperCase(Locale.ROOT), regionCode);
    }

    private static Optional<JurisdictionContext> matchAliases(String lowerText, Map<String, String> aliases) {
        return aliases.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()))
                .filter(e -> containsWord(lowerText, e.getKey()))
                .findFirst()
                .map(e -> usRegionContext(e.getValue()));
    }

    private static JurisdictionContext usRegionContext(String regionCode) {
        JurisdictionContext ctx = new JurisdictionContext();
        ctx.setCountryCode("US");
        ctx.setCountryName("United States");
        ctx.setRegionCode(regionCode);
        ctx.setRegionName(displayName(regionCode));
        return ctx;
    }

    private static boolean containsWord(String lowerText, String alias) {
        if (alias.contains(" ")) {
            return lowerText.contains(alias);
        }
        return Pattern.compile("\\b" + Pattern.quote(alias) + "\\b", Pattern.CASE_INSENSITIVE)
                .matcher(lowerText)
                .find();
    }

    private static Map<String, String> buildStateAliases() {
        LinkedHashMap<String, String> m = new LinkedHashMap<>();
        state(m, "alabama", "AL");
        state(m, "alaska", "AK");
        state(m, "arizona", "AZ");
        state(m, "arkansas", "AR");
        state(m, "california", "CA");
        state(m, "colorado", "CO");
        state(m, "connecticut", "CT");
        state(m, "delaware", "DE");
        state(m, "florida", "FL");
        state(m, "georgia", "GA");
        state(m, "hawaii", "HI");
        state(m, "idaho", "ID");
        state(m, "illinois", "IL");
        state(m, "indiana", "IN");
        state(m, "iowa", "IA");
        state(m, "kansas", "KS");
        state(m, "kentucky", "KY");
        state(m, "louisiana", "LA");
        state(m, "maine", "ME");
        state(m, "maryland", "MD");
        state(m, "massachusetts", "MA");
        state(m, "michigan", "MI");
        state(m, "minnesota", "MN");
        state(m, "mississippi", "MS");
        state(m, "missouri", "MO");
        state(m, "montana", "MT");
        state(m, "nebraska", "NE");
        state(m, "nevada", "NV");
        state(m, "new hampshire", "NH");
        state(m, "new jersey", "NJ");
        state(m, "new mexico", "NM");
        state(m, "new york", "NY");
        state(m, "north carolina", "NC");
        state(m, "north dakota", "ND");
        state(m, "ohio", "OH");
        state(m, "oklahoma", "OK");
        state(m, "oregon", "OR");
        state(m, "pennsylvania", "PA");
        state(m, "rhode island", "RI");
        state(m, "south carolina", "SC");
        state(m, "south dakota", "SD");
        state(m, "tennessee", "TN");
        state(m, "texas", "TX");
        state(m, "utah", "UT");
        state(m, "vermont", "VT");
        state(m, "virginia", "VA");
        state(m, "washington", "WA");
        state(m, "west virginia", "WV");
        state(m, "wisconsin", "WI");
        state(m, "wyoming", "WY");
        state(m, "district of columbia", "DC");
        state(m, "washington dc", "DC");
        state(m, "washington d.c.", "DC");
        state(m, "d.c.", "DC");
        return Map.copyOf(m);
    }

    private static Map<String, String> buildCityAliases() {
        LinkedHashMap<String, String> m = new LinkedHashMap<>();
        state(m, "houston", "TX");
        state(m, "dallas", "TX");
        state(m, "austin", "TX");
        state(m, "los angeles", "CA");
        state(m, "san francisco", "CA");
        state(m, "new york city", "NY");
        state(m, "nyc", "NY");
        state(m, "chicago", "IL");
        state(m, "miami", "FL");
        return Map.copyOf(m);
    }

    private static Map<String, String> buildCodeToName() {
        LinkedHashMap<String, String> names = new LinkedHashMap<>();
        names.put("AL", "Alabama");
        names.put("AK", "Alaska");
        names.put("AZ", "Arizona");
        names.put("AR", "Arkansas");
        names.put("CA", "California");
        names.put("CO", "Colorado");
        names.put("CT", "Connecticut");
        names.put("DE", "Delaware");
        names.put("FL", "Florida");
        names.put("GA", "Georgia");
        names.put("HI", "Hawaii");
        names.put("ID", "Idaho");
        names.put("IL", "Illinois");
        names.put("IN", "Indiana");
        names.put("IA", "Iowa");
        names.put("KS", "Kansas");
        names.put("KY", "Kentucky");
        names.put("LA", "Louisiana");
        names.put("ME", "Maine");
        names.put("MD", "Maryland");
        names.put("MA", "Massachusetts");
        names.put("MI", "Michigan");
        names.put("MN", "Minnesota");
        names.put("MS", "Mississippi");
        names.put("MO", "Missouri");
        names.put("MT", "Montana");
        names.put("NE", "Nebraska");
        names.put("NV", "Nevada");
        names.put("NH", "New Hampshire");
        names.put("NJ", "New Jersey");
        names.put("NM", "New Mexico");
        names.put("NY", "New York");
        names.put("NC", "North Carolina");
        names.put("ND", "North Dakota");
        names.put("OH", "Ohio");
        names.put("OK", "Oklahoma");
        names.put("OR", "Oregon");
        names.put("PA", "Pennsylvania");
        names.put("RI", "Rhode Island");
        names.put("SC", "South Carolina");
        names.put("SD", "South Dakota");
        names.put("TN", "Tennessee");
        names.put("TX", "Texas");
        names.put("UT", "Utah");
        names.put("VT", "Vermont");
        names.put("VA", "Virginia");
        names.put("WA", "Washington");
        names.put("WV", "West Virginia");
        names.put("WI", "Wisconsin");
        names.put("WY", "Wyoming");
        names.put("DC", "District of Columbia");
        return Map.copyOf(names);
    }

    private static void state(Map<String, String> m, String alias, String code) {
        m.put(alias, code);
    }

    private static String titleCase(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
