package com.legally.service;

import com.legally.model.ContactCard;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts contact details visible in search snippets (no invented numbers).
 */
final class ContactSnippetParser {

    private static final Pattern PHONE = Pattern.compile(
            "(?:\\+\\d{1,3}[\\s.-]?)?(?:\\(?\\d{2,4}\\)?[\\s.-]?)?\\d{3,4}[\\s.-]?\\d{3,4}(?:[\\s.-]?\\d{2,4})?");
    private static final Pattern EMAIL = Pattern.compile(
            "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    private static final Pattern SOCIAL_URL = Pattern.compile(
            "https?://(?:www\\.)?(?:twitter\\.com|x\\.com|facebook\\.com|instagram\\.com|linkedin\\.com|youtube\\.com)/[\\w./%-]+",
            Pattern.CASE_INSENSITIVE);

    private ContactSnippetParser() {
    }

    static Optional<ContactCard> fromSearchHit(String title, String url, String snippet) {
        String combined = (title != null ? title : "") + " " + (snippet != null ? snippet : "") + " " + url;
        List<String> phones = extractPhones(combined);
        List<String> emails = extractEmails(combined);
        Map<String, String> social = extractSocial(combined, url);

        if (phones.isEmpty() && emails.isEmpty() && social.isEmpty()) {
            return Optional.empty();
        }

        ContactCard card = new ContactCard();
        card.setId(stableId(url != null ? url : title));
        card.setName(title != null && !title.isBlank() ? title.trim() : hostOf(url));
        card.setRole(snippet != null && snippet.length() > 20
                ? snippet.trim().substring(0, Math.min(120, snippet.length())) + "..."
                : "Public contact (verify on official site)");
        card.setPhones(phones);
        card.setEmails(emails);
        card.setSocial(social);
        card.setSourceUrl(url);
        card.setNotes("Verify details on the official source before calling or emailing.");
        return Optional.of(card);
    }

    private static List<String> extractPhones(String text) {
        LinkedHashSet<String> found = new LinkedHashSet<>();
        Matcher m = PHONE.matcher(text);
        while (m.find()) {
            String digits = m.group().replaceAll("\\D", "");
            if (digits.length() >= 7 && digits.length() <= 15) {
                found.add(m.group().trim());
            }
            if (found.size() >= 3) {
                break;
            }
        }
        return new ArrayList<>(found);
    }

    private static List<String> extractEmails(String text) {
        LinkedHashSet<String> found = new LinkedHashSet<>();
        Matcher m = EMAIL.matcher(text);
        while (m.find()) {
            found.add(m.group().toLowerCase(Locale.ROOT));
            if (found.size() >= 2) {
                break;
            }
        }
        return new ArrayList<>(found);
    }

    private static Map<String, String> extractSocial(String text, String pageUrl) {
        LinkedHashMap<String, String> social = new LinkedHashMap<>();
        Matcher m = SOCIAL_URL.matcher(text);
        while (m.find()) {
            String link = m.group();
            String platform = platformName(link);
            social.putIfAbsent(platform, link);
        }
        if (pageUrl != null && isSocialHost(pageUrl)) {
            social.putIfAbsent(platformName(pageUrl), pageUrl);
        }
        return social;
    }

    private static boolean isSocialHost(String url) {
        String host = hostOf(url);
        return host != null && (host.contains("twitter") || host.contains("facebook")
                || host.contains("instagram") || host.contains("linkedin") || host.equals("x.com"));
    }

    private static String platformName(String url) {
        String host = hostOf(url);
        if (host == null) {
            return "Website";
        }
        if (host.contains("twitter") || host.equals("x.com")) {
            return "X";
        }
        if (host.contains("facebook")) {
            return "Facebook";
        }
        if (host.contains("instagram")) {
            return "Instagram";
        }
        if (host.contains("linkedin")) {
            return "LinkedIn";
        }
        if (host.contains("youtube")) {
            return "YouTube";
        }
        return "Website";
    }

    private static String hostOf(String url) {
        try {
            return URI.create(url.trim()).getHost();
        } catch (Exception e) {
            return null;
        }
    }

    private static String stableId(String key) {
        byte[] hash = key.getBytes(StandardCharsets.UTF_8);
        StringBuilder hex = new StringBuilder();
        for (int i = 0; i < Math.min(8, hash.length); i++) {
            hex.append(String.format("%02x", hash[i]));
        }
        return "contact-" + hex;
    }
}
