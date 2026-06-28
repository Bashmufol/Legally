package com.legally.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.legally.entity.ConsultationRecord;
import com.legally.model.dto.ConsultRequest;
import com.legally.model.dto.ConsultResponse;
import com.legally.model.dto.HistoryDetailDto;
import com.legally.model.dto.HistoryItemDto;
import com.legally.repository.ConsultationRecordRepository;
import com.legally.security.AuthContext;
import com.legally.security.SessionContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Persists and lists consultation history for the current user and session.
 */
@Service
public class ConsultationHistoryService {

    /** Maximum consultations kept per session in the database. */
    public static final int MAX_RECORDS_PER_SESSION = 7;

    private final ConsultationRecordRepository consultationRecordRepository;
    private final UserQuestionService userQuestionService;
    private final ObjectMapper objectMapper;
    private final SessionService sessionService;

    public ConsultationHistoryService(
            ConsultationRecordRepository consultationRecordRepository,
            UserQuestionService userQuestionService,
            ObjectMapper objectMapper,
            SessionService sessionService) {
        this.consultationRecordRepository = consultationRecordRepository;
        this.userQuestionService = userQuestionService;
        this.objectMapper = objectMapper;
        this.sessionService = sessionService;
    }

    /** Saves one consult and trims older rows beyond {@link #MAX_RECORDS_PER_SESSION}. */
    @Transactional
    public void save(ConsultRequest request, ConsultResponse response, String responseJson) throws Exception {
        String uid = AuthContext.currentUserId();
        UUID sessionId = sessionService.touchCurrentSession();
        String question = userQuestionService.resolveDisplayQuestion(request);

        consultationRecordRepository.save(ConsultationRecord.create(
                uid,
                sessionId,
                request.getScenario() != null ? request.getScenario() : "general",
                question,
                response.getSummary(),
                response.getConfidence(),
                responseJson));

        trimToMaxRecords(uid, sessionId);
    }

    /** Returns up to seven recent consultations for the current session. */
    @Transactional(readOnly = true)
    public List<HistoryItemDto> listForCurrentUser() {
        String uid = AuthContext.currentUserId();
        return SessionContext.current()
                .map(sessionId -> consultationRecordRepository
                        .findTop7ByFirebaseUidAndSessionIdOrderByCreatedAtDesc(uid, sessionId)
                        .stream()
                        .map(this::toListDto)
                        .toList())
                .orElse(List.of());
    }

    /** Loads the full stored response for one consultation id. */
    @Transactional(readOnly = true)
    public HistoryDetailDto getDetailForCurrentUser(UUID id) {
        String uid = AuthContext.currentUserId();
        UUID sessionId = SessionContext.require();
        ConsultationRecord record = consultationRecordRepository
                .findByIdAndFirebaseUidAndSessionId(id, uid, sessionId)
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

    private void trimToMaxRecords(String firebaseUid, UUID sessionId) {
        List<ConsultationRecord> records =
                consultationRecordRepository.findByFirebaseUidAndSessionIdOrderByCreatedAtDesc(
                        firebaseUid, sessionId);
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
