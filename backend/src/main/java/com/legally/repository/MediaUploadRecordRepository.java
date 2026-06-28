package com.legally.repository;

import com.legally.entity.MediaUploadRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** JPA access for uploaded file metadata. */
public interface MediaUploadRecordRepository extends JpaRepository<MediaUploadRecord, UUID> {

    List<MediaUploadRecord> findBySessionId(UUID sessionId);

    void deleteBySessionId(UUID sessionId);
}
