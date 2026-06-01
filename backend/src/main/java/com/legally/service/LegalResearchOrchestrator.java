package com.legally.service;

import com.legally.llm.MultiLlmLegalResearchService;
import com.legally.model.JurisdictionContext;
import com.legally.model.dto.ConsultRequest;
import com.legally.model.dto.LegalResearchResult;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LegalResearchOrchestrator {

    private final MultiLlmLegalResearchService multiLlmLegalResearchService;

    public LegalResearchOrchestrator(MultiLlmLegalResearchService multiLlmLegalResearchService) {
        this.multiLlmLegalResearchService = multiLlmLegalResearchService;
    }

    public LegalResearchResult research(
            String messageText,
            String scenario,
            JurisdictionContext jurisdiction,
            List<ConsultRequest.MediaRef> media) throws Exception {
        return multiLlmLegalResearchService.research(messageText, scenario, jurisdiction, media);
    }
}
