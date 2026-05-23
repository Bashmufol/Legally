package com.legally.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legally.model.LawChunk;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CorpusService {

    private static final Map<String, List<String>> SCENARIO_TAGS = Map.of(
            "police_stop", List.of("police_stop", "search", "phone", "arrest", "fundamental_rights"),
            "tenancy", List.of("tenancy", "eviction", "rent", "contract", "demand_letter"),
            "land", List.of("land", "property", "registration", "fraud", "kwara_government")
    );

    private List<LawChunk> chunks = List.of();
    private final ObjectMapper objectMapper;

    public CorpusService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void load() throws Exception {
        try (InputStream in = new ClassPathResource("corpus/corpus.json").getInputStream()) {
            this.chunks = objectMapper.readValue(in, new TypeReference<List<LawChunk>>() {});
        }
    }

    public List<LawChunk> retrieve(String scenario, String userMessage, int limit) {
        Set<String> activeTags = new LinkedHashSet<>();
        if (scenario != null && SCENARIO_TAGS.containsKey(scenario)) {
            activeTags.addAll(SCENARIO_TAGS.get(scenario));
        }

        String lower = userMessage == null ? "" : userMessage.toLowerCase(Locale.ROOT);
        if (lower.contains("phone") || lower.contains("search") || lower.contains("police")) {
            activeTags.addAll(List.of("police_stop", "phone", "search"));
        }
        if (lower.contains("rent") || lower.contains("tenant") || lower.contains("evict") || lower.contains("lease")) {
            activeTags.addAll(List.of("tenancy", "eviction", "rent"));
        }
        if (lower.contains("land") || lower.contains("deed") || lower.contains("survey") || lower.contains("purchase")) {
            activeTags.addAll(List.of("land", "fraud", "registration"));
        }

        List<ScoredChunk> scored = new ArrayList<>();
        for (LawChunk chunk : chunks) {
            int score = 0;
            if (chunk.getTags() != null) {
                for (String tag : chunk.getTags()) {
                    if (activeTags.contains(tag)) {
                        score += 3;
                    }
                }
            }
            if (chunk.getText() != null) {
                for (String word : tokenize(lower)) {
                    if (word.length() > 4 && chunk.getText().toLowerCase(Locale.ROOT).contains(word)) {
                        score += 1;
                    }
                }
            }
            if (score > 0) {
                scored.add(new ScoredChunk(chunk, score));
            }
        }

        if (scored.isEmpty()) {
            return chunks.stream().limit(limit).collect(Collectors.toList());
        }

        return scored.stream()
                .sorted(Comparator.comparingInt(ScoredChunk::score).reversed())
                .limit(limit)
                .map(ScoredChunk::chunk)
                .collect(Collectors.toList());
    }

    public Optional<LawChunk> findById(String id) {
        return chunks.stream().filter(c -> c.getId().equals(id)).findFirst();
    }

    public Set<String> validChunkIds(Collection<String> ids) {
        Set<String> all = chunks.stream().map(LawChunk::getId).collect(Collectors.toSet());
        return ids.stream().filter(all::contains).collect(Collectors.toSet());
    }

    private List<String> tokenize(String text) {
        return Arrays.stream(text.split("\\W+"))
                .filter(w -> w.length() > 3)
                .toList();
    }

    private record ScoredChunk(LawChunk chunk, int score) {}
}
