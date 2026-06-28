package com.legally.llm;

import com.legally.config.LlmChainQualifiers;
import com.legally.model.JurisdictionContext;
import com.legally.model.LawChunk;
import com.legally.model.dto.ConsultRequest;
import com.legally.model.dto.GeminiLegalResponse;
import com.legally.model.dto.LegalResearchResult;
import com.legally.service.GeminiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Tries each configured legal LLM provider in order until one returns substantive content.
 */
@Service
public class MultiLlmLegalResearchService {

    private static final Logger log = LoggerFactory.getLogger(MultiLlmLegalResearchService.class);

    private final List<LegalLlmProvider> providers;
    private final GeminiService geminiService;
    private final LegalMediaDigestService legalMediaDigestService;
    private final GoogleSpeechToTextService googleSpeechToTextService;

    public MultiLlmLegalResearchService(
            @Qualifier(LlmChainQualifiers.LEGAL) List<LegalLlmProvider> providers,
            GeminiService geminiService,
            LegalMediaDigestService legalMediaDigestService,
            GoogleSpeechToTextService googleSpeechToTextService) {
        this.providers = providers;
        this.geminiService = geminiService;
        this.legalMediaDigestService = legalMediaDigestService;
        this.googleSpeechToTextService = googleSpeechToTextService;
    }

    /** Runs legal research across configured LLM providers. */
    public LegalResearchResult research(
            String messageText,
            String scenario,
            JurisdictionContext jurisdiction,
            List<ConsultRequest.MediaRef> media) throws Exception {

        boolean hasMedia = media != null && !media.isEmpty();
        Optional<String> mediaDigest = Optional.empty();
        Optional<String> speechTranscript = Optional.empty();
        if (hasMedia) {
            speechTranscript = googleSpeechToTextService.transcribeAudio(media);
            speechTranscript.ifPresent(t ->
                    log.info("Speech-to-Text transcript ready ({} chars)", t.length()));
            log.info("Building media digest for {} attachment(s) (voice/video/uploads)", media.size());
            mediaDigest = legalMediaDigestService.buildDigest(messageText, scenario, jurisdiction, media);
            if (mediaDigest.isEmpty()) {
                log.warn(
                        "Media digest unavailable; text-only providers may lack full evidence context "
                                + "(check GEMINI_API_KEY and quota)");
            }
        }

        for (LegalLlmProvider provider : providers) {
            if (!provider.isConfigured()) {
                log.warn("Provider {} skipped (no API key or model in backend/.env)", provider.id());
                continue;
            }

            String messageWithTranscript = appendTranscript(messageText, speechTranscript.orElse(null));
            String effectiveMessage = messageWithTranscript;
            List<ConsultRequest.MediaRef> effectiveMedia = media;
            if (!provider.supportsNativeMultimodal()) {
                effectiveMessage = LegalPrompts.analyzeUserMessageWithMediaDigest(
                        messageWithTranscript, scenario, jurisdiction, mediaDigest.orElse(null));
                effectiveMedia = Collections.emptyList();
            }

            log.info("Trying legal analysis provider: {}", provider.id());
            try {
                var outcome = provider.analyze(effectiveMessage, scenario, jurisdiction, effectiveMedia);
                if (outcome.isPresent() && LlmResponseParser.hasSubstantiveLegalContent(outcome.get().response())) {
                    log.info("Legal analysis succeeded via provider {}", provider.id());
                    GeminiLegalResponse response = outcome.get().response();
                    List<LawChunk> sources = outcome.get().sources();
                    return new LegalResearchResult(response, sources, true);
                }
                log.warn("Provider {} returned empty or non-substantive result", provider.id());
            } catch (Exception e) {
                log.warn("Provider {} failed: {}", provider.id(), e.getMessage());
            }
        }

        log.info("All LLM providers exhausted for {} — returning no-information response", jurisdiction.displayLabel());
        if (hasMedia && mediaDigest.isEmpty() && speechTranscript.isEmpty()) {
            GeminiLegalResponse mediaFailed =
                    geminiService.buildMediaProcessingFailedResponse(jurisdiction);
            return new LegalResearchResult(mediaFailed, List.of(), false, false);
        }
        String noInfoMessage = hasMedia
                ? LegalPrompts.analyzeUserMessageWithMediaDigest(
                        appendTranscript(messageText, speechTranscript.orElse(null)),
                        scenario,
                        jurisdiction,
                        mediaDigest.orElse(null))
                : messageText;
        GeminiLegalResponse noInfo = geminiService.buildNoInformationResponse(noInfoMessage, scenario, jurisdiction);
        return new LegalResearchResult(noInfo, List.of(), false, true);
    }

    private String appendTranscript(String messageText, String transcript) {
        String msg = messageText != null ? messageText.trim() : "";
        if (transcript == null || transcript.isBlank()) {
            return msg;
        }
        if (msg.isBlank()) {
            return "User media transcript:\n" + transcript;
        }
        return msg + "\n\nUser media transcript:\n" + transcript;
    }
}
