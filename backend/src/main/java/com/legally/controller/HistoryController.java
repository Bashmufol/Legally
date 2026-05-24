package com.legally.controller;

import com.legally.model.dto.HistoryDetailDto;
import com.legally.model.dto.HistoryItemDto;
import com.legally.service.ConsultationHistoryService;
import com.legally.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/history")
public class HistoryController {

    private final ConsultationHistoryService consultationHistoryService;
    private final UserService userService;

    public HistoryController(ConsultationHistoryService consultationHistoryService, UserService userService) {
        this.consultationHistoryService = consultationHistoryService;
        this.userService = userService;
    }

    @GetMapping("/consultations")
    public ResponseEntity<List<HistoryItemDto>> consultations() {
        userService.syncCurrentUser();
        return ResponseEntity.ok(consultationHistoryService.listForCurrentUser());
    }

    @GetMapping("/consultations/{id}")
    public ResponseEntity<HistoryDetailDto> consultationDetail(@PathVariable UUID id) {
        userService.syncCurrentUser();
        return ResponseEntity.ok(consultationHistoryService.getDetailForCurrentUser(id));
    }
}
