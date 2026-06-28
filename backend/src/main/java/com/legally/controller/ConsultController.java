package com.legally.controller;

import com.legally.model.dto.ConsultRequest;
import com.legally.model.dto.ConsultResponse;
import com.legally.service.ConsultService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Legal consultation endpoint.
 */
@RestController
@RequestMapping("/api")
public class ConsultController {

    private final ConsultService consultService;

    public ConsultController(ConsultService consultService) {
        this.consultService = consultService;
    }

    /** Runs jurisdiction resolution, legal research, optional contacts, and saves history. */
    @PostMapping("/consult")
    public ResponseEntity<ConsultResponse> consult(@Valid @RequestBody ConsultRequest request) throws Exception {
        return ResponseEntity.ok(consultService.consult(request));
    }
}
