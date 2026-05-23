package com.legally.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legally.model.JurisdictionContext;
import com.legally.model.LawChunk;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CorpusService {

    private static final Map<String, List<String>> SCENARIO_TAGS = Map.ofEntries(
            Map.entry("police_stop", List.of("police_stop", "search", "phone", "arrest", "fundamental_rights")),
            Map.entry("tenancy", List.of("tenancy", "eviction", "rent", "contract", "demand_letter")),
            Map.entry("land", List.of("land", "property", "registration", "fraud", "kwara_government")),
            Map.entry("employment", List.of("fundamental_rights", "contract", "legal_aid")),
            Map.entry("consumer", List.of("contract", "fraud", "fundamental_rights", "legal_aid")),
            Map.entry("family", List.of("fundamental_rights", "legal_aid", "tenancy")),
            Map.entry("debt", List.of("contract", "fundamental_rights", "legal_aid")),
            Map.entry("business_contract", List.of("contract", "demand_letter", "fraud", "legal_aid")),
            Map.entry("inheritance", List.of("land", "property", "fundamental_rights", "legal_aid"))
    );

    private List<LawChunk> allChunks = List.of();
    private final ObjectMapper objectMapper;

    public CorpusService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void load() throws Exception {
        List<LawChunk> loaded = new ArrayList<>();
        loaded.addAll(readChunks("corpus/corpus.json"));
        loaded.addAll(readChunks("corpus/international.json"));
        for (LawChunk chunk : loaded) {
            normalizeChunk(chunk);
        }
        this.allChunks = List.copyOf(loaded);
    }

    private List<LawChunk> readChunks(String path) throws Exception {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return objectMapper.readValue(in, new TypeReference<List<LawChunk>>() {});
        }
    }

    private void normalizeChunk(LawChunk chunk) {
        if (chunk.getCountryCode() == null || chunk.getCountryCode().isBlank()) {
            chunk.setCountryCode("NG");
        }
        if (chunk.getRegionCode() == null || chunk.getRegionCode().isBlank()) {
            String j = chunk.getJurisdiction();
            if (j != null && !j.isBlank()) {
                chunk.setRegionCode(j.toUpperCase(Locale.ROOT));
            } else {
                chunk.setRegionCode("FEDERAL");
            }
        }
        if (chunk.getJurisdiction() == null || chunk.getJurisdiction().isBlank()) {
            chunk.setJurisdiction(chunk.getRegionCode());
        }
    }

    public List<LawChunk> retrieve(JurisdictionContext jurisdiction, String scenario, String userMessage, int limit) {
        List<LawChunk> pool = filterByJurisdiction(jurisdiction);
        return scoreAndRank(pool, scenario, userMessage, limit);
    }

    private List<LawChunk> filterByJurisdiction(JurisdictionContext jurisdiction) {
        String country = jurisdiction.getCountryCode() != null
                ? jurisdiction.getCountryCode().toUpperCase(Locale.ROOT) : "INT";
        String region = jurisdiction.getRegionCode() != null
                ? jurisdiction.getRegionCode().toUpperCase(Locale.ROOT) : "GENERAL";

        List<LawChunk> countryChunks = allChunks.stream()
                .filter(c -> country.equalsIgnoreCase(c.getCountryCode()))
                .toList();

        if (!countryChunks.isEmpty()) {
            List<LawChunk> regional = countryChunks.stream()
                    .filter(c -> region.equalsIgnoreCase(c.getRegionCode())
                            || "FEDERAL".equalsIgnoreCase(c.getRegionCode())
                            || "GENERAL".equalsIgnoreCase(c.getRegionCode()))
                    .toList();
            if (!regional.isEmpty()) {
                return regional;
            }
            return countryChunks;
        }

        return allChunks.stream()
                .filter(c -> "INT".equalsIgnoreCase(c.getCountryCode()))
                .collect(Collectors.toList());
    }

    private List<LawChunk> scoreAndRank(List<LawChunk> chunks, String scenario, String userMessage, int limit) {
        Set<String> activeTags = new LinkedHashSet<>();
        if (scenario != null && SCENARIO_TAGS.containsKey(scenario)) {
            activeTags.addAll(SCENARIO_TAGS.get(scenario));
        }

        String lower = userMessage == null ? "" : userMessage.toLowerCase(Locale.ROOT);
        enrichTagsFromMessage(lower, activeTags);

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

    private void enrichTagsFromMessage(String lower, Set<String> activeTags) {
        if (lower.contains("phone") || lower.contains("search") || lower.contains("police")) {
            activeTags.addAll(List.of("police_stop", "phone", "search"));
        }
        if (lower.contains("rent") || lower.contains("tenant") || lower.contains("evict") || lower.contains("lease")) {
            activeTags.addAll(List.of("tenancy", "eviction", "rent"));
        }
        if (lower.contains("land") || lower.contains("deed") || lower.contains("survey") || lower.contains("purchase")) {
            activeTags.addAll(List.of("land", "fraud", "registration"));
        }
        if (lower.contains("employ") || lower.contains("salary") || lower.contains("fired") || lower.contains("workplace")) {
            activeTags.addAll(List.of("contract", "fundamental_rights", "legal_aid"));
        }
        if (lower.contains("consumer") || lower.contains("refund") || lower.contains("warranty") || lower.contains("scam")) {
            activeTags.addAll(List.of("contract", "fraud", "legal_aid"));
        }
        if (lower.contains("marriage") || lower.contains("divorce") || lower.contains("custody") || lower.contains("domestic")) {
            activeTags.addAll(List.of("fundamental_rights", "legal_aid"));
        }
        if (lower.contains("debt") || lower.contains("loan") || lower.contains("lender")) {
            activeTags.addAll(List.of("contract", "legal_aid"));
        }
        if (lower.contains("inherit") || lower.contains("will") || lower.contains("estate")) {
            activeTags.addAll(List.of("land", "property", "legal_aid"));
        }
        if (lower.contains("contract") || lower.contains("breach") || lower.contains("vendor")) {
            activeTags.addAll(List.of("contract", "demand_letter"));
        }
    }

    public Optional<LawChunk> findById(String id) {
        return allChunks.stream().filter(c -> c.getId().equals(id)).findFirst();
    }

    public Set<String> validChunkIds(Collection<String> ids) {
        Set<String> all = allChunks.stream().map(LawChunk::getId).collect(Collectors.toSet());
        return ids.stream().filter(all::contains).collect(Collectors.toSet());
    }

    private List<String> tokenize(String text) {
        return Arrays.stream(text.split("\\W+"))
                .filter(w -> w.length() > 3)
                .toList();
    }

    private record ScoredChunk(LawChunk chunk, int score) {}
}
