package com.legally.service;

import com.legally.llm.LegalDocumentDraftRequest;
import com.legally.llm.MultiLlmDocumentService;
import com.legally.model.JurisdictionContext;
import com.legally.model.LegalDocumentType;
import com.legally.model.dto.ConsultRequest;
import com.legally.model.dto.LegalDocumentRequest;
import com.legally.model.dto.LegalDocumentResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class LegalDocumentService {

    private static final String DISCLAIMER =
            "This document is generated for informational purposes only. "
                    + "It is not legal advice. Have a licensed lawyer in your jurisdiction review and adapt it before signing or relying on it.";

    private final MultiLlmDocumentService multiLlmDocumentService;
    private final GeminiService geminiService;
    private final JurisdictionService jurisdictionService;
    private final UserService userService;

    public LegalDocumentService(
            MultiLlmDocumentService multiLlmDocumentService,
            GeminiService geminiService,
            JurisdictionService jurisdictionService,
            UserService userService) {
        this.multiLlmDocumentService = multiLlmDocumentService;
        this.geminiService = geminiService;
        this.jurisdictionService = jurisdictionService;
        this.userService = userService;
    }

    public LegalDocumentResponse generate(LegalDocumentRequest request) throws Exception {
        userService.syncCurrentUser();

        LegalDocumentType docType = LegalDocumentType.fromApiValue(request.getDocumentType());
        String combinedFacts = buildCombinedFacts(request);

        ConsultRequest jurisdictionProbe = new ConsultRequest();
        jurisdictionProbe.setMessage(combinedFacts);
        jurisdictionProbe.setCountryCode(request.getCountryCode());
        jurisdictionProbe.setCountryName(request.getCountryName());
        jurisdictionProbe.setRegionCode(request.getRegionCode());
        jurisdictionProbe.setRegionName(request.getRegionName());

        JurisdictionContext jurisdiction = jurisdictionService.resolve(jurisdictionProbe);
        if (jurisdiction.getLocationSource() != JurisdictionContext.LocationSource.input_override
                && jurisdiction.getLocationSource() != JurisdictionContext.LocationSource.device) {
            final JurisdictionContext baseline = jurisdiction;
            Optional<JurisdictionContext> detected = geminiService.detectJurisdictionFromInputs(
                    combinedFacts, List.of(), baseline);
            jurisdiction = detected
                    .filter(ctx -> jurisdictionService.isExplicitUserJurisdiction(ctx, baseline))
                    .map(jurisdictionService::applyDetectedOverride)
                    .orElse(baseline);
        }
        jurisdictionService.requireResolved(jurisdiction);

        String title = resolveTitle(docType, request);
        LegalDocumentDraftRequest draftRequest = new LegalDocumentDraftRequest(
                docType,
                title,
                combinedFacts,
                request.getAdditionalDetails(),
                request.getPartyAName(),
                request.getPartyBName(),
                jurisdiction);
        String content = multiLlmDocumentService.generate(draftRequest);

        content = applyPartyPlaceholders(content, request);

        return new LegalDocumentResponse(
                docType.name(),
                title,
                content,
                DISCLAIMER,
                jurisdiction.getCountryName(),
                jurisdiction.getRegionName(),
                jurisdiction.getLocationSource().name());
    }

    private String buildCombinedFacts(LegalDocumentRequest request) {
        StringBuilder sb = new StringBuilder(request.getFacts());
        if (request.getAdditionalDetails() != null && !request.getAdditionalDetails().isBlank()) {
            sb.append("\n\nAdditional details: ").append(request.getAdditionalDetails());
        }
        if (request.getCustomDocumentName() != null && !request.getCustomDocumentName().isBlank()) {
            sb.append("\n\nRequested document name: ").append(request.getCustomDocumentName());
        }
        return sb.toString();
    }

    private String resolveTitle(LegalDocumentType docType, LegalDocumentRequest request) {
        if (docType == LegalDocumentType.OTHER
                && request.getCustomDocumentName() != null
                && !request.getCustomDocumentName().isBlank()) {
            return request.getCustomDocumentName().trim();
        }
        return docType.getDisplayName();
    }

    private String applyPartyPlaceholders(String content, LegalDocumentRequest request) {
        String result = content;
        String partyA = firstNonBlank(request.getPartyAName(), request.getSenderName());
        String partyB = firstNonBlank(request.getPartyBName(), request.getRecipientName());
        if (partyA != null) {
            result = result.replace("[PARTY A]", partyA)
                    .replace("[PARTY A NAME]", partyA)
                    .replace("[YOUR NAME]", partyA)
                    .replace("[YOUR FULL NAME]", partyA)
                    .replace("[LANDLORD NAME]", partyA)
                    .replace("[TENANT NAME]", partyA);
        }
        if (partyB != null) {
            result = result.replace("[PARTY B]", partyB)
                    .replace("[PARTY B NAME]", partyB)
                    .replace("[LANDLORD / COUNTERPARTY NAME]", partyB)
                    .replace("[LANDLORD NAME]", partyB)
                    .replace("[TENANT NAME]", partyB);
        }
        return result;
    }

    private String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
    }
}
