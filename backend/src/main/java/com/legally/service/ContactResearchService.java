package com.legally.service;

import com.legally.model.ContactCard;
import com.legally.model.JurisdictionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class ContactResearchService {

    private static final Logger log = LoggerFactory.getLogger(ContactResearchService.class);

    private static final List<String> INTERNATIONAL_QUERIES = List.of(
            "UN OHCHR human rights legal assistance contact phone email",
            "Amnesty International helpline contact",
            "UNICEF legal protection contact",
            "International Committee of the Red Cross legal assistance contact"
    );

    private final SerpApiWebSearchService serpApiWebSearchService;

    public ContactResearchService(SerpApiWebSearchService serpApiWebSearchService) {
        this.serpApiWebSearchService = serpApiWebSearchService;
    }

    public List<ContactCard> findContacts(
            JurisdictionContext jurisdiction, String scenario, String userMessage) {
        if (!serpApiWebSearchService.isConfigured()) {
            return List.of();
        }

        List<ContactCard> cards = new ArrayList<>();
        LinkedHashMap<String, ContactCard> dedupe = new LinkedHashMap<>();

        boolean international = isInternationalContext(jurisdiction, userMessage);
        String country = jurisdiction.getCountryName() != null
                ? jurisdiction.getCountryName()
                : jurisdiction.getCountryCode();

        if (!international || !"INT".equalsIgnoreCase(jurisdiction.getCountryCode())) {
            addFromQueries(dedupe, buildLocalQueries(country, scenario));
        }

        if (international) {
            addFromQueries(dedupe, INTERNATIONAL_QUERIES);
        }

        cards.addAll(dedupe.values());
        if (cards.size() > 8) {
            return cards.subList(0, 8);
        }
        log.info("Found {} web contacts for {}", cards.size(), jurisdiction.displayLabel());
        return cards;
    }

    private void addFromQueries(LinkedHashMap<String, ContactCard> dedupe, List<String> queries) {
        for (String query : queries) {
            for (SerpApiWebSearchService.SearchHit hit : serpApiWebSearchService.searchOpen(query)) {
                Optional<ContactCard> card = ContactSnippetParser.fromSearchHit(
                        hit.title(), hit.url(), hit.snippet());
                card.ifPresent(c -> dedupe.putIfAbsent(c.getId(), c));
            }
        }
    }

    private List<String> buildLocalQueries(String country, String scenario) {
        String topic = scenario != null && !scenario.isBlank() && !"general".equals(scenario)
                ? scenario.replace('_', ' ')
                : "legal";
        return List.of(
                country + " legal aid contact phone email official",
                country + " " + topic + " lawyer bar association contact phone",
                country + " police public relations contact phone email",
                country + " ministry of justice contact phone email");
    }

    private boolean isInternationalContext(JurisdictionContext jurisdiction, String userMessage) {
        if ("INT".equalsIgnoreCase(jurisdiction.getCountryCode())) {
            return true;
        }
        if (userMessage == null) {
            return false;
        }
        String lower = userMessage.toLowerCase(Locale.ROOT);
        return lower.contains("international") || lower.contains("refugee") || lower.contains("asylum")
                || lower.contains("united nations") || lower.contains("cross-border");
    }
}
