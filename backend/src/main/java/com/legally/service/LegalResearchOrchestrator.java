package com.legally.service;

import com.legally.model.JurisdictionContext;
import com.legally.model.LawChunk;
import com.legally.model.WebLegalSource;
import com.legally.model.dto.ConsultRequest;
import com.legally.model.dto.GeminiLegalResponse;
import com.legally.model.dto.LegalResearchResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class LegalResearchOrchestrator {

    private final WebResearchService webResearchService;
    private final GeminiService geminiService;

    public LegalResearchOrchestrator(WebResearchService webResearchService, GeminiService geminiService) {
        this.webResearchService = webResearchService;
        this.geminiService = geminiService;
    }

    public LegalResearchResult research(
            String messageText,
            String scenario,
            JurisdictionContext jurisdiction,
            List<ConsultRequest.MediaRef> media) throws Exception {

        List<WebLegalSource> webSources = webResearchService.research(jurisdiction, scenario, messageText);

        if (!webSources.isEmpty()) {
            GeminiLegalResponse ai = geminiService.analyzeFromWebSources(
                    messageText, scenario, jurisdiction, webSources, media);
            List<LawChunk> chunks = webResearchService.toLawChunks(webSources, jurisdiction);
            if (hasSubstantiveLegalContent(ai)) {
                return new LegalResearchResult(ai, chunks, true);
            }
        }

        GeminiLegalResponse grounded = geminiService.analyzeWithGoogleSearchGrounding(
                messageText, scenario, jurisdiction, media);
        List<LawChunk> groundingChunks = buildGroundingSources(grounded, jurisdiction);

        if (hasSubstantiveLegalContent(grounded)) {
            return new LegalResearchResult(grounded, groundingChunks, true);
        }

        GeminiLegalResponse noInfo = geminiService.buildNoInformationResponse(
                messageText, scenario, jurisdiction);
        return new LegalResearchResult(noInfo, List.of(), false);
    }

    private boolean hasSubstantiveLegalContent(GeminiLegalResponse ai) {
        if (ai == null || ai.getLegalAnalysis() == null || ai.getLegalAnalysis().isEmpty()) {
            return false;
        }
        return ai.getLegalAnalysis().stream().anyMatch(point -> {
            if (point.getPoint() != null && !point.getPoint().isBlank()) {
                return true;
            }
            return point.getCitation() != null
                    && point.getCitation().getSourceUrl() != null
                    && !point.getCitation().getSourceUrl().isBlank();
        });
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
}
