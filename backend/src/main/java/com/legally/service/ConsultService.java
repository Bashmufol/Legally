package com.legally.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.legally.model.JurisdictionContext;
import com.legally.model.LawChunk;
import com.legally.model.dto.ConsultRequest;
import com.legally.model.dto.ConsultResponse;
import com.legally.model.dto.GeminiLegalResponse;
import com.legally.model.dto.LegalResearchResult;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class ConsultService {

    private final LegalResearchOrchestrator legalResearchOrchestrator;
    private final GeminiService geminiService;
    private final JurisdictionService jurisdictionService;
    private final ContactResearchService contactResearchService;
    private final ConsultationHistoryService consultationHistoryService;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    public ConsultService(
            LegalResearchOrchestrator legalResearchOrchestrator,
            GeminiService geminiService,
            JurisdictionService jurisdictionService,
            ContactResearchService contactResearchService,
            ConsultationHistoryService consultationHistoryService,
            UserService userService,
            ObjectMapper objectMapper) {
        this.legalResearchOrchestrator = legalResearchOrchestrator;
        this.geminiService = geminiService;
        this.jurisdictionService = jurisdictionService;
        this.contactResearchService = contactResearchService;
        this.consultationHistoryService = consultationHistoryService;
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    public ConsultResponse consult(ConsultRequest request) throws Exception {
        userService.syncCurrentUser();

        validateConsultInput(request);
        String messageText = normalizedMessage(request);

        ConsultRequest jurisdictionRequest = copyForJurisdiction(request, messageText);
        JurisdictionContext jurisdiction = jurisdictionService.resolve(jurisdictionRequest);
        jurisdiction = applyGeminiJurisdictionOverride(jurisdictionRequest, jurisdiction);

        LegalResearchResult research = legalResearchOrchestrator.research(
                messageText,
                request.getScenario(),
                jurisdiction,
                request.getMedia());

        GeminiLegalResponse ai = research.getResponse();
        List<LawChunk> chunks = research.getSources();

        enrichCitationSources(ai.getLegalAnalysis(), chunks);

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
        response.setJurisdictionRegion(jurisdiction.getRegionName());
        response.setLocationSource(jurisdiction.getLocationSource().name());
        response.setCorpusLimited(false);
        response.setSources(chunks);
        response.setContacts(contactResearchService.findContacts(
                jurisdiction, request.getScenario(), messageText));

        consultationHistoryService.save(request, response, objectMapper.writeValueAsString(response));
        return response;
    }

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

    private JurisdictionContext applyGeminiJurisdictionOverride(
            ConsultRequest request, JurisdictionContext jurisdiction) throws Exception {
        if (jurisdiction.getLocationSource() == com.legally.model.JurisdictionContext.LocationSource.input_override) {
            return jurisdiction;
        }
        Optional<JurisdictionContext> detected = geminiService.detectJurisdictionFromInputs(
                request.getMessage(),
                request.getMedia() != null ? request.getMedia() : List.of(),
                jurisdiction);
        return detected
                .map(jurisdictionService::applyDetectedOverride)
                .orElse(jurisdiction);
    }
}
