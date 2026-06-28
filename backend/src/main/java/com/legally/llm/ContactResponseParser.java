package com.legally.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legally.model.ContactCard;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Parses and validates contact JSON from LLM text output.
 */
public final class ContactResponseParser {

    private static final int MAX_CONTACTS = 10;
    private static final Pattern LOOKS_LIKE_URL =
            Pattern.compile("^(https?://|www\\.)[^\\s]+$", Pattern.CASE_INSENSITIVE);

    private ContactResponseParser() {
    }

    /** parse contacts. */
    public static List<ContactCard> parseContacts(ObjectMapper objectMapper, String rawText) throws Exception {
        String text = rawText.replace("```json", "").replace("```", "").trim();
        int start = text.indexOf('{');
        if (start < 0) {
            throw new IllegalArgumentException("Response contained no JSON object");
        }
        int end = text.lastIndexOf('}');
        String candidate = end > start ? text.substring(start, end + 1) : text.substring(start);
        JsonNode root = parseJsonWithRecovery(objectMapper, candidate);
        JsonNode contactsNode = root.path("contacts");
        if (!contactsNode.isArray()) {
            throw new IllegalArgumentException("Missing contacts array");
        }

        LinkedHashMap<String, ContactCard> byKey = new LinkedHashMap<>();
        for (JsonNode node : contactsNode) {
            Optional<ContactCard> card = toContactCard(node);
            card.ifPresent(c -> byKey.putIfAbsent(dedupeKey(c), c));
            if (byKey.size() >= MAX_CONTACTS) {
                break;
            }
        }
        return new ArrayList<>(byKey.values());
    }

    private static JsonNode parseJsonWithRecovery(ObjectMapper objectMapper, String candidate) throws Exception {
        try {
            return objectMapper.readTree(candidate);
        } catch (Exception first) {
            String repaired = repairLikelyTruncatedJson(candidate);
            try {
                return objectMapper.readTree(repaired);
            } catch (Exception second) {
                throw first;
            }
        }
    }

