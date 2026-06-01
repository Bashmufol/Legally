package com.legally.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String GENERIC =
            "Something went wrong. Please try again in a moment.";
    private static final String AI_UNAVAILABLE =
            "Our legal assistant is temporarily unavailable. Please try again shortly.";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(e -> friendlyFieldMessage(e.getField(), e.getDefaultMessage()))
                .orElse("Please check your input and try again.");
        return ResponseEntity.badRequest().body(Map.of("error", msg));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, String>> handleMissingParam(MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", "Some required information was missing. Please try again."));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleUnreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", "We couldn't read your request. Please refresh and try again."));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, String>> handleMediaType(HttpMediaTypeNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(Map.of("error", "That file type is not supported. Try an image, PDF, audio, or video."));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxUpload(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(Map.of("error", "That file is too large. Please use a file under 50 MB."));
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<Map<String, String>> handleMultipart(MultipartException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", "We couldn't upload that file. Please try again with a different file."));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        String message = ex.getMessage();
        if (message != null && isSafeUserMessage(message)) {
            return ResponseEntity.badRequest().body(Map.of("error", message));
        }
        return ResponseEntity.badRequest()
                .body(Map.of("error", "Please check your input and try again."));
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<Map<String, String>> handleIo(IOException ex) {
        log.warn("IO error: {}", ex.getMessage());
        if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("not found")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "The file you requested could not be found."));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "We had trouble processing your file. Please try again."));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "You don't have permission to do that. Try refreshing the page."));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, String>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(Map.of("error", "That action is not supported."));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "We couldn't find what you were looking for."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneric(Exception ex) {
        log.error("Unhandled error", ex);
        String friendly = mapKnownTechnicalMessage(ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", friendly));
    }

    private String mapKnownTechnicalMessage(Exception ex) {
        String msg = ex.getMessage();
        if (msg == null) {
            return GENERIC;
        }
        String lower = msg.toLowerCase();
        if (lower.contains("gemini") || lower.contains("generativelanguage") || lower.contains("api key")) {
            return AI_UNAVAILABLE;
        }
        if (lower.contains("firebase") || lower.contains("token")) {
            return "Your session could not be verified. Please refresh the page and try again.";
        }
        if (lower.contains("timeout") || lower.contains("timed out")) {
            return "That took too long. Please try again.";
        }
        if (isSafeUserMessage(msg)) {
            return msg;
        }
        return GENERIC;
    }

    private String friendlyFieldMessage(String field, String defaultMessage) {
        String label = switch (field) {
            case "message" -> "Your description";
            case "facts" -> "The details you provided";
            case "documentType" -> "Document type";
            case "file" -> "File";
            default -> "A required field";
        };
        if (defaultMessage != null && defaultMessage.toLowerCase().contains("must not be blank")) {
            return label + " is required.";
        }
        return "Please check " + label.toLowerCase() + " and try again.";
    }

    private boolean isSafeUserMessage(String message) {
        if (message.length() > 200) {
            return false;
        }
        String lower = message.toLowerCase();
        return !lower.contains("exception")
                && !lower.contains("java.")
                && !lower.contains("org.springframework")
                && !lower.contains("sql")
                && !lower.contains("stack")
                && !lower.contains("nullpointer");
    }
}
