package com.legally.repository;

import com.legally.entity.ConsultationRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConsultationRecordRepository extends JpaRepository<ConsultationRecord, UUID> {

    List<ConsultationRecord> findTop20ByFirebaseUidOrderByCreatedAtDesc(String firebaseUid);
}
