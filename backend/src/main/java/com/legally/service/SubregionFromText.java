package com.legally.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts city or subregion names from phrases like "in Accra, Ghana" or "Lagos, Nigeria".
 */
final class SubregionFromText {

    private static final Set<String> STOP_PHRASES = Set.of(
            "the", "a", "an", "my", "our", "this", "that", "plot", "land", "property", "house",
            "buy", "sell", "want", "need", "live", "living", "work", "area", "region");

    private SubregionFromText() {
    }

    static Optional<ExtractedSubregion> extract(String text, String countryCode, String countryName) {
        if (text == null || text.isBlank() || countryCode == null || countryCode.isBlank()) {
            return Optional.empty();
        }
        List<String> countryTokens = countryTokens(countryCode, countryName);
        String haystack = text.toLowerCase(Locale.ROOT);

        for (String token : countryTokens) {
            if (token.isBlank()) {
                continue;
            }
            Optional<String> fromIn = matchInCityCountry(haystack, token);
            if (fromIn.isPresent()) {
                return Optional.of(toSubregion(fromIn.get()));
            }
            Optional<String> fromComma = matchCityCommaCountry(haystack, token);
            if (fromComma.isPresent()) {
                return Optional.of(toSubregion(fromComma.get()));
            }
        }
        return Optional.empty();
    }

    private static List<String> countryTokens(String countryCode, String countryName) {
        Set<String> tokens = new LinkedHashSet<>();
        if (countryName != null && !countryName.isBlank()) {
            tokens.add(countryName.toLowerCase(Locale.ROOT).trim());
        }
        switch (countryCode.toUpperCase(Locale.ROOT)) {
            case "NG" -> tokens.add("nigeria");
            case "GH" -> tokens.add("ghana");
            case "KE" -> tokens.add("kenya");
            case "ZA" -> tokens.add("south africa");
            case "GB" -> tokens.add("united kingdom");
            case "UK" -> tokens.add("united kingdom");
            case "US" -> {
                tokens.add("united states");
                tokens.add("usa");
                tokens.add("america");
            }
            case "CA" -> tokens.add("canada");
            case "IN" -> tokens.add("india");
            default -> {
            }
        }
        return new ArrayList<>(tokens);
    }

    private static Optional<String> matchInCityCountry(String lowerText, String countryToken) {
        Pattern pattern = Pattern.compile(
                "(?:\\bin\\b|\\bat\\b|\\bnear\\b|\\bfrom\\b)\\s+([^,]{2,45}?)\\s*,\\s*"
                        + Pattern.quote(countryToken)
                        + "\\b",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        Matcher matcher = pattern.matcher(lowerText);
        while (matcher.find()) {
            String candidate = sanitizeCandidate(matcher.group(1));
            if (isValidCandidate(candidate)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    /** Single- or multi-word place immediately before ", country". */
    private static Optional<String> matchCityCommaCountry(String lowerText, String countryToken) {
        Pattern pattern = Pattern.compile(
                "\\b([\\p{L}][\\p{L}\\p{M}'\\- ]{0,38}[\\p{L}\\p{M}])\\s*,\\s*"
                        + Pattern.quote(countryToken)
                        + "\\b",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        Matcher matcher = pattern.matcher(lowerText);
        String best = null;
        while (matcher.find()) {
            String candidate = sanitizeCandidate(matcher.group(1));
            if (!isValidCandidate(candidate)) {
                continue;
            }
            if (best == null || candidate.length() < best.length()) {
                best = candidate;
            }
        }
        return Optional.ofNullable(best);
    }

    private static String sanitizeCandidate(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim().replaceAll("\\s+", " ");
        s = s.replaceAll("^(?:a|an|the)\\s+", "");
        return s.trim();
    }

    private static boolean isValidCandidate(String candidate) {
        if (candidate == null || candidate.length() < 2 || candidate.length() > 40) {
            return false;
        }
        String lower = candidate.toLowerCase(Locale.ROOT);
        if (STOP_PHRASES.contains(lower)) {
            return false;
        }
        for (String word : lower.split("\\s+")) {
            if (STOP_PHRASES.contains(word)) {
                return false;
            }
        }
        return lower.chars().anyMatch(Character::isLetter);
    }

    private static ExtractedSubregion toSubregion(String name) {
        String display = titleCaseWords(name);
        String code = display.toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        return new ExtractedSubregion(code, display);
    }

    private static String titleCaseWords(String s) {
        String[] parts = s.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            String part = parts[i];
            if (part.isEmpty()) {
                continue;
            }
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                sb.append(part.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return sb.toString();
    }

    /**
     * extracted subregion.
     */
    record ExtractedSubregion(String code, String displayName) {}
}
