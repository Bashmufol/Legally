package com.legally.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.legally.config.LegallyProperties;
import com.legally.model.dto.ConsultRequest;
import com.legally.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class GoogleSpeechToTextService {

    private static final Logger log = LoggerFactory.getLogger(GoogleSpeechToTextService.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final StorageService storageService;
    private final LegallyProperties properties;

    private GoogleCredentials cachedCredentials;
    private Instant tokenExpiresAt = Instant.EPOCH;
    private String cachedBearerToken;

    public GoogleSpeechToTextService(
            RestClient restClient,
            ObjectMapper objectMapper,
            StorageService storageService,
            LegallyProperties properties) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.storageService = storageService;
        this.properties = properties;
    }

    public Optional<String> transcribeAudio(List<ConsultRequest.MediaRef> media) {
        if (media == null || media.isEmpty()) {
            return Optional.empty();
        }

        List<String> snippets = new ArrayList<>();
        for (ConsultRequest.MediaRef ref : media) {
            if (!isAudioLike(ref)) {
                continue;
            }
            try {
                Optional<String> transcript = transcribeSingle(ref);
                transcript.ifPresent(snippets::add);
            } catch (Exception e) {
                log.warn("Speech-to-Text failed for attachment: {}", e.getMessage());
            }
        }

        if (snippets.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(String.join("\n\n", snippets));
    }

    private Optional<String> transcribeSingle(ConsultRequest.MediaRef ref) throws Exception {
        byte[] bytes = storageService.readBytes(ref.getUrl(), ref.getStorageType());
        if (bytes.length == 0) {
            return Optional.empty();
        }

        String token = bearerToken();
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        String projectId = projectId();
        if (projectId == null || projectId.isBlank()) {
            log.warn("Speech-to-Text skipped: GOOGLE_CLOUD_PROJECT/FIREBASE_PROJECT_ID not set");
            return Optional.empty();
        }

        String url = "https://speech.googleapis.com/v2/projects/" + projectId
                + "/locations/global/recognizers/_:recognize";

        Map<String, Object> body = Map.of(
                "config", Map.of(
                        "autoDecodingConfig", Map.of(),
                        "languageCodes", List.of("en-US"),
                        "model", "latest_long"),
                "content", Base64.getEncoder().encodeToString(bytes));

        String responseBody = restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .body(body)
                .retrieve()
                .body(String.class);

        JsonNode root = objectMapper.readTree(responseBody);
        StringBuilder text = new StringBuilder();
        for (JsonNode result : root.path("results")) {
            JsonNode alt = result.path("alternatives").path(0);
            String snippet = alt.path("transcript").asText("").trim();
            if (!snippet.isBlank()) {
                if (text.length() > 0) {
                    text.append(' ');
                }
                text.append(snippet);
            }
        }
        String transcript = text.toString().trim();
        return transcript.isBlank() ? Optional.empty() : Optional.of(transcript);
    }

    private boolean isAudioLike(ConsultRequest.MediaRef ref) {
        if (ref == null || ref.getMimeType() == null) {
            return false;
        }
        String mime = ref.getMimeType().toLowerCase();
        return mime.startsWith("audio/") || mime.startsWith("video/");
    }

    private synchronized String bearerToken() {
        try {
            if (cachedCredentials == null) {
                cachedCredentials = GoogleCredentials.getApplicationDefault()
                        .createScoped("https://www.googleapis.com/auth/cloud-platform");
            }
            if (cachedBearerToken != null && Instant.now().isBefore(tokenExpiresAt.minusSeconds(60))) {
                return cachedBearerToken;
            }
            cachedCredentials.refreshIfExpired();
            AccessToken token = cachedCredentials.getAccessToken();
            if (token == null) {
                return null;
            }
            cachedBearerToken = token.getTokenValue();
            tokenExpiresAt = token.getExpirationTime() != null
                    ? token.getExpirationTime().toInstant()
                    : Instant.now().plusSeconds(300);
            return cachedBearerToken;
        } catch (Exception e) {
            log.warn("Speech-to-Text auth unavailable: {}", e.getMessage());
            return null;
        }
    }

    private String projectId() {
        String fromProps = properties.getFirebase().getProjectId();
        if (fromProps != null && !fromProps.isBlank()) {
            return fromProps.trim();
        }
        String fromEnv = System.getenv("GOOGLE_CLOUD_PROJECT");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }
        return null;
    }
}
