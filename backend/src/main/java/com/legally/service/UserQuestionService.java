package com.legally.service;

import com.legally.model.dto.ConsultRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * Builds a short display label for history from message text or uploaded media.
 */
@Service
public class UserQuestionService {

    private static final int MAX_QUESTION_LENGTH = 500;

    private final GeminiService geminiService;

    public UserQuestionService(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    /**
     * Text from the user, or a short question extracted from voice/image/video when text is empty.
     */
    public String resolveDisplayQuestion(ConsultRequest request) throws Exception {
        String message = request.getMessage() != null ? request.getMessage().trim() : "";
        if (!message.isBlank()) {
            return abbreviate(message);
        }

        List<ConsultRequest.MediaRef> media =
                request.getMedia() != null ? request.getMedia() : List.of();
        if (media.isEmpty()) {
            return "Legal consultation";
        }

        String extracted = geminiService.extractQuestionFromMedia(media);
        if (extracted != null && !extracted.isBlank()) {
            return abbreviate(extracted);
        }

        return mediaTypeFallback(media);
    }

    private String mediaTypeFallback(List<ConsultRequest.MediaRef> media) {
        ConsultRequest.MediaRef first = media.get(0);
        String mime = first.getMimeType() != null ? first.getMimeType().toLowerCase(Locale.ROOT) : "";
        if (mime.startsWith("audio/")) {
            return "Voice recording consultation";
        }
        if (mime.startsWith("video/")) {
            return "Video evidence consultation";
        }
        if (mime.contains("pdf")) {
            return "Document upload consultation";
        }
        if (mime.startsWith("image/")) {
            return "Image upload consultation";
        }
        return "Uploaded file consultation";
    }

    private String abbreviate(String text) {
        if (text.length() <= MAX_QUESTION_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_QUESTION_LENGTH - 3) + "...";
    }
}
