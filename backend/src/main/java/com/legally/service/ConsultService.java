package com.legally.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.legally.model.JurisdictionContext;
import com.legally.model.LawChunk;
import com.legally.model.dto.ConsultRequest;
import com.legally.model.dto.ConsultResponse;
import com.legally.model.dto.GeminiLegalResponse;
import com.legally.llm.GoogleSpeechToTextService;
import com.legally.llm.MultiLlmLegalResearchService;
import com.legally.model.dto.LegalResearchResult;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Main consult workflow: jurisdiction, legal research, contacts, and history persistence.
 */
@Service
public class ConsultService {

    private final MultiLlmLegalResearchService legalResearchService;
    private final GeminiService geminiService;
    private final JurisdictionService jurisdictionService;
    private final ContactResearchService contactResearchService;
    private final GoogleSpeechToTextService googleSpeechToTextService;
    private final ConsultationHistoryService consultationHistoryService;
    private final SessionService sessionService;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    public ConsultService(
            MultiLlmLegalResearchService legalResearchService,
            GeminiService geminiService,
            JurisdictionService jurisdictionService,
            ContactResearchService contactResearchService,
            GoogleSpeechToTextService googleSpeechToTextService,
            ConsultationHistoryService consultationHistoryService,
            SessionService sessionService,
            UserService userService,
            ObjectMapper objectMapper) {
        this.legalResearchService = legalResearchService;
        this.geminiService = geminiService;
        this.jurisdictionService = jurisdictionService;
        this.contactResearchService = contactResearchService;
        this.googleSpeechToTextService = googleSpeechToTextService;
        this.consultationHistoryService = consultationHistoryService;
        this.sessionService = sessionService;
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    /** Runs the full consult pipeline and returns the API response. */
    public ConsultResponse consult(ConsultRequest request) throws Exception {
        userService.syncCurrentUser();
        sessionService.touchCurrentSession();

        validateConsultInput(request);
        String messageText = normalizedMessage(request);

        String jurisdictionMessage = enrichJurisdictionMessageWithVoice(messageText, request.getMedia());
        ConsultRequest jurisdictionRequest = copyForJurisdiction(request, jurisdictionMessage);
        JurisdictionContext jurisdiction = jurisdictionService.resolve(jurisdictionRequest);
        jurisdiction = applyGeminiJurisdictionOverride(jurisdictionRequest, jurisdiction);
        if (!jurisdictionService.isResolved(jurisdiction)) {
            JurisdictionContext displayJurisdiction = displayFallbackJurisdiction(jurisdiction, request);
            GeminiLegalResponse unresolved = geminiService.buildJurisdictionUnresolvedResponse();
            ConsultResponse response = buildConsultResponse(
                    request,
                    messageText,
                    unresolved,
                    displayJurisdiction,
                    List.of(),
                    false);
            consultationHistoryService.save(request, response, objectMapper.writeValueAsString(response));
            return response;
        }

        LegalResearchResult research = legalResearchService.research(
                messageText,
                request.getScenario(),
                jurisdiction,
                request.getMedia());

        GeminiLegalResponse ai = research.getResponse();
        List<LawChunk> chunks = research.getSources();

        enrichCitationSources(ai.getLegalAnalysis(), chunks);
        ConsultResponse response = buildConsultResponse(
                request,
                messageText,
                ai,
                jurisdiction,
                chunks,
                research.isSuggestContacts());

        consultationHistoryService.save(request, response, objectMapper.writeValueAsString(response));
        return response;
    }

    /** Maps AI output and metadata into the client response shape. */
    private ConsultResponse buildConsultResponse(
            ConsultRequest request,
            String messageText,
            GeminiLegalResponse ai,
            JurisdictionContext jurisdiction,
            List<LawChunk> chunks,
            boolean includeContacts) {
        String disclaimer = ai.getDisclaimer() != null && !ai.getDisclaimer().isBlank()
                ? ai.getDisclaimer()
                : jurisdictionService.disclaimerFor(jurisdiction);

        ConsultResponse response = new ConsultResponse();
        response.setSummary(ai.getSummary());
        response.setLegalAnalysis(ai.getLegalAnalysis());
        response.setSteps(ai.getSteps());
        response.setDemandLetterEligible(ai.isDemandLetterEligible());
        response.setConfidence(ai.getConfidence());
        response.setDisclaimer(disclaimer);
        response.setJurisdictionCountry(jurisdiction.getCountryName());
        response.setJurisdictionRegion(jurisdiction.displayableRegionName());
        response.setLocationSource(jurisdiction.getLocationSource().name());
        response.setCorpusLimited(false);
        response.setSources(chunks);
        if (includeContacts) {
            response.setContacts(contactResearchService.findContacts(
                    jurisdiction,
                    request.getScenario(),
                    messageText,
                    ai.getSuggestedContactTags(),
                    ai.getSummary()));
        } else {
            response.setContacts(List.of());
        }
        return response;
    }

    /** Requires at least message text or one uploaded file. */
    private void validateConsultInput(ConsultRequest request) {
        boolean hasMessage = request.getMessage() != null && !request.getMessage().isBlank();
        boolean hasMedia = request.getMedia() != null && !request.getMedia().isEmpty();
        if (!hasMessage && !hasMedia) {
            throw new IllegalArgumentException(
                    "Provide a written description, a voice recording, or an uploaded file (or combination).");
        }
    }

    private String normalizedMessage(ConsultRequest request) {
        if (request.getMessage() == null) {
            return "";
        }
        return request.getMessage().trim();
    }

    /** Appends a speech-to-text transcript when audio is attached. */
    private String enrichJurisdictionMessageWithVoice(
            String messageText, List<ConsultRequest.MediaRef> media) {
        String base = messageText != null ? messageText : "";
        try {
            Optional<String> transcript = googleSpeechToTextService.transcribeAudio(media);
            if (transcript.isEmpty() || transcript.get().isBlank()) {
                return base;
            }
            if (base.isBlank()) {
                return transcript.get();
            }
            return base + "\n\nUser media transcript:\n" + transcript.get();
        } catch (Exception ignored) {
            return base;
        }
    }

    /** Fills display fields when jurisdiction is unresolved. */
    private JurisdictionContext displayFallbackJurisdiction(
            JurisdictionContext resolved,
            ConsultRequest request) {
        if (resolved != null && resolved.getCountryName() != null && !resolved.getCountryName().isBlank()) {
            return resolved;
        }
        JurisdictionContext fallback = new JurisdictionContext();
        fallback.setCountryCode(
                request.getCountryCode() != null && !request.getCountryCode().isBlank()
                        ? request.getCountryCode()
                        : "UNK");
        fallback.setCountryName(
                request.getCountryName() != null && !request.getCountryName().isBlank()
                        ? request.getCountryName()
                        : "your area");
        fallback.setRegionCode(
                request.getRegionCode() != null && !request.getRegionCode().isBlank()
                        ? request.getRegionCode()
                        : "GENERAL");
        fallback.setRegionName(
                request.getRegionName() != null && !request.getRegionName().isBlank()
                        ? request.getRegionName()
                        : "General");
        fallback.setLocationSource(com.legally.model.JurisdictionContext.LocationSource.default_fallback);
        return fallback;
    }

    /** Copies consult fields for jurisdiction parsing without mutating the original request. */
    private ConsultRequest copyForJurisdiction(ConsultRequest request, String messageText) {
        ConsultRequest copy = new ConsultRequest();
        copy.setMessage(messageText);
        copy.setScenario(request.getScenario());
        copy.setMedia(request.getMedia());
        copy.setCountryCode(request.getCountryCode());
        copy.setCountryName(request.getCountryName());
        copy.setRegionCode(request.getRegionCode());
        copy.setRegionName(request.getRegionName());
        copy.setLocationSource(request.getLocationSource());
        copy.setJurisdictionOverride(request.getJurisdictionOverride());
        return copy;
    }

    /** Copies source URLs from law chunks onto legal point citations. */
    private void enrichCitationSources(List<GeminiLegalResponse.LegalPoint> points, List<LawChunk> chunks) {
        Map<String, LawChunk> byId = new HashMap<>();
        for (LawChunk chunk : chunks) {
            byId.put(chunk.getId(), chunk);
        }
        for (GeminiLegalResponse.LegalPoint point : points) {
            if (point.getCitation() == null) {
                continue;
            }
            LawChunk chunk = point.getChunkId() != null ? byId.get(point.getChunkId()) : null;
            if (chunk == null) {
                for (LawChunk c : chunks) {
                    if (Objects.equals(c.getInstrument(), point.getCitation().getInstrument())
                            && Objects.equals(c.getSection(), point.getCitation().getSection())) {
                        chunk = c;
                        break;
                    }
                }
            }
            if (chunk != null && chunk.getSourceUrl() != null && !chunk.getSourceUrl().isBlank()) {
                point.getCitation().setSourceUrl(chunk.getSourceUrl());
            }
        }
    }

    /** Applies Gemini or text-based jurisdiction when the user names a different place. */
    private JurisdictionContext applyGeminiJurisdictionOverride(
            ConsultRequest request, JurisdictionContext jurisdiction) throws Exception {
        if (jurisdiction.getLocationSource() == com.legally.model.JurisdictionContext.LocationSource.input_override) {
            return jurisdiction;
        }

        String message = request.getMessage() != null ? request.getMessage() : "";
        Optional<JurisdictionContext> fromText = jurisdictionService.extractFromUserMessage(message);
        if (fromText.isPresent()
                && jurisdictionService.isExplicitUserJurisdiction(fromText.get(), jurisdiction)) {
            return jurisdictionService.applyDetectedOverride(fromText.get());
        }

        boolean hasMedia = request.getMedia() != null && !request.getMedia().isEmpty();
        boolean hasMessage = !message.isBlank();
        if (!hasMessage && !hasMedia) {
            return jurisdiction;
        }

        Optional<JurisdictionContext> detected = geminiService.detectJurisdictionFromInputs(
                request.getMessage(),
                request.getMedia() != null ? request.getMedia() : List.of(),
                jurisdiction);
        final JurisdictionContext baseline = jurisdiction;
        return detected
                .filter(ctx -> jurisdictionService.isExplicitUserJurisdiction(ctx, baseline))
                .map(jurisdictionService::applyDetectedOverride)
                .orElse(baseline);
    }
}
