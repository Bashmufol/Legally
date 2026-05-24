package com.legally.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.legally.entity.ConsultationRecord;
import com.legally.model.dto.ConsultRequest;
import com.legally.model.dto.ConsultResponse;
import com.legally.model.dto.HistoryDetailDto;
import com.legally.model.dto.HistoryItemDto;
import com.legally.repository.ConsultationRecordRepository;
import com.legally.security.AuthContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class ConsultationHistoryService {

    public static final int MAX_RECORDS_PER_SESSION = 7;

    private final ConsultationRecordRepository consultationRecordRepository;
    private final UserQuestionService userQuestionService;
    private final ObjectMapper objectMapper;

    public ConsultationHistoryService(
            ConsultationRecordRepository consultationRecordRepository,
            UserQuestionService userQuestionService,
            ObjectMapper objectMapper) {
        this.consultationRecordRepository = consultationRecordRepository;
        this.userQuestionService = userQuestionService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void save(ConsultRequest request, ConsultResponse response, String responseJson) throws Exception {
        String uid = AuthContext.currentUserId();
        if (AuthContext.GUEST_UID.equals(uid)) {
            return;
        }

        String question = userQuestionService.resolveDisplayQuestion(request);

        consultationRecordRepository.save(ConsultationRecord.create(
                uid,
                request.getScenario() != null ? request.getScenario() : "general",
                question,
                response.getSummary(),
                response.getConfidence(),
                responseJson));

        trimToMaxRecords(uid);
    }

    @Transactional(readOnly = true)
    public List<HistoryItemDto> listForCurrentUser() {
        String uid = AuthContext.currentUserId();
        if (AuthContext.GUEST_UID.equals(uid)) {
            return List.of();
        }
        return consultationRecordRepository.findTop7ByFirebaseUidOrderByCreatedAtDesc(uid).stream()
                .map(this::toListDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public HistoryDetailDto getDetailForCurrentUser(UUID id) {
        String uid = AuthContext.currentUserId();
        if (AuthContext.GUEST_UID.equals(uid)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Consultation not found");
        }
        ConsultationRecord record = consultationRecordRepository
                .findByIdAndFirebaseUid(id, uid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Consultation not found"));

        try {
            ConsultResponse response = objectMapper.readValue(record.getResponseJson(), ConsultResponse.class);
            HistoryDetailDto dto = new HistoryDetailDto();
            dto.setId(record.getId().toString());
            dto.setScenario(record.getScenario());
            dto.setQuestion(record.getUserMessage());
            dto.setCreatedAt(record.getCreatedAt().toString());
            dto.setResponse(response);
            return dto;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not load consultation details");
        }
    }

    private void trimToMaxRecords(String firebaseUid) {
        List<ConsultationRecord> records =
                consultationRecordRepository.findByFirebaseUidOrderByCreatedAtDesc(firebaseUid);
        if (records.size() <= MAX_RECORDS_PER_SESSION) {
            return;
        }
        for (ConsultationRecord extra : records.subList(MAX_RECORDS_PER_SESSION, records.size())) {
            consultationRecordRepository.delete(extra);
        }
    }

    private HistoryItemDto toListDto(ConsultationRecord r) {
        HistoryItemDto dto = new HistoryItemDto();
        dto.setId(r.getId().toString());
        dto.setScenario(r.getScenario());
        dto.setQuestion(r.getUserMessage() != null ? r.getUserMessage() : "");
        dto.setCreatedAt(r.getCreatedAt().toString());
        return dto;
    }
}
