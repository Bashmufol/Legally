package com.legally.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legally.config.LegallyProperties;
import com.legally.model.JurisdictionContext;
import com.legally.service.official.OfficialDomainRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Service
public class SerpApiWebSearchService {

    private static final Logger log = LoggerFactory.getLogger(SerpApiWebSearchService.class);

    private final LegallyProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final OfficialDomainRegistry domainRegistry;

    public SerpApiWebSearchService(
            LegallyProperties properties,
            RestClient restClient,
            ObjectMapper objectMapper,
            OfficialDomainRegistry domainRegistry) {
        this.properties = properties;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.domainRegistry = domainRegistry;
    }

    public record SearchHit(String title, String url, String snippet) {}

    public boolean isConfigured() {
        return properties.getSerpApi().isConfigured();
    }

    public List<SearchHit> search(JurisdictionContext jurisdiction, String scenario, String userMessage) {
        if (!isConfigured()) {
            return List.of();
        }

        String apiKey = properties.getSerpApi().getApiKey();
        int num = Math.min(Math.max(properties.getSerpApi().getMaxResults(), 1), 10);
        String query = buildQuery(jurisdiction, scenario, userMessage);

        URI uri = UriComponentsBuilder
                .fromUriString("https://serpapi.com/search.json")
                .queryParam("engine", "google")
                .queryParam("api_key", apiKey)
                .queryParam("q", query)
                .queryParam("num", num)
                .build()
                .toUri();

        try {
            String body = restClient.get().uri(uri).retrieve().body(String.class);
            if (body == null || body.isBlank()) {
                return List.of();
            }
            JsonNode organic = objectMapper.readTree(body).path("organic_results");
            List<SearchHit> hits = new ArrayList<>();
            if (!organic.isArray()) {
                return hits;
            }
            String countryCode = jurisdiction.getCountryCode();
            for (JsonNode item : organic) {
                String link = item.path("link").asText("");
                if (link.isBlank() || !domainRegistry.isOfficialSource(link, countryCode)) {
                    continue;
                }
                hits.add(new SearchHit(
                        item.path("title").asText(""),
                        link,
                        item.path("snippet").asText("")));
            }
            return hits;
        } catch (Exception e) {
            log.warn("SerpApi request failed: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Open web search for contact discovery (not restricted to official legal domains).
     */
    public List<SearchHit> searchOpen(String query) {
        if (!properties.getSerpApi().isConfigured() || query == null || query.isBlank()) {
            return List.of();
        }

        String apiKey = properties.getSerpApi().getApiKey();
        int num = Math.min(Math.max(properties.getSerpApi().getMaxResults(), 1), 10);

        URI uri = UriComponentsBuilder
                .fromUriString("https://serpapi.com/search.json")
                .queryParam("engine", "google")
                .queryParam("api_key", apiKey)
                .queryParam("q", query.trim())
                .queryParam("num", num)
                .build()
                .toUri();

        try {
            String body = restClient.get().uri(uri).retrieve().body(String.class);
            if (body == null || body.isBlank()) {
                return List.of();
            }
            JsonNode organic = objectMapper.readTree(body).path("organic_results");
            List<SearchHit> hits = new ArrayList<>();
            if (!organic.isArray()) {
                return hits;
            }
            for (JsonNode item : organic) {
                String link = item.path("link").asText("");
                if (link.isBlank()) {
                    continue;
                }
                hits.add(new SearchHit(
                        item.path("title").asText(""),
                        link,
                        item.path("snippet").asText("")));
            }
            return hits;
        } catch (Exception e) {
            log.warn("SerpApi open search failed: {}", e.getMessage());
            return List.of();
        }
    }

    private String buildQuery(JurisdictionContext jurisdiction, String scenario, String userMessage) {
        String country = jurisdiction.getCountryName() != null
                ? jurisdiction.getCountryName()
                : jurisdiction.getCountryCode();
        String region = jurisdiction.getRegionName() != null && !jurisdiction.getRegionName().isBlank()
                ? jurisdiction.getRegionName()
                : "";
        String topic = userMessage != null && userMessage.length() > 200
                ? userMessage.substring(0, 200)
                : (userMessage != null && !userMessage.isBlank() ? userMessage : "legal rights");
        String siteClause = domainRegistry.siteRestrictionClause(jurisdiction.getCountryCode(), 4);
        String scenarioBit = scenario != null && !scenario.isBlank() && !"general".equals(scenario)
                ? scenario.replace('_', ' ')
                : "";

        StringBuilder q = new StringBuilder();
        q.append(topic).append(" ").append(country);
        if (!region.isBlank() && !"General".equalsIgnoreCase(region)) {
            q.append(" ").append(region);
        }
        if (!scenarioBit.isBlank()) {
            q.append(" ").append(scenarioBit);
        }
        q.append(" law");
        if (!siteClause.isBlank()) {
            q.append(" (").append(siteClause).append(")");
        }
        return q.toString().trim();
    }
}
