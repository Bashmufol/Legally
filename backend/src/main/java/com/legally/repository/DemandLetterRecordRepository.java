package com.legally.repository;

import com.legally.entity.DemandLetterRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** JPA access for generated demand letters. */
public interface DemandLetterRecordRepository extends JpaRepository<DemandLetterRecord, UUID> {

    void deleteBySessionId(UUID sessionId);
}
