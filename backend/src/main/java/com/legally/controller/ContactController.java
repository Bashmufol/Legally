package com.legally.controller;

import com.legally.model.ContactCard;
import com.legally.service.ContactService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/contacts")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @GetMapping
    public ResponseEntity<List<ContactCard>> list(@RequestParam(required = false) String tags) {
        if (tags == null || tags.isBlank()) {
            return ResponseEntity.ok(contactService.all());
        }
        List<String> tagList = Arrays.stream(tags.split(",")).map(String::trim).toList();
        return ResponseEntity.ok(contactService.byTags(tagList));
    }
}
