package com.legally.service;

import com.legally.entity.DemandLetterRecord;
import com.legally.model.dto.DemandLetterRequest;
import com.legally.model.dto.DemandLetterResponse;
import com.legally.model.dto.LegalDocumentRequest;
import com.legally.model.dto.LegalDocumentResponse;
import com.legally.repository.DemandLetterRecordRepository;
import com.legally.security.AuthContext;
import com.legally.security.SessionContext;
import org.springframework.stereotype.Service;

import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * Generates demand letters via {@link LegalDocumentService} and persists them per session.
 */
@Service
public class DemandLetterService {

    private final LegalDocumentService legalDocumentService;
    private final DemandLetterRecordRepository demandLetterRecordRepository;

    public DemandLetterService(
            LegalDocumentService legalDocumentService,
            DemandLetterRecordRepository demandLetterRecordRepository) {
        this.legalDocumentService = legalDocumentService;
        this.demandLetterRecordRepository = demandLetterRecordRepository;
    }

    /** Builds a demand letter document and stores it for the current session. */
    @Transactional
    public DemandLetterResponse generate(DemandLetterRequest request) throws Exception {
        LegalDocumentRequest docRequest = new LegalDocumentRequest();
        docRequest.setDocumentType("DEMAND_LETTER");
        docRequest.setFacts(request.getFacts());
        docRequest.setSenderName(request.getSenderName());
        docRequest.setRecipientName(request.getRecipientName());
        docRequest.setPartyAName(request.getSenderName());
        docRequest.setPartyBName(request.getRecipientName());
        docRequest.setCountryCode(request.getCountryCode());
        docRequest.setCountryName(request.getCountryName());
        docRequest.setRegionCode(request.getRegionCode());
        docRequest.setRegionName(request.getRegionName());

        LegalDocumentResponse generated = legalDocumentService.generate(docRequest);
        persistLetter(request, generated.getContent());
        return new DemandLetterResponse(generated.getContent(), generated.getDisclaimer());
    }

    private void persistLetter(DemandLetterRequest request, String letter) {
        String uid = AuthContext.currentUserId();
        if (AuthContext.GUEST_UID.equals(uid)) {
            return;
        }
        UUID sessionId = SessionContext.current().orElse(null);
        demandLetterRecordRepository.save(DemandLetterRecord.create(
                uid,
                sessionId,
                request.getScenario(),
                request.getFacts(),
                letter));
    }
}
