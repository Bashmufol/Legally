package com.legally.llm;

import com.legally.model.LawChunk;
import com.legally.model.dto.GeminiLegalResponse;

import java.util.List;

public record LlmAnalysisOutcome(GeminiLegalResponse response, List<LawChunk> sources, String providerId) {}
