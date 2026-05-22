package com.legally.repository;

import com.legally.entity.MediaUploadRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MediaUploadRecordRepository extends JpaRepository<MediaUploadRecord, UUID> {
}
