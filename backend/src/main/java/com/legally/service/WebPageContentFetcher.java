package com.legally.service;

import com.legally.config.LegallyProperties;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class WebPageContentFetcher {

    private static final Logger log = LoggerFactory.getLogger(WebPageContentFetcher.class);

    private final LegallyProperties properties;

    public WebPageContentFetcher(LegallyProperties properties) {
        this.properties = properties;
    }

    public String fetchText(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        int maxChars = properties.getSerpApi().getMaxExcerptChars();
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Legally/1.0 (legal-information; +https://legally.app)")
                    .timeout((int) Duration.ofSeconds(12).toMillis())
                    .followRedirects(true)
                    .get();
            doc.select("script, style, nav, footer, header, aside, form, noscript").remove();
            String text = doc.body() != null ? doc.body().text() : doc.text();
            text = Jsoup.clean(text, Safelist.none());
            text = text.replaceAll("\\s+", " ").trim();
            if (text.length() > maxChars) {
                text = text.substring(0, maxChars) + "...";
            }
            return text;
        } catch (Exception e) {
            log.debug("Could not fetch {}: {}", url, e.getMessage());
            return "";
        }
    }
}
