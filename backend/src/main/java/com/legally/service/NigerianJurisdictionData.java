package com.legally.service;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Nigerian states, cities, and phrase patterns for jurisdiction extraction from user text.
 */
final class NigerianJurisdictionData {

    private static final Pattern NG_STATE_PHRASE =
            Pattern.compile("\\b([a-z][a-z\\s]{2,28}?)\\s+state\\b", Pattern.CASE_INSENSITIVE);

    /** Canonical region codes (values in alias map). */
    static final Set<String> REGION_CODES = Set.of(
            "ABIA", "ADAMAWA", "AKWA_IBOM", "ANAMBRA", "BAUCHI", "BAYELSA", "BENUE", "BORNO",
            "CROSS_RIVER", "DELTA", "EBONYI", "EDO", "EKITI", "ENUGU", "FCT", "FEDERAL", "GOMBE",
            "IMO", "JIGAWA", "KADUNA", "KANO", "KATSINA", "KEBBI", "KOGI", "KWARA", "LAGOS",
            "NASARAWA", "NIGER", "OGUN", "ONDO", "OSUN", "OYO", "PLATEAU", "RIVERS", "SOKOTO",
            "TARABA", "YOBE", "ZAMFARA");

    private static final Map<String, String> ALIASES = buildAliases();

    private NigerianJurisdictionData() {
    }

    static Map<String, String> aliases() {
        return ALIASES;
    }

    static Pattern statePhrasePattern() {
        return NG_STATE_PHRASE;
    }

    /**
     * Parse phrases like "abia state" or "cross river state" into a region code, or null if unknown.
     */
    static String regionCodeFromStatePhrase(String lowerText) {
        var matcher = NG_STATE_PHRASE.matcher(lowerText);
        while (matcher.find()) {
            String phrase = matcher.group(1).trim().replaceAll("\\s+", " ");
            String code = regionCodeFromNameToken(phrase);
            if (code != null) {
                return code;
            }
        }
        return null;
    }

    static String regionCodeFromNameToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String key = token.toLowerCase(Locale.ROOT).trim();
        if (ALIASES.containsKey(key)) {
            return ALIASES.get(key);
        }
        String normalized = key.replace(' ', '_').toUpperCase(Locale.ROOT);
        if (REGION_CODES.contains(normalized)) {
            return normalized;
        }
        return null;
    }

    private static Map<String, String> buildAliases() {
        LinkedHashMap<String, String> m = new LinkedHashMap<>();
        state(m, "abia", "ABIA");
        state(m, "adamawa", "ADAMAWA");
        state(m, "akwa ibom", "AKWA_IBOM");
        state(m, "akwa-ibom", "AKWA_IBOM");
        state(m, "anambra", "ANAMBRA");
        state(m, "bauchi", "BAUCHI");
        state(m, "bayelsa", "BAYELSA");
        state(m, "benue", "BENUE");
        state(m, "borno", "BORNO");
        state(m, "cross river", "CROSS_RIVER");
        state(m, "delta", "DELTA");
        state(m, "ebonyi", "EBONYI");
        state(m, "edo", "EDO");
        state(m, "ekiti", "EKITI");
        state(m, "enugu", "ENUGU");
        state(m, "gombe", "GOMBE");
        state(m, "imo", "IMO");
        state(m, "jigawa", "JIGAWA");
        state(m, "kaduna", "KADUNA");
        state(m, "kano", "KANO");
        state(m, "katsina", "KATSINA");
        state(m, "kebbi", "KEBBI");
        state(m, "kogi", "KOGI");
        state(m, "kwara", "KWARA");
        state(m, "lagos", "LAGOS");
        state(m, "nasarawa", "NASARAWA");
        state(m, "niger", "NIGER");
        state(m, "ogun", "OGUN");
        state(m, "ondo", "ONDO");
        state(m, "osun", "OSUN");
        state(m, "oyo", "OYO");
        state(m, "plateau", "PLATEAU");
        state(m, "rivers", "RIVERS");
        state(m, "sokoto", "SOKOTO");
        state(m, "taraba", "TARABA");
        state(m, "yobe", "YOBE");
        state(m, "zamfara", "ZAMFARA");
        state(m, "abuja", "FCT");
        state(m, "fct", "FCT");
        state(m, "federal capital", "FCT");
        state(m, "federal capital territory", "FCT");
        // Major cities → state
        state(m, "ilorin", "KWARA");
        state(m, "ibadan", "OYO");
        state(m, "port harcourt", "RIVERS");
        state(m, "uyo", "AKWA_IBOM");
        state(m, "calabar", "CROSS_RIVER");
        state(m, "enugu city", "ENUGU");
        state(m, "owerri", "IMO");
        state(m, "aba", "ABIA");
        state(m, "umuahia", "ABIA");
        return Map.copyOf(m);
    }

    private static void state(Map<String, String> m, String alias, String code) {
        m.put(alias, code);
    }
}
