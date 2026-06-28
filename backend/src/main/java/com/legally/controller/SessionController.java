package com.legally.controller;

import com.legally.service.SessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Session lifecycle (start is client-driven; end wipes session data).
 */
@RestController
@RequestMapping("/api/session")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    /** Deletes uploads, consultation history, and demand letters for the current session. */
    @PostMapping("/end")
    public ResponseEntity<Map<String, String>> endSession() {
        sessionService.endCurrentSession();
        return ResponseEntity.ok(Map.of("status", "ended"));
    }
}
