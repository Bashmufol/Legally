package com.legally.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.legally.model.ContactCard;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContactResponseParserTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void acceptsNameAndSourceUrlWithoutContactMethods() throws Exception {
        String json = """
                {
                  "contacts": [
                    {
                      "name": "Kwara State Ministry of Justice",
                      "role": "State legal matters",
                      "sourceUrl": "https://kwara.gov.ng/justice"
                    }
                  ]
                }
                """;

        List<ContactCard> cards = ContactResponseParser.parseContacts(objectMapper, json);

        assertEquals(1, cards.size());
        assertEquals("Kwara State Ministry of Justice", cards.get(0).getName());
        assertTrue(cards.get(0).getPhones().isEmpty());
        assertTrue(cards.get(0).getEmails().isEmpty());
    }

    @Test
    void normalizesHttpAndBareDomains() throws Exception {
        assertEquals("https://example.gov.ng", ContactResponseParser.normalizeSourceUrl("http://example.gov.ng"));
        assertEquals("https://www.example.gov.ng", ContactResponseParser.normalizeSourceUrl("www.example.gov.ng"));
        assertEquals("https://example.gov.ng", ContactResponseParser.normalizeSourceUrl("example.gov.ng"));
    }

    @Test
    void readsAlternateFieldNames() throws Exception {
        String json = """
                {
                  "contacts": [
                    {
                      "organization": "Legal Aid Council of Nigeria",
                      "website": "legalaidcouncil.gov.ng",
                      "phone": "+234 700 123 4567"
                    }
                  ]
                }
                """;

        List<ContactCard> cards = ContactResponseParser.parseContacts(objectMapper, json);

        assertEquals(1, cards.size());
        assertEquals("Legal Aid Council of Nigeria", cards.get(0).getName());
        assertEquals("https://legalaidcouncil.gov.ng", cards.get(0).getSourceUrl());
        assertEquals(1, cards.get(0).getPhones().size());
    }
}
