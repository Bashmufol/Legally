package com.legally.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.legally.model.JurisdictionContext;
import com.legally.model.LawChunk;
import com.legally.model.WebLegalSource;
import com.legally.model.dto.ConsultRequest;
import com.legally.model.dto.ConsultResponse;
import com.legally.model.dto.GeminiLegalResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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
    private final WebResearchService webResearchService;
    private final ObjectMapper objectMapper;

    public ConsultService(
            CorpusService corpusService,
            GeminiService geminiService,
            JurisdictionService jurisdictionService,
            ContactService contactService,
            ConsultationHistoryService consultationHistoryService,
            UserService userService,
            WebResearchService webResearchService,
            ObjectMapper objectMapper) {
        this.corpusService = corpusService;
        this.geminiService = geminiService;
        this.jurisdictionService = jurisdictionService;
        this.contactService = contactService;
        this.consultationHistoryService = consultationHistoryService;
        this.userService = userService;
        this.webResearchService = webResearchService;
        this.objectMapper = objectMapper;
    }

    public ConsultResponse consult(ConsultRequest request) throws Exception {
        userService.syncCurrentUser();

        validateConsultInput(request);
        String messageText = normalizedMessage(request);

        ConsultRequest jurisdictionRequest = copyForJurisdiction(request, messageText);
        JurisdictionContext jurisdiction = jurisdictionService.resolve(jurisdictionRequest);
        jurisdiction = applyGeminiJurisdictionOverride(jurisdictionRequest, jurisdiction);

        boolean useNigerianCorpus = usesNigerianCorpus(jurisdiction);

        List<LawChunk> chunks;
        GeminiLegalResponse ai;
        String disclaimer;

        if (useNigerianCorpus) {
            chunks = corpusService.retrieve(jurisdiction, request.getScenario(), messageText, 8);
            ai = geminiService.analyze(
                    messageText,
                    request.getScenario(),
                    jurisdiction,
                    chunks,
                    request.getMedia());
            disclaimer = jurisdictionService.disclaimerFor(jurisdiction);
        } else {
            List<WebLegalSource> webSources = webResearchService.research(
                    jurisdiction, request.getScenario(), messageText);
            if (!webSources.isEmpty()) {
                chunks = webResearchService.toLawChunks(webSources, jurisdiction);
                ai = geminiService.analyzeFromWebSources(
                        messageText,
                        request.getScenario(),
                        jurisdiction,
                        webSources,
                        request.getMedia());
            } else {
                ai = geminiService.analyzeWithGoogleSearchGrounding(
                        messageText,
                        request.getScenario(),
                        jurisdiction,
                        request.getMedia());
                chunks = buildGroundingSources(ai, jurisdiction);
            }
            disclaimer = ai.getDisclaimer() != null && !ai.getDisclaimer().isBlank()
                    ? ai.getDisclaimer()
                    : jurisdictionService.disclaimerFor(jurisdiction);
        }

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
        response.setCorpusLimited(useNigerianCorpus && jurisdiction.isCorpusLimited());
        response.setSources(chunks);
        response.setContacts(contactService.byTags(ai.getSuggestedContactTags()));

        if (response.getContacts().isEmpty()) {
            response.setContacts(contactService.byTags(List.of("legal_aid")));
        }

        consultationHistoryService.save(request, response, objectMapper.writeValueAsString(response));
        return response;
    }

    private static boolean usesNigerianCorpus(JurisdictionContext jurisdiction) {
        return jurisdiction.getCountryCode() != null
                && "NG".equalsIgnoreCase(jurisdiction.getCountryCode());
    }

    private List<LawChunk> buildGroundingSources(GeminiLegalResponse ai, JurisdictionContext jurisdiction) {
        List<LawChunk> chunks = new ArrayList<>();
        int i = 0;
        for (GeminiLegalResponse.LegalPoint point : ai.getLegalAnalysis()) {
            if (point.getCitation() == null || point.getCitation().getSourceUrl() == null) {
                continue;
            }
            String url = point.getCitation().getSourceUrl();
            LawChunk c = new LawChunk();
            c.setId("web-ground-" + i++);
            c.setCountryCode(jurisdiction.getCountryCode());
            c.setRegionCode(jurisdiction.getRegionCode());
            c.setJurisdiction(jurisdiction.getCountryCode());
            c.setInstrument(point.getCitation().getInstrument());
            c.setSection(point.getCitation().getSection());
            c.setTitle(point.getCitation().getInstrument());
            c.setText(point.getPoint());
            c.setSourceUrl(url);
            chunks.add(c);
        }
        return chunks;
    }

    private void validateConsultInput(ConsultRequest request) {
        boolean hasMessage = request.getMessage() != null && !request.getMessage().isBlank();
        boolean hasMedia = request.getMedia() != null && !request.getMedia().isEmpty();
        if (!hasMessage && !hasMedia) {
            throw new IllegalArgumentException(
                    "Provide a written description, a voice recording, or an uploaded file (or any combination).");
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