    /**
     * Recover common provider truncation patterns by balancing braces/brackets and removing dangling commas.
     */
    private static String repairLikelyTruncatedJson(String text) {
        StringBuilder out = new StringBuilder(text.length() + 16);
        Deque<Character> stack = new ArrayDeque<>();
        boolean inString = false;
        boolean escaping = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            out.append(c);
            if (inString) {
                if (escaping) {
                    escaping = false;
                    continue;
                }
                if (c == '\\') {
                    escaping = true;
                    continue;
                }
                if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
                continue;
            }
            if (c == '{') {
                stack.push('}');
            } else if (c == '[') {
                stack.push(']');
            } else if ((c == '}' || c == ']') && !stack.isEmpty() && stack.peek() == c) {
                stack.pop();
            }
        }
        String repaired = out.toString().replaceAll(",\\s*$", "");
        while (!stack.isEmpty()) {
            char closer = stack.pop();
            if (repaired.endsWith(",")) {
                repaired = repaired.substring(0, repaired.length() - 1);
            }
            repaired += closer;
        }
        repaired = repaired.replaceAll(",\\s*([}\\]])", "$1");
        return repaired;
    }

    private static Optional<ContactCard> toContactCard(JsonNode node) {
        String name = resolveName(node);
        String sourceUrl = normalizeSourceUrl(resolveSourceUrl(node));
        if (name.isBlank() || sourceUrl.isBlank() || !isAcceptableSourceUrl(sourceUrl)) {
            return Optional.empty();
        }

        List<String> phones = mergeLists(
                readStringList(node.path("phones")),
                readStringList(node.path("phone")),
                readStringList(node.path("telephone")));
        List<String> emails = mergeLists(
                readStringList(node.path("emails")),
                readStringList(node.path("email")));
        Map<String, String> social = readSocial(node.path("social"));

        ContactCard card = new ContactCard();
        card.setId(stableId(sourceUrl + "|" + name));
        card.setName(name);
        card.setRole(buildRole(node));
        card.setTags(readStringList(node.path("tags")));
        card.setPhones(phones);
        card.setEmails(emails);
        card.setSocial(social.isEmpty() ? null : social);
        card.setSourceUrl(sourceUrl);

        String notes = node.path("notes").asText("").trim();
        if (notes.isBlank()) {
            if (phones.isEmpty() && emails.isEmpty() && social.isEmpty()) {
                notes = "Official website only — open the source link to find current phone, email, or contact form.";
            } else {
                notes = "Verify phone, email, and social details on the official source before contacting.";
            }
        }
        card.setNotes(notes);
        return Optional.of(card);
    }

    private static String resolveName(JsonNode node) {
        for (String field : List.of("name", "organization", "organizationName", "org", "title")) {
            String value = node.path(field).asText("").trim();
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static String resolveSourceUrl(JsonNode node) {
        for (String field : List.of("sourceUrl", "source_url", "url", "website", "link", "homepage")) {
            String value = node.path(field).asText("").trim();
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    static String normalizeSourceUrl(String raw) {
        if (raw == null) {
            return "";
        }
        String url = raw.trim();
        if (url.isBlank()) {
            return "";
        }
        while (url.endsWith(".") || url.endsWith(",") || url.endsWith(")")) {
            url = url.substring(0, url.length() - 1).trim();
        }
        if (url.regionMatches(true, 0, "www.", 0, 4)) {
            return "https://" + url;
        }
        if (url.regionMatches(true, 0, "http://", 0, 7)) {
            return "https://" + url.substring(7);
        }
        if (!url.contains("://") && url.contains(".") && !url.contains(" ")) {
            return "https://" + url;
        }
        return url;
    }

    static boolean isAcceptableSourceUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String lower = url.toLowerCase(Locale.ROOT);
        return lower.startsWith("https://") || lower.startsWith("http://") || LOOKS_LIKE_URL.matcher(url).matches();
    }

    private static List<String> mergeLists(List<String>... lists) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        for (List<String> list : lists) {
            if (list != null) {
                merged.addAll(list);
            }
        }
        return new ArrayList<>(merged);
    }

    private static String buildRole(JsonNode node) {
        String role = node.path("role").asText("").trim();
        String type = node.path("organizationType").asText("").trim();
        if (role.isBlank() && type.isBlank()) {
            return "Public contact";
        }
        if (role.isBlank()) {
            return capitalizeType(type);
        }
        if (type.isBlank()) {
            return role;
        }
        return role + " (" + capitalizeType(type) + ")";
    }

    private static String capitalizeType(String type) {
        return switch (type.toLowerCase(Locale.ROOT)) {
            case "government" -> "Government";
            case "ngo" -> "NGO";
            case "organization" -> "Organization";
            case "legal_practitioner" -> "Legal practitioner";
            default -> type;
        };
    }

    private static List<String> readStringList(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) {
            return List.of();
        }
        if (node.isArray()) {
            LinkedHashSet<String> values = new LinkedHashSet<>();
            for (JsonNode item : node) {
                String s = item.asText("").trim();
                if (!s.isBlank()) {
                    values.add(s);
                }
            }
            return new ArrayList<>(values);
        }
        String single = node.asText("").trim();
        return single.isBlank() ? List.of() : List.of(single);
    }

    private static Map<String, String> readSocial(JsonNode node) {
        if (!node.isObject()) {
            return Map.of();
        }
        LinkedHashMap<String, String> social = new LinkedHashMap<>();
        node.fields().forEachRemaining(entry -> {
            String key = entry.getKey().trim();
            String value = entry.getValue().asText("").trim();
            if (!key.isBlank() && !value.isBlank()) {
                social.put(key, value);
            }
        });
        return social;
    }

    private static String dedupeKey(ContactCard card) {
        return card.getSourceUrl().toLowerCase(Locale.ROOT) + "|" + card.getName().toLowerCase(Locale.ROOT);
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
