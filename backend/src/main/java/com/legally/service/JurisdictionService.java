package com.legally.service;

import com.legally.model.JurisdictionContext;
import com.legally.model.JurisdictionContext.LocationSource;
import com.legally.model.dto.ConsultRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Resolves country and region from device fields, message text, or Gemini detection.
 */
@Service
public class JurisdictionService {


    private static final Map<String, String> COUNTRY_NAMES = Map.ofEntries(
            Map.entry("NG", "Nigeria"),
            Map.entry("US", "United States"),
            Map.entry("GB", "United Kingdom"),
            Map.entry("CA", "Canada"),
            Map.entry("IN", "India"),
            Map.entry("KE", "Kenya"),
            Map.entry("GH", "Ghana"),
            Map.entry("ZA", "South Africa"),
            Map.entry("INT", "International")
    );

    private static final List<Map.Entry<String, String>> NG_ALIASES_LONGEST_FIRST =
            NigerianJurisdictionData.aliases().entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()))
                    .toList();

    private static final List<CountryMention> COUNTRY_MENTIONS = List.of(
            new CountryMention(Pattern.compile("\\bnigeri(a|an)\\b", Pattern.CASE_INSENSITIVE), "NG", "Nigeria"),
            new CountryMention(Pattern.compile("\\b(united states|u\\.?s\\.?a?|america)\\b", Pattern.CASE_INSENSITIVE), "US", "United States"),
            new CountryMention(Pattern.compile("\\b(united kingdom|u\\.?k\\.?)\\b", Pattern.CASE_INSENSITIVE), "GB", "United Kingdom"),
            new CountryMention(Pattern.compile("\\bcanada\\b", Pattern.CASE_INSENSITIVE), "CA", "Canada"),
            new CountryMention(Pattern.compile("\\bindia\\b", Pattern.CASE_INSENSITIVE), "IN", "India"),
            new CountryMention(Pattern.compile("\\bkenya\\b", Pattern.CASE_INSENSITIVE), "KE", "Kenya"),
            new CountryMention(Pattern.compile("\\bghana\\b", Pattern.CASE_INSENSITIVE), "GH", "Ghana"),
            new CountryMention(Pattern.compile("\\bsouth africa\\b", Pattern.CASE_INSENSITIVE), "ZA", "South Africa")
    );

    /** Determines jurisdiction from request fields and message text. */
    public JurisdictionContext resolve(ConsultRequest request) {
        String message = request.getMessage() != null ? request.getMessage() : "";

        Optional<JurisdictionContext> fromMessage = extractFromText(message);
        if (fromMessage.isPresent()) {
            JurisdictionContext ctx = fromMessage.get();
            ctx.setLocationSource(LocationSource.input_override);
            enrichNames(ctx);
            ctx.setCorpusLimited(false);
            return ctx;
        }

        if (hasDeviceFields(request)) {
            JurisdictionContext ctx = fromRequestFields(request);
            ctx.setLocationSource(LocationSource.device);
            enrichNames(ctx);
            ctx.setCorpusLimited(false);
            return ctx;
        }

        return defaultContext();
    }

    /** apply detected override. */
    public JurisdictionContext applyDetectedOverride(JurisdictionContext detected) {
        detected.setLocationSource(LocationSource.input_override);
        stripGenericRegion(detected);
        enrichNames(detected);
        detected.setCorpusLimited(false);
        return detected;
    }

    private void stripGenericRegion(JurisdictionContext ctx) {
        if (isGenericRegion(ctx.getRegionName(), ctx.getRegionCode())) {
            ctx.setRegionCode("");
            ctx.setRegionName("");
        }
    }

    /** disclaimer for. */
    public String disclaimerFor(JurisdictionContext ctx) {
        return "Legally provides general legal information only, not legal advice. "
                + "Consult a licensed lawyer in " + ctx.getCountryName() + " for your specific case.";
    }

    /**
     * Only accept Gemini-detected jurisdiction when it clearly differs from device/default and is user-driven.
     */
    public boolean isExplicitUserJurisdiction(JurisdictionContext detected, JurisdictionContext baseline) {
        if (detected == null || detected.getCountryCode() == null) {
            return false;
        }
        if (baseline.getLocationSource() == LocationSource.default_fallback) {
            return true;
        }
        if (baseline.getLocationSource() == LocationSource.device) {
            String dCountry = detected.getCountryCode().toUpperCase(Locale.ROOT);
            String bCountry = baseline.getCountryCode() != null
                    ? baseline.getCountryCode().toUpperCase(Locale.ROOT)
                    : "";
            if (!dCountry.equals(bCountry)) {
                return true;
            }
            String dRegion = normalizeRegionCode(detected.getRegionCode());
            String bRegion = normalizeRegionCode(baseline.getRegionCode());
            return !dRegion.isBlank() && !dRegion.equals(bRegion) && !"GENERAL".equals(dRegion);
        }
        return true;
    }

    /**
     * Parses country/state from free text (message or transcript). Used before device location and for overrides.
     */
    public Optional<JurisdictionContext> extractFromUserMessage(String text) {
        return extractFromText(text);
    }

    private Optional<JurisdictionContext> extractFromText(String text) {
        if (text.isBlank()) {
            return Optional.empty();
        }
        String lower = text.toLowerCase(Locale.ROOT);

        Optional<JurisdictionContext> us = UsJurisdictionData.extractFromText(lower);
        if (us.isPresent()) {
            return us;
        }

        for (Map.Entry<String, String> e : NG_ALIASES_LONGEST_FIRST) {
            if (containsNgStateMention(lower, e.getKey())) {
                return Optional.of(nigeriaRegionContext(e.getValue()));
            }
        }

        String fromStatePhrase = NigerianJurisdictionData.regionCodeFromStatePhrase(lower);
        if (fromStatePhrase != null) {
            return Optional.of(nigeriaRegionContext(fromStatePhrase));
        }

        String countryCode = null;
        String countryName = null;
        for (CountryMention m : COUNTRY_MENTIONS) {
            if (m.pattern.matcher(text).find()) {
                countryCode = m.code;
                countryName = m.name;
                break;
            }
        }

        String regionCode = null;
        String regionName = null;
        if ("US".equals(countryCode)) {
            Optional<JurisdictionContext> usRegion = UsJurisdictionData.extractFromText(lower);
            if (usRegion.isPresent()) {
                regionCode = usRegion.get().getRegionCode();
                regionName = usRegion.get().getRegionName();
            }
        }
        if ("NG".equals(countryCode)) {
            for (Map.Entry<String, String> e : NG_ALIASES_LONGEST_FIRST) {
                if (containsNgStateMention(lower, e.getKey())) {
                    regionCode = e.getValue();
                    regionName = humanRegionName(e.getValue());
                    break;
                }
            }
            if (regionCode == null) {
                String fromPhrase = NigerianJurisdictionData.regionCodeFromStatePhrase(lower);
                if (fromPhrase != null) {
                    regionCode = fromPhrase;
                    regionName = humanRegionName(fromPhrase);
                }
            }
            if (lower.contains("federal") && regionCode == null) {
                regionCode = "FEDERAL";
                regionName = "Federal";
            }
        }

        if (countryCode == null && regionCode == null) {
            return Optional.empty();
        }
        if (countryCode == null) {
            countryCode = "NG";
            countryName = "Nigeria";
        }

        if (regionCode == null || regionCode.isBlank()) {
            Optional<SubregionFromText.ExtractedSubregion> subregion =
                    SubregionFromText.extract(text, countryCode, countryName);
            if (subregion.isPresent()) {
                regionCode = subregion.get().code();
                regionName = subregion.get().displayName();
            }
        }

        JurisdictionContext ctx = new JurisdictionContext();
        ctx.setCountryCode(countryCode);
        ctx.setCountryName(countryName);
        applyRegion(ctx, regionCode, regionName);
        return Optional.of(ctx);
    }

    private void applyRegion(JurisdictionContext ctx, String regionCode, String regionName) {
        if (regionCode != null && !regionCode.isBlank() && !"GENERAL".equalsIgnoreCase(regionCode)) {
            ctx.setRegionCode(regionCode);
            ctx.setRegionName(regionName != null && !regionName.isBlank()
                    ? regionName
                    : regionDisplayName(ctx.getCountryCode(), regionCode));
            return;
        }
        ctx.setRegionCode("");
        ctx.setRegionName("");
    }

    private JurisdictionContext fromRequestFields(ConsultRequest request) {
        JurisdictionContext ctx = new JurisdictionContext();
        String countryCode = firstNonBlank(request.getCountryCode(), normalizeCode(request.getCountryName()));
        if (countryCode == null || "INT".equalsIgnoreCase(countryCode)) {
            return defaultContext();
        }
        ctx.setCountryCode(countryCode);
        ctx.setCountryName(firstNonBlank(request.getCountryName(), COUNTRY_NAMES.getOrDefault(ctx.getCountryCode(), ctx.getCountryCode())));
        ctx.setRegionCode(firstNonBlank(request.getRegionCode(), normalizeCode(request.getRegionName()), "GENERAL"));
        String regionName = firstNonBlank(request.getRegionName(), null);
        if (regionName != null && !isGenericRegion(regionName, ctx.getRegionCode())) {
            ctx.setRegionName(regionName);
        } else if (ctx.getRegionCode() != null && !ctx.getRegionCode().isBlank()
                && !isGenericRegion(null, ctx.getRegionCode())) {
            ctx.setRegionName(regionDisplayName(ctx.getCountryCode(), ctx.getRegionCode()));
        } else {
            ctx.setRegionCode("");
            ctx.setRegionName("");
        }
        if (request.getLocationSource() != null && !request.getLocationSource().isBlank()) {
            try {
                ctx.setLocationSource(LocationSource.valueOf(request.getLocationSource()));
            } catch (IllegalArgumentException ignored) {
                ctx.setLocationSource(LocationSource.device);
            }
        } else {
            ctx.setLocationSource(LocationSource.device);
        }
        normalizeNigeriaRegion(ctx);
        return ctx;
    }

    /** Map geocoder state names (e.g. "Kwara State") to stable region codes. */
    private void normalizeNigeriaRegion(JurisdictionContext ctx) {
        if (ctx.getCountryCode() == null || !"NG".equalsIgnoreCase(ctx.getCountryCode())) {
            return;
        }
        String combined = ((ctx.getRegionCode() != null ? ctx.getRegionCode() : "")
                + " " + (ctx.getRegionName() != null ? ctx.getRegionName() : ""))
                .toLowerCase(Locale.ROOT);
        for (Map.Entry<String, String> e : NG_ALIASES_LONGEST_FIRST) {
            if (combined.contains(e.getKey())) {
                ctx.setRegionCode(e.getValue());
                ctx.setRegionName(humanRegionName(e.getValue()));
                return;
            }
        }
        String fromPhrase = NigerianJurisdictionData.regionCodeFromStatePhrase(combined);
        if (fromPhrase != null) {
            ctx.setRegionCode(fromPhrase);
            ctx.setRegionName(humanRegionName(fromPhrase));
        }
    }

    private JurisdictionContext nigeriaRegionContext(String regionCode) {
        JurisdictionContext ctx = new JurisdictionContext();
        ctx.setCountryCode("NG");
        ctx.setCountryName("Nigeria");
        ctx.setRegionCode(regionCode);
        ctx.setRegionName(humanRegionName(regionCode));
        return ctx;
    }

    private String normalizeRegionCode(String regionCode) {
        if (regionCode == null || regionCode.isBlank()) {
            return "";
        }
        String code = regionCode.toUpperCase(Locale.ROOT).trim();
        if (code.endsWith("_STATE")) {
            code = code.substring(0, code.length() - 6);
        }
        return code;
    }

    private boolean isGenericRegion(String regionName, String regionCode) {
        if (regionCode != null && "GENERAL".equalsIgnoreCase(regionCode.trim())) {
            return true;
        }
        return regionName != null && "General".equalsIgnoreCase(regionName.trim());
    }

    private String regionDisplayName(String countryCode, String regionCode) {
        if (regionCode == null || regionCode.isBlank() || "GENERAL".equalsIgnoreCase(regionCode)) {
            return "";
        }
        if ("US".equalsIgnoreCase(countryCode)) {
            return UsJurisdictionData.displayName(regionCode);
        }
        return humanRegionName(regionCode);
    }

    private String humanRegionName(String regionCode) {
        if (regionCode == null || regionCode.isBlank() || "GENERAL".equalsIgnoreCase(regionCode)) {
            return "";
        }
        if ("FCT".equalsIgnoreCase(regionCode)) {
            return "Federal Capital Territory";
        }
        if ("FEDERAL".equalsIgnoreCase(regionCode)) {
            return "Federal";
        }
        String lower = regionCode.toLowerCase(Locale.ROOT).replace('_', ' ');
        String name = titleCase(lower);
        if (!name.toLowerCase(Locale.ROOT).contains("state") && !name.contains("Territory")) {
            return name + " State";
        }
        return name;
    }

    private boolean hasDeviceFields(ConsultRequest request) {
        if (request.getLocationSource() != null
                && "default_fallback".equalsIgnoreCase(request.getLocationSource().trim())) {
            return false;
        }
        String code = firstNonBlank(request.getCountryCode());
        if (code == null || "INT".equalsIgnoreCase(code)) {
            return false;
        }
        return firstNonBlank(request.getCountryName()) != null || code != null;
    }

    /** True when country and region are known enough for legal research. */
    public boolean isResolved(JurisdictionContext ctx) {
        if (ctx == null || ctx.getLocationSource() == LocationSource.default_fallback) {
            return false;
        }
        String code = ctx.getCountryCode();
        return code != null && !code.isBlank() && !"INT".equalsIgnoreCase(code.trim());
    }

    /** Throws if jurisdiction is not resolved. */
    public void requireResolved(JurisdictionContext ctx) {
        if (!isResolved(ctx)) {
            throw new IllegalArgumentException(
                    "Could not determine your jurisdiction. Allow device location in your browser, "
                            + "or mention your country or state in your description, voice note, or uploads.");
        }
    }

    private JurisdictionContext defaultContext() {
        return new JurisdictionContext(
                "", "", "", "",
                LocationSource.default_fallback, false);
    }

    private void enrichNames(JurisdictionContext ctx) {
        if (ctx.getCountryName() == null || ctx.getCountryName().isBlank()) {
            String code = ctx.getCountryCode() != null ? ctx.getCountryCode().toUpperCase(Locale.ROOT) : "";
            ctx.setCountryName(COUNTRY_NAMES.getOrDefault(code, code.isBlank() ? "" : code));
        }
        if (ctx.getRegionName() == null || ctx.getRegionName().isBlank()) {
            ctx.setRegionName(regionDisplayName(ctx.getCountryCode(), ctx.getRegionCode()));
        }
        if (ctx.getCountryCode() != null) {
            ctx.setCountryCode(ctx.getCountryCode().toUpperCase(Locale.ROOT));
        }
        if (ctx.getRegionCode() != null) {
            ctx.setRegionCode(ctx.getRegionCode().toUpperCase(Locale.ROOT));
        }
    }

    private String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
    }

    private String normalizeCode(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String n = name.trim().toLowerCase(Locale.ROOT);
        for (Map.Entry<String, String> e : COUNTRY_NAMES.entrySet()) {
            if (e.getValue().equalsIgnoreCase(name.trim())) {
                return e.getKey();
            }
        }
        if (n.length() <= 3) {
            return n.toUpperCase(Locale.ROOT);
        }
        Optional<JurisdictionContext> us = UsJurisdictionData.extractFromText(n);
        if (us.isPresent()) {
            return us.get().getRegionCode();
        }
        for (Map.Entry<String, String> e : NG_ALIASES_LONGEST_FIRST) {
            if (n.contains(e.getKey())) {
                return e.getValue();
            }
        }
        String fromPhrase = NigerianJurisdictionData.regionCodeFromNameToken(n);
        if (fromPhrase != null) {
            return fromPhrase;
        }
        return n.toUpperCase(Locale.ROOT).replace(' ', '_');
    }

    /** Avoid matching substrings inside unrelated words (e.g. "police" contains "ice"). */
    private boolean containsNgStateMention(String lowerText, String alias) {
        if (alias.contains(" ")) {
            return lowerText.contains(alias);
        }
        if ("fct".equals(alias) || "abuja".equals(alias) || alias.contains("federal capital")) {
            return lowerText.contains("abuja") || lowerText.contains("fct")
                    || lowerText.contains("federal capital");
        }
        return Pattern.compile("\\b" + Pattern.quote(alias) + "\\b", Pattern.CASE_INSENSITIVE)
                .matcher(lowerText)
                .find();
    }

    private String titleCase(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private record CountryMention(Pattern pattern, String code, String name) {}
}
