package com.legally.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.legally.model.JurisdictionContext;
import com.legally.model.LawChunk;
import com.legally.model.dto.ConsultRequest;
import com.legally.model.dto.ConsultResponse;
import com.legally.model.dto.GeminiLegalResponse;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class ConsultService {

    private final CorpusService corpusService;
    private final GeminiService geminiService;
    private final JurisdictionService jurisdictionService;
    private final ContactService contactService;
    private final ConsultationHistoryService consultationHistoryService;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    public ConsultService(
            CorpusService corpusService,
            GeminiService geminiService,
            JurisdictionService jurisdictionService,
            ContactService contactService,
            ConsultationHistoryService consultationHistoryService,
            UserService userService,
            ObjectMapper objectMapper) {
        this.corpusService = corpusService;
        this.geminiService = geminiService;
        this.jurisdictionService = jurisdictionService;
        this.contactService = contactService;
        this.consultationHistoryService = consultationHistoryService;
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    public ConsultResponse consult(ConsultRequest request) throws Exception {
        userService.syncCurrentUser();

        JurisdictionContext jurisdiction = jurisdictionService.resolve(request);
        jurisdiction = applyGeminiJurisdictionOverride(request, jurisdiction);

        String disclaimer = jurisdictionService.disclaimerFor(jurisdiction);

        List<LawChunk> chunks = corpusService.retrieve(
                jurisdiction, request.getScenario(), request.getMessage(), 8);
        GeminiLegalResponse ai = geminiService.analyze(
                request.getMessage(),
                request.getScenario(),
                jurisdiction,
                chunks,
                request.getMedia());

        enrichCitationSources(ai.getLegalAnalysis(), chunks);

        ConsultResponse response = new ConsultResponse();
        response.setSummary(ai.getSummary());
        response.setLegalAnalysis(ai.getLegalAnalysis());
        response.setSteps(ai.getSteps());
        response.setDemandLetterEligible(ai.isDemandLetterEligible());
        response.setConfidence(ai.getConfidence());
        response.setDisclaimer(ai.getDisclaimer() != null && !ai.getDisclaimer().isBlank()
                ? ai.getDisclaimer() : disclaimer);
        response.setJurisdictionCountry(jurisdiction.getCountryName());
        response.setJurisdictionRegion(jurisdiction.getRegionName());
        response.setLocationSource(jurisdiction.getLocationSource().name());
        response.setCorpusLimited(jurisdiction.isCorpusLimited());
        response.setSources(chunks);
        response.setContacts(contactService.byTags(ai.getSuggestedContactTags()));

        if (response.getContacts().isEmpty()) {
            response.setContacts(contactService.byTags(List.of("legal_aid")));
        }

        consultationHistoryService.save(request, response, objectMapper.writeValueAsString(response));
        return response;
    }

    /**
     * When regex text scan did not override, use Gemini on message + uploads for any country/state.
     */
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
