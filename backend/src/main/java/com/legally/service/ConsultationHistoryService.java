package com.legally.service;

import com.legally.entity.ConsultationRecord;
import com.legally.model.dto.ConsultRequest;
import com.legally.model.dto.ConsultResponse;
import com.legally.model.dto.HistoryItemDto;
import com.legally.repository.ConsultationRecordRepository;
import com.legally.security.AuthContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ConsultationHistoryService {

    private final ConsultationRecordRepository consultationRecordRepository;

    public ConsultationHistoryService(ConsultationRecordRepository consultationRecordRepository) {
        this.consultationRecordRepository = consultationRecordRepository;
    }

    @Transactional
    public void save(ConsultRequest request, ConsultResponse response, String responseJson) {
        String uid = AuthContext.currentUserId();
        if (AuthContext.GUEST_UID.equals(uid)) {
            return;
        }
        consultationRecordRepository.save(ConsultationRecord.create(
                uid,
                request.getScenario() != null ? request.getScenario() : "general",
                request.getMessage(),
                response.getSummary(),
                response.getConfidence(),
                responseJson));
    }

    @Transactional(readOnly = true)
    public List<HistoryItemDto> listForCurrentUser() {
        String uid = AuthContext.currentUserId();
        if (AuthContext.GUEST_UID.equals(uid)) {
            return List.of();
        }
        return consultationRecordRepository.findTop20ByFirebaseUidOrderByCreatedAtDesc(uid).stream()
                .map(this::toDto)
                .toList();
    }

    private HistoryItemDto toDto(ConsultationRecord r) {
        HistoryItemDto dto = new HistoryItemDto();
        dto.setId(r.getId().toString());
        dto.setScenario(r.getScenario());
        dto.setMessage(r.getUserMessage());
        dto.setSummary(r.getSummary() != null ? r.getSummary() : "");
        dto.setConfidence(r.getConfidence() != null ? r.getConfidence() : "medium");
        dto.setCreatedAt(r.getCreatedAt().toString());
        return dto;
    }
}
