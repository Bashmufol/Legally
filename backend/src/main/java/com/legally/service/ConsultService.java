package com.legally.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.legally.model.LawChunk;
import com.legally.model.dto.ConsultRequest;
import com.legally.model.dto.ConsultResponse;
import com.legally.model.dto.GeminiLegalResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConsultService {

    private static final String DISCLAIMER =
            "Legally provides general legal information only, not legal advice. Consult a licensed Nigerian lawyer for your specific case.";

    private final CorpusService corpusService;
    private final GeminiService geminiService;
    private final ContactService contactService;
    private final ConsultationHistoryService consultationHistoryService;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    public ConsultService(
            CorpusService corpusService,
            GeminiService geminiService,
            ContactService contactService,
            ConsultationHistoryService consultationHistoryService,
            UserService userService,
            ObjectMapper objectMapper) {
        this.corpusService = corpusService;
        this.geminiService = geminiService;
        this.contactService = contactService;
        this.consultationHistoryService = consultationHistoryService;
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    public ConsultResponse consult(ConsultRequest request) throws Exception {
        userService.syncCurrentUser();

        List<LawChunk> chunks = corpusService.retrieve(request.getScenario(), request.getMessage(), 8);
        GeminiLegalResponse ai = geminiService.analyze(
                request.getMessage(),
                request.getScenario(),
                chunks,
                request.getMedia());

        ConsultResponse response = new ConsultResponse();
        response.setSummary(ai.getSummary());
        response.setLegalAnalysis(ai.getLegalAnalysis());
        response.setSteps(ai.getSteps());
        response.setDemandLetterEligible(ai.isDemandLetterEligible());
        response.setConfidence(ai.getConfidence());
        response.setDisclaimer(ai.getDisclaimer() != null && !ai.getDisclaimer().isBlank()
                ? ai.getDisclaimer() : DISCLAIMER);
        response.setSources(chunks);
        response.setContacts(contactService.byTags(ai.getSuggestedContactTags()));

        if (response.getContacts().isEmpty()) {
            response.setContacts(contactService.byTags(List.of("legal_aid")));
        }

        consultationHistoryService.save(request, response, objectMapper.writeValueAsString(response));
        return response;
    }
}
