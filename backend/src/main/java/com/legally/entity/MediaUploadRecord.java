package com.legally.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "media_uploads")
public class MediaUploadRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "firebase_uid", nullable = false, length = 128)
    private String firebaseUid;

    @Column(name = "storage_path", nullable = false)
    private String storagePath;

    @Column(name = "public_url", nullable = false, columnDefinition = "TEXT")
    private String publicUrl;

    @Column(name = "mime_type", nullable = false)
    private String mimeType;

    @Column(name = "storage_type", nullable = false, length = 32)
    private String storageType;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected MediaUploadRecord() {
    }

    public static MediaUploadRecord create(
            String firebaseUid,
            String storagePath,
            String publicUrl,
            String mimeType,
            String storageType,
            String fileName) {
        MediaUploadRecord r = new MediaUploadRecord();
        r.firebaseUid = firebaseUid;
        r.storagePath = storagePath;
        r.publicUrl = publicUrl;
        r.mimeType = mimeType;
        r.storageType = storageType;
        r.fileName = fileName;
        return r;
    }

    public UUID getId() {
        return id;
    }
}
