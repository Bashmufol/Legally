package com.legally.llm;

import com.legally.model.LawChunk;
import com.legally.model.dto.GeminiLegalResponse;

import java.util.List;

/**
 * Parsed legal response plus source chunks from one provider.
 */
public record LlmAnalysisOutcome(GeminiLegalResponse response, List<LawChunk> sources, String providerId) {}
