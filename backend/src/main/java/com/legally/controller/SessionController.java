package com.legally.controller;

import com.legally.service.SessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/session")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

  /**
   * Ends the current session: deletes uploads, consultation history, and demand letters for this session.
   */
    @PostMapping("/end")
    public ResponseEntity<Map<String, String>> endSession() {
        sessionService.endCurrentSession();
        return ResponseEntity.ok(Map.of("status", "ended"));
    }
}
