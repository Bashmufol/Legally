package com.legally.service;

import com.legally.firebase.ContactStore;
import com.legally.model.ContactCard;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ContactService {

    private final ContactStore contactStore;

    public ContactService(ContactStore contactStore) {
        this.contactStore = contactStore;
    }

    public List<ContactCard> byTags(Collection<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        Set<String> wanted = tags.stream()
                .map(t -> t.toLowerCase(Locale.ROOT).trim())
                .collect(Collectors.toSet());

        LinkedHashMap<String, ContactCard> result = new LinkedHashMap<>();
        for (ContactCard card : contactStore.findAll()) {
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
        return contactStore.findAll();
    }
}
