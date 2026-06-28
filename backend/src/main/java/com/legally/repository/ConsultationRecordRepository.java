package com.legally.repository;

import com.legally.entity.ConsultationRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** JPA access for consultation history rows. */
public interface ConsultationRecordRepository extends JpaRepository<ConsultationRecord, UUID> {

    /** Recent consultations for the history sidebar (limited to seven). */
    List<ConsultationRecord> findTop7ByFirebaseUidAndSessionIdOrderByCreatedAtDesc(
            String firebaseUid, UUID sessionId);

    List<ConsultationRecord> findByFirebaseUidAndSessionIdOrderByCreatedAtDesc(
            String firebaseUid, UUID sessionId);

    /** Loads one consultation when id, user, and session all match. */
    Optional<ConsultationRecord> findByIdAndFirebaseUidAndSessionId(
            UUID id, String firebaseUid, UUID sessionId);

    void deleteBySessionId(UUID sessionId);
}
