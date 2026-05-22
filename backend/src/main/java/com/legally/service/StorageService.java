package com.legally.service;

import com.google.cloud.storage.Blob;
import com.google.firebase.FirebaseApp;
import com.google.firebase.cloud.StorageClient;
import com.legally.config.LegallyProperties;
import com.legally.entity.MediaUploadRecord;
import com.legally.repository.MediaUploadRecordRepository;
import com.legally.security.AuthContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class StorageService {

    private final LegallyProperties properties;
    private final MediaUploadRecordRepository mediaUploadRecordRepository;

    public StorageService(LegallyProperties properties, MediaUploadRecordRepository mediaUploadRecordRepository) {
        this.properties = properties;
        this.mediaUploadRecordRepository = mediaUploadRecordRepository;
    }

    @Transactional
    public StoredFile store(MultipartFile file) throws IOException {
        String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload";
        String safeName = UUID.randomUUID() + "-" + original.replaceAll("[^a-zA-Z0-9._-]", "_");
        String mimeType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        byte[] bytes = file.getBytes();

        if (isFirebaseReady()) {
            String path = "uploads/" + AuthContext.currentUserId() + "/" + safeName;
            StorageClient.getInstance()
                    .bucket()
                    .create(path, bytes, mimeType);
            String firebaseUrl = "firebase://" + path;
            persistUpload(path, firebaseUrl, mimeType, "firebase", safeName);
            return new StoredFile(firebaseUrl, mimeType, "firebase", safeName);
        }

        Path dir = Path.of(properties.getUpload().getLocalDir());
        Files.createDirectories(dir);
        Path target = dir.resolve(safeName);
        Files.write(target, bytes);
        String url = "/api/uploads/files/" + safeName;
        persistUpload(safeName, url, mimeType, "local", safeName);
        return new StoredFile(url, mimeType, "local", safeName);
    }

    private void persistUpload(String storagePath, String publicUrl, String mimeType, String storageType, String fileName) {
        String uid = AuthContext.currentUserId();
        if (AuthContext.GUEST_UID.equals(uid)) {
            return;
        }
        mediaUploadRecordRepository.save(MediaUploadRecord.create(
                uid, storagePath, publicUrl, mimeType, storageType, fileName));
    }

    public byte[] readLocal(String fileName) throws IOException {
        Path path = Path.of(properties.getUpload().getLocalDir()).resolve(fileName);
        if (!Files.exists(path)) {
            throw new IOException("File not found");
        }
        return Files.readAllBytes(path);
    }

    public byte[] readBytes(String url, String storageType) throws IOException {
        if ("local".equals(storageType) && url != null && url.contains("/files/")) {
            int idx = url.indexOf("/files/");
            String name = url.substring(idx + "/files/".length());
            if (name.contains("?")) {
                name = name.substring(0, name.indexOf('?'));
            }
            return readLocal(name);
        }
        if ("firebase".equals(storageType) && url != null && url.startsWith("firebase://")) {
            return readFirebase(url.substring("firebase://".length()));
        }
        throw new IOException("Unsupported storage URL: " + url);
    }

    private byte[] readFirebase(String path) throws IOException {
        if (!isFirebaseReady()) {
            throw new IOException("Firebase Storage not configured");
        }
        Blob blob = StorageClient.getInstance().bucket().get(path);
        if (blob == null || !blob.exists()) {
            throw new IOException("Firebase object not found: " + path);
        }
        return blob.getContent();
    }

    private boolean isFirebaseReady() {
        return properties.getFirebase().isEnabled() && !FirebaseApp.getApps().isEmpty();
    }

    public record StoredFile(String url, String mimeType, String storageType, String fileName) {}
}
