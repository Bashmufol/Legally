package com.legally.service;

import com.legally.config.LegallyProperties;
import com.legally.model.JurisdictionContext;
import com.legally.model.LawChunk;
import com.legally.model.WebLegalSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Service
public class WebResearchService {

    private static final Logger log = LoggerFactory.getLogger(WebResearchService.class);

    private final LegallyProperties properties;
    private final SerpApiWebSearchService serpApiService;
    private final WebPageContentFetcher pageFetcher;

    public WebResearchService(
            LegallyProperties properties,
            SerpApiWebSearchService serpApiService,
            WebPageContentFetcher pageFetcher) {
        this.properties = properties;
        this.serpApiService = serpApiService;
        this.pageFetcher = pageFetcher;
    }

    public boolean isConfigured() {
        return properties.getSerpApi().isConfigured();
    }

    public List<WebLegalSource> research(JurisdictionContext jurisdiction, String scenario, String userMessage) {
        List<SerpApiWebSearchService.SearchHit> hits =
                serpApiService.search(jurisdiction, scenario, userMessage);
        if (hits.isEmpty()) {
            log.info("No official web search hits for {} — {}", jurisdiction.getCountryCode(), userMessage);
            return List.of();
        }

        int fetchLimit = Math.max(1, properties.getSerpApi().getMaxPagesToFetch());
        List<WebLegalSource> sources = new ArrayList<>();
        int fetched = 0;

        for (SerpApiWebSearchService.SearchHit hit : hits) {
            if (sources.size() >= fetchLimit + 3) {
                break;
            }
            String excerpt = hit.snippet();
            if (fetched < fetchLimit) {
                String pageText = pageFetcher.fetchText(hit.url());
                if (pageText != null && pageText.length() > 80) {
                    excerpt = pageText;
                    fetched++;
                }
            }
            if (excerpt == null || excerpt.isBlank()) {
                excerpt = hit.snippet();
            }
            if (excerpt == null || excerpt.isBlank()) {
                continue;
            }

            WebLegalSource source = new WebLegalSource();
            source.setId(stableId(hit.url()));
            source.setUrl(hit.url());
            source.setTitle(hit.title() != null && !hit.title().isBlank() ? hit.title() : hit.url());
            source.setExcerpt(excerpt);
            source.setDomain(hostOf(hit.url()));
            sources.add(source);
        }
        return sources;
    }

    public List<LawChunk> toLawChunks(List<WebLegalSource> sources, JurisdictionContext jurisdiction) {
        String country = jurisdiction.getCountryCode() != null
                ? jurisdiction.getCountryCode().toUpperCase(Locale.ROOT)
                : "INT";
        String region = jurisdiction.getRegionCode() != null
                ? jurisdiction.getRegionCode().toUpperCase(Locale.ROOT)
                : "GENERAL";

        List<LawChunk> chunks = new ArrayList<>();
        for (WebLegalSource s : sources) {
            LawChunk c = new LawChunk();
            c.setId(s.getId());
            c.setCountryCode(country);
            c.setRegionCode(region);
            c.setJurisdiction(country);
            c.setInstrument(s.getTitle());
            c.setSection("Official web source");
            c.setTitle(s.getTitle());
            c.setText(s.getExcerpt());
            c.setSourceUrl(s.getUrl());
            chunks.add(c);
        }
        return chunks;
    }

    private String stableId(String url) {
        byte[] hash = url.getBytes(StandardCharsets.UTF_8);
        String hex = HexFormat.of().formatHex(hash, 0, Math.min(8, hash.length));
        return "web-" + hex;
    }

    private String hostOf(String url) {
        try {
            URI uri = URI.create(url);
            return uri.getHost() != null ? uri.getHost() : "";
        } catch (Exception e) {
            return "";
        }
    }
}
