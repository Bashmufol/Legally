package com.legally.service;

import com.legally.model.JurisdictionContext;
import com.legally.model.JurisdictionContext.LocationSource;
import com.legally.model.dto.ConsultRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class JurisdictionService {

    private static final String DEFAULT_DISCLAIMER_SUFFIX =
            " Local law corpus may be limited for your jurisdiction; verify with a licensed lawyer in your country.";

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

    private static final Map<String, String> NG_STATE_ALIASES = Map.ofEntries(
            Map.entry("kwara", "KWARA"),
            Map.entry("ilorin", "KWARA"),
            Map.entry("lagos", "LAGOS"),
            Map.entry("abuja", "FCT"),
            Map.entry("fct", "FCT"),
            Map.entry("kano", "KANO"),
            Map.entry("rivers", "RIVERS")
    );

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

    /**
     * Resolution order: explicit country/state in user text &gt; device location from request &gt; default.
     * Further overrides (any country) are detected in {@link ConsultService} via Gemini on text and media.
     */
    public JurisdictionContext resolve(ConsultRequest request) {
        String message = request.getMessage() != null ? request.getMessage() : "";

        Optional<JurisdictionContext> fromMessage = extractFromText(message);
        if (fromMessage.isPresent()) {
            JurisdictionContext ctx = fromMessage.get();
            ctx.setLocationSource(LocationSource.input_override);
            enrichNames(ctx);
            ctx.setCorpusLimited(!"NG".equalsIgnoreCase(ctx.getCountryCode()));
            return ctx;
        }

        if (hasDeviceFields(request)) {
            JurisdictionContext ctx = fromRequestFields(request);
            ctx.setLocationSource(LocationSource.device);
            enrichNames(ctx);
            ctx.setCorpusLimited(!"NG".equalsIgnoreCase(ctx.getCountryCode()));
            return ctx;
        }

        return defaultContext();
    }

    public JurisdictionContext applyDetectedOverride(JurisdictionContext detected) {
        detected.setLocationSource(LocationSource.input_override);
        enrichNames(detected);
        detected.setCorpusLimited(!"NG".equalsIgnoreCase(detected.getCountryCode()));
        return detected;
    }

    public String disclaimerFor(JurisdictionContext ctx) {
        String base = "Legally provides general legal information only, not legal advice. "
                + "Consult a licensed lawyer in " + ctx.getCountryName() + " for your specific case.";
        if (ctx.isCorpusLimited()) {
            return base + DEFAULT_DISCLAIMER_SUFFIX;
        }
        return base;
    }

    private Optional<JurisdictionContext> extractFromText(String text) {
        if (text.isBlank()) {
            return Optional.empty();
        }
        String lower = text.toLowerCase(Locale.ROOT);

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
        if ("NG".equals(countryCode)) {
            for (Map.Entry<String, String> e : NG_STATE_ALIASES.entrySet()) {
                if (lower.contains(e.getKey())) {
                    regionCode = e.getValue();
                    regionName = titleCase(e.getKey());
                    break;
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

        JurisdictionContext ctx = new JurisdictionContext();
        ctx.setCountryCode(countryCode);
        ctx.setCountryName(countryName);
        if (regionCode != null) {
            ctx.setRegionCode(regionCode);
            ctx.setRegionName(regionName);
        } else if ("NG".equals(countryCode)) {
            ctx.setRegionCode("FEDERAL");
            ctx.setRegionName("Federal");
        } else {
            ctx.setRegionCode("GENERAL");
            ctx.setRegionName("General");
        }
        return Optional.of(ctx);
    }

    private JurisdictionContext fromRequestFields(ConsultRequest request) {
        JurisdictionContext ctx = new JurisdictionContext();
        ctx.setCountryCode(firstNonBlank(request.getCountryCode(), normalizeCode(request.getCountryName()), "INT"));
        ctx.setCountryName(firstNonBlank(request.getCountryName(), COUNTRY_NAMES.getOrDefault(ctx.getCountryCode(), ctx.getCountryCode())));
        ctx.setRegionCode(firstNonBlank(request.getRegionCode(), normalizeCode(request.getRegionName()), "GENERAL"));
        ctx.setRegionName(firstNonBlank(request.getRegionName(), ctx.getRegionCode()));
        return ctx;
    }

    private boolean hasDeviceFields(ConsultRequest request) {
        return firstNonBlank(request.getCountryCode(), request.getCountryName()) != null;
    }

    private JurisdictionContext defaultContext() {
        JurisdictionContext ctx = new JurisdictionContext(
                "INT", "International", "GENERAL", "General",
                LocationSource.default_fallback, true);
        return ctx;
    }

    private void enrichNames(JurisdictionContext ctx) {
        if (ctx.getCountryName() == null || ctx.getCountryName().isBlank()) {
            ctx.setCountryName(COUNTRY_NAMES.getOrDefault(
                    ctx.getCountryCode() != null ? ctx.getCountryCode().toUpperCase(Locale.ROOT) : "INT",
                    ctx.getCountryCode()));
        }
        if (ctx.getRegionName() == null || ctx.getRegionName().isBlank()) {
            ctx.setRegionName(ctx.getRegionCode());
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
        for (Map.Entry<String, String> e : NG_STATE_ALIASES.entrySet()) {
            if (n.contains(e.getKey())) {
                return e.getValue();
            }
        }
        return n.toUpperCase(Locale.ROOT).replace(' ', '_');
    }

    private String titleCase(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private record CountryMention(Pattern pattern, String code, String name) {}
}
