package com.legally.repository;

import com.legally.entity.DemandLetterRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DemandLetterRecordRepository extends JpaRepository<DemandLetterRecord, UUID> {
}
