package com.legally.service.official;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Allowlist of host suffixes for official / authoritative legal sources per country.
 */
@Component
public class OfficialDomainRegistry {

    private static final List<String> GLOBAL = List.of(
            "ohchr.org",
            "un.org",
            "icc-cpi.int",
            "wto.org"
    );

    private static final Map<String, List<String>> BY_COUNTRY = Map.ofEntries(
            Map.entry("US", List.of(
                    ".gov",
                    "supremecourt.gov",
                    "uscourts.gov",
                    "congress.gov",
                    "house.gov",
                    "senate.gov",
                    "justice.gov",
                    "law.cornell.edu",
                    "loc.gov"
            )),
            Map.entry("GB", List.of(
                    "gov.uk",
                    "legislation.gov.uk",
                    "parliament.uk",
                    "judiciary.uk"
            )),
            Map.entry("CA", List.of(
                    ".gc.ca",
                    "canlii.org",
                    "scc-csc.ca"
            )),
            Map.entry("IN", List.of(
                    "gov.in",
                    "indiankanoon.org",
                    "legislative.gov.in"
            )),
            Map.entry("KE", List.of(
                    "go.ke",
                    "kenyalaw.org"
            )),
            Map.entry("GH", List.of(
                    "gov.gh",
                    "ghalii.org"
            )),
            Map.entry("ZA", List.of(
                    "gov.za",
                    "gov.za",
                    "justice.gov.za",
                    "concourt.org.za"
            )),
            Map.entry("NG", List.of(
                    "nigeria-law.org",
                    "gov.ng",
                    "npf.gov.ng",
                    "nhrc.gov.ng"
            )),
            Map.entry("INT", GLOBAL)
    );

    public List<String> domainsFor(String countryCode) {
        String code = countryCode != null ? countryCode.toUpperCase(Locale.ROOT) : "INT";
        List<String> merged = new ArrayList<>(GLOBAL);
        List<String> specific = BY_COUNTRY.getOrDefault(code, List.of());
        merged.addAll(specific);
        if ("INT".equals(code)) {
            return GLOBAL;
        }
        return merged;
    }

    public boolean isOfficialSource(String url, String countryCode) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String host = hostOf(url);
        if (host == null || host.isBlank()) {
            return false;
        }
        String lowerHost = host.toLowerCase(Locale.ROOT);
        for (String pattern : domainsFor(countryCode)) {
            if (matchesHost(lowerHost, pattern)) {
                return true;
            }
        }
        return false;
    }

    public String siteRestrictionClause(String countryCode, int maxSites) {
        List<String> domains = domainsFor(countryCode);
        List<String> picked = domains.stream()
                .filter(d -> !d.startsWith("."))
                .limit(maxSites)
                .toList();
        if (picked.isEmpty()) {
            return "";
        }
        return picked.stream()
                .map(d -> "site:" + d)
                .reduce((a, b) -> a + " OR " + b)
                .orElse("");
    }

    private boolean matchesHost(String host, String pattern) {
        String p = pattern.toLowerCase(Locale.ROOT);
        if (p.startsWith(".")) {
            return host.endsWith(p) || host.equals(p.substring(1));
        }
        return host.equals(p) || host.endsWith("." + p);
    }

    private String hostOf(String url) {
        try {
            URI uri = URI.create(url.trim());
            String host = uri.getHost();
            if (host != null) {
                return host;
            }
            if (url.contains("://")) {
                return URI.create(url).getHost();
            }
        } catch (Exception ignored) {
            // fall through
        }
        return null;
    }
}
