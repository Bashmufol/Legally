package com.legally.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Metadata for a file uploaded during a session.
 */
@Entity
@Table(name = "media_uploads")
public class MediaUploadRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "firebase_uid", nullable = false, length = 128)
    private String firebaseUid;

    @Column(name = "session_id")
    private UUID sessionId;

    /** Internal path (Firebase or local disk). */
    @Column(name = "storage_path", nullable = false)
    private String storagePath;

    /** URL returned to the client for consult references. */
    @Column(name = "public_url", nullable = false, columnDefinition = "TEXT")
    private String publicUrl;

    @Column(name = "mime_type", nullable = false)
    private String mimeType;

    /** {@code firebase} or {@code local}. */
    @Column(name = "storage_type", nullable = false, length = 32)
    private String storageType;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected MediaUploadRecord() {
    }

    /** Factory for a new upload metadata row. */
    public static MediaUploadRecord create(
            String firebaseUid,
            UUID sessionId,
            String storagePath,
            String publicUrl,
            String mimeType,
            String storageType,
            String fileName) {
        MediaUploadRecord r = new MediaUploadRecord();
        r.firebaseUid = firebaseUid;
        r.sessionId = sessionId;
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

    public UUID getSessionId() {
        return sessionId;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public String getStorageType() {
        return storageType;
    }
}
