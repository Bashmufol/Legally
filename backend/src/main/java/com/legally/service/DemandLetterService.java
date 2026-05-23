package com.legally.service;

import com.legally.entity.DemandLetterRecord;
import com.legally.model.JurisdictionContext;
import com.legally.model.LawChunk;
import com.legally.model.dto.ConsultRequest;
import com.legally.model.dto.DemandLetterRequest;
import com.legally.model.dto.DemandLetterResponse;
import com.legally.repository.DemandLetterRecordRepository;
import com.legally.security.AuthContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class DemandLetterService {

    private static final String DISCLAIMER =
            "This template is for informational purposes only. Review with a licensed lawyer before sending.";

    private final CorpusService corpusService;
    private final GeminiService geminiService;
    private final JurisdictionService jurisdictionService;
    private final DemandLetterRecordRepository demandLetterRecordRepository;
    private final UserService userService;

    public DemandLetterService(
            CorpusService corpusService,
            GeminiService geminiService,
            JurisdictionService jurisdictionService,
            DemandLetterRecordRepository demandLetterRecordRepository,
            UserService userService) {
        this.corpusService = corpusService;
        this.geminiService = geminiService;
        this.jurisdictionService = jurisdictionService;
        this.demandLetterRecordRepository = demandLetterRecordRepository;
        this.userService = userService;
    }

    @Transactional
    public DemandLetterResponse generate(DemandLetterRequest request) throws Exception {
        userService.syncCurrentUser();

        ConsultRequest jurisdictionProbe = new ConsultRequest();
        jurisdictionProbe.setMessage(request.getFacts());
        jurisdictionProbe.setCountryCode(request.getCountryCode());
        jurisdictionProbe.setCountryName(request.getCountryName());
        jurisdictionProbe.setRegionCode(request.getRegionCode());
        jurisdictionProbe.setRegionName(request.getRegionName());
        JurisdictionContext jurisdiction = jurisdictionService.resolve(jurisdictionProbe);
        if (jurisdiction.getLocationSource() != JurisdictionContext.LocationSource.input_override) {
            Optional<JurisdictionContext> detected = geminiService.detectJurisdictionFromInputs(
                    request.getFacts(), List.of(), jurisdiction);
            jurisdiction = detected
                    .map(jurisdictionService::applyDetectedOverride)
                    .orElse(jurisdiction);
        }

        List<LawChunk> chunks = corpusService.retrieve(
                jurisdiction,
                request.getScenario() != null ? request.getScenario() : "tenancy",
                request.getFacts(),
                5);

        String letter = geminiService.generateDemandLetter(
                request.getFacts(),
                request.getScenario(),
                jurisdiction,
                chunks);

        if (request.getSenderName() != null && !request.getSenderName().isBlank()) {
            letter = letter.replace("[YOUR NAME]", request.getSenderName());
            letter = letter.replace("[YOUR FULL NAME]", request.getSenderName());
        }
        if (request.getRecipientName() != null && !request.getRecipientName().isBlank()) {
            letter = letter.replace("[LANDLORD / COUNTERPARTY NAME]", request.getRecipientName());
        }

        persistLetter(request, letter);
        return new DemandLetterResponse(letter, DISCLAIMER);
    }

    private void persistLetter(DemandLetterRequest request, String letter) {
        String uid = AuthContext.currentUserId();
        if (AuthContext.GUEST_UID.equals(uid)) {
            return;
        }
        demandLetterRecordRepository.save(DemandLetterRecord.create(
                uid,
                request.getScenario(),
                request.getFacts(),
                letter));
    }
}
