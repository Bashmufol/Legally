package com.legally.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legally.model.ContactCard;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ContactService {

    private List<ContactCard> contacts = List.of();
    private final ObjectMapper objectMapper;

    public ContactService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void load() throws Exception {
        try (InputStream in = new ClassPathResource("contacts/contacts.json").getInputStream()) {
            this.contacts = objectMapper.readValue(in, new TypeReference<List<ContactCard>>() {});
        }
    }

    public List<ContactCard> byTags(Collection<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        Set<String> wanted = tags.stream()
                .map(t -> t.toLowerCase(Locale.ROOT).trim())
                .collect(Collectors.toSet());

        LinkedHashMap<String, ContactCard> result = new LinkedHashMap<>();
        for (ContactCard card : contacts) {
            if (card.getTags() == null) continue;
            for (String tag : card.getTags()) {
                if (wanted.contains(tag.toLowerCase(Locale.ROOT))) {
                    result.putIfAbsent(card.getId(), card);
                    break;
                }
            }
        }
        return new ArrayList<>(result.values());
    }

    public List<ContactCard> all() {
        return Collections.unmodifiableList(contacts);
    }
}
