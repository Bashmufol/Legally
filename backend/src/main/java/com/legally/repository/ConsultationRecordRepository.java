package com.legally.repository;

import com.legally.entity.ConsultationRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConsultationRecordRepository extends JpaRepository<ConsultationRecord, UUID> {

    List<ConsultationRecord> findTop7ByFirebaseUidAndSessionIdOrderByCreatedAtDesc(
            String firebaseUid, UUID sessionId);

    List<ConsultationRecord> findByFirebaseUidAndSessionIdOrderByCreatedAtDesc(
            String firebaseUid, UUID sessionId);

    Optional<ConsultationRecord> findByIdAndFirebaseUidAndSessionId(
            UUID id, String firebaseUid, UUID sessionId);

    void deleteBySessionId(UUID sessionId);
}
