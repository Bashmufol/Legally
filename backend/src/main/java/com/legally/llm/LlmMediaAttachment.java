package com.legally.llm;

import com.legally.model.dto.ConsultRequest;
import com.legally.service.StorageService;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Builds Gemini multimodal parts from stored uploads.
 */
final class LlmMediaAttachment {

    private LlmMediaAttachment() {
    }

    static List<Map<String, Object>> buildParts(
            String textPrompt, List<ConsultRequest.MediaRef> media, StorageService storageService) {
        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of("text", textPrompt));
        if (media != null) {
            for (ConsultRequest.MediaRef ref : media) {
                attach(parts, ref, storageService);
            }
        }
        return parts;
    }

    private static void attach(
            List<Map<String, Object>> parts, ConsultRequest.MediaRef ref, StorageService storageService) {
        try {
            byte[] bytes = storageService.readBytes(ref.getUrl(), ref.getStorageType());
            String mime = ref.getMimeType() != null ? ref.getMimeType() : "application/octet-stream";
            String base64 = Base64.getEncoder().encodeToString(bytes);
            parts.add(Map.of("inlineData", Map.of("mimeType", mime, "data", base64)));
        } catch (Exception e) {
            parts.add(Map.of("text", "[Could not load attached media: " + ref.getUrl() + "]"));
        }
    }
}
