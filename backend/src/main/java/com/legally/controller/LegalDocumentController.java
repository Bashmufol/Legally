package com.legally.controller;

import com.legally.model.dto.LegalDocumentRequest;
import com.legally.model.dto.LegalDocumentResponse;
import com.legally.service.LegalDocumentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI-generated legal document drafts (agreements, letters, and similar).
 */
@RestController
@RequestMapping("/api/documents")
public class LegalDocumentController {

    private final LegalDocumentService legalDocumentService;

    public LegalDocumentController(LegalDocumentService legalDocumentService) {
        this.legalDocumentService = legalDocumentService;
    }

    /** Generates document text for the requested type and jurisdiction. */
    @PostMapping("/generate")
    public ResponseEntity<LegalDocumentResponse> generate(@Valid @RequestBody LegalDocumentRequest request)
            throws Exception {
        return ResponseEntity.ok(legalDocumentService.generate(request));
    }
}
