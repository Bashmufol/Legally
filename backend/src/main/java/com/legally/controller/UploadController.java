package com.legally.controller;

import com.legally.model.dto.UploadResponse;
import com.legally.service.StorageService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    private final StorageService storageService;

    public UploadController(StorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping
    public ResponseEntity<UploadResponse> upload(@RequestParam("file") MultipartFile file) throws Exception {
        StorageService.StoredFile stored = storageService.store(file);
        return ResponseEntity.ok(new UploadResponse(
                stored.url(),
                stored.mimeType(),
                stored.storageType(),
                stored.fileName()));
    }

    @GetMapping("/files/{fileName}")
    public ResponseEntity<byte[]> serveLocal(@PathVariable String fileName) throws Exception {
        byte[] data = storageService.readLocal(fileName);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }
}
