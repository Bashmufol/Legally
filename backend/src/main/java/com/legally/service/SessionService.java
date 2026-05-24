package com.legally.service;

import com.legally.config.LegallyProperties;
import com.legally.entity.MediaUploadRecord;
import com.legally.entity.UserSession;
import com.legally.repository.ConsultationRecordRepository;
import com.legally.repository.DemandLetterRecordRepository;
import com.legally.repository.MediaUploadRecordRepository;
import com.legally.repository.UserSessionRepository;
import com.legally.security.AuthContext;
import com.legally.security.SessionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class SessionService {

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);

    private final LegallyProperties properties;
    private final UserSessionRepository userSessionRepository;
    private final MediaUploadRecordRepository mediaUploadRecordRepository;
    private final ConsultationRecordRepository consultationRecordRepository;
    private final DemandLetterRecordRepository demandLetterRecordRepository;
    private final StorageService storageService;

    public SessionService(
            LegallyProperties properties,
            UserSessionRepository userSessionRepository,
            MediaUploadRecordRepository mediaUploadRecordRepository,
            ConsultationRecordRepository consultationRecordRepository,
            DemandLetterRecordRepository demandLetterRecordRepository,
            StorageService storageService) {
        this.properties = properties;
        this.userSessionRepository = userSessionRepository;
        this.mediaUploadRecordRepository = mediaUploadRecordRepository;
        this.consultationRecordRepository = consultationRecordRepository;
        this.demandLetterRecordRepository = demandLetterRecordRepository;
        this.storageService = storageService;
    }

  /**
   * Registers or refreshes the current session from {@link SessionContext}.
   */
    @Transactional
    public UUID touchCurrentSession() {
        if (AuthContext.GUEST_UID.equals(AuthContext.currentUserId())) {
            return SessionContext.current().orElse(null);
        }
        UUID sessionId = SessionContext.require();
        touchSession(sessionId);
        return sessionId;
    }

    @Transactional
    public void touchSession(UUID sessionId) {
        String uid = AuthContext.currentUserId();
        if (AuthContext.GUEST_UID.equals(uid)) {
            return;
        }

        UserSession existing = userSessionRepository.findById(sessionId).orElse(null);
        if (existing != null) {
            if (!existing.getFirebaseUid().equals(uid)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Session does not belong to this user");
            }
            existing.setLastActivityAt(Instant.now());
            userSessionRepository.save(existing);
            return;
        }

        userSessionRepository.save(UserSession.create(sessionId, uid));
    }

    @Transactional
    public void endCurrentSession() {
        if (AuthContext.GUEST_UID.equals(AuthContext.currentUserId())) {
            return;
        }
        UUID sessionId = SessionContext.require();
        String uid = AuthContext.currentUserId();
        userSessionRepository.findById(sessionId).ifPresent(session -> {
            if (!session.getFirebaseUid().equals(uid)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Session does not belong to this user");
            }
        });
        purgeSession(sessionId);
        log.info("Ended session {} for user {}", sessionId, uid);
    }

    @Transactional
    public int purgeExpiredSessions() {
        int ttlHours = properties.getSession().getTtlHours();
        Instant cutoff = Instant.now().minus(ttlHours, ChronoUnit.HOURS);
        List<UserSession> expired = userSessionRepository.findByLastActivityAtBefore(cutoff);
        int count = 0;
        for (UserSession session : expired) {
            purgeSession(session.getSessionId());
            count++;
        }
        if (count > 0) {
            log.info("Purged {} session(s) inactive since before {}", count, cutoff);
        }
        return count;
    }

    @Transactional
    public void purgeSession(UUID sessionId) {
        List<MediaUploadRecord> uploads = mediaUploadRecordRepository.findBySessionId(sessionId);
        for (MediaUploadRecord upload : uploads) {
            try {
                storageService.deleteStored(upload.getStoragePath(), upload.getStorageType());
            } catch (Exception e) {
                log.warn("Could not delete file {}: {}", upload.getStoragePath(), e.getMessage());
            }
        }
        mediaUploadRecordRepository.deleteBySessionId(sessionId);
        consultationRecordRepository.deleteBySessionId(sessionId);
        demandLetterRecordRepository.deleteBySessionId(sessionId);
        userSessionRepository.deleteById(sessionId);
    }

}
