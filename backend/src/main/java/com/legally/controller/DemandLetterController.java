package com.legally.controller;

import com.legally.model.dto.DemandLetterRequest;
import com.legally.model.dto.DemandLetterResponse;
import com.legally.service.DemandLetterService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Demand letter generation endpoint.
 */
@RestController
@RequestMapping("/api/demand-letter")
public class DemandLetterController {

    private final DemandLetterService demandLetterService;

    public DemandLetterController(DemandLetterService demandLetterService) {
        this.demandLetterService = demandLetterService;
    }

    /** Generates a demand letter from the supplied facts and scenario. */
    @PostMapping
    public ResponseEntity<DemandLetterResponse> generate(@Valid @RequestBody DemandLetterRequest request)
            throws Exception {
        return ResponseEntity.ok(demandLetterService.generate(request));
    }
}
