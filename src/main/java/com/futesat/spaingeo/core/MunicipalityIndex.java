package com.futesat.spaingeo.core;

import com.futesat.spaingeo.model.ReverseGeocodeResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory index for fast municipality name searching.
 * All lookups are accent-insensitive and case-insensitive.
 */
public final class MunicipalityIndex {
    private final List<ReverseGeocodeResult> allResults;
    private final Map<String, List<ReverseGeocodeResult>> byNormalizedName;

    public MunicipalityIndex(List<MunicipalityFeature> features) {
        this.allResults = new ArrayList<>(features.size());
        this.byNormalizedName = new HashMap<>();

        for (MunicipalityFeature feature : features) {
            ReverseGeocodeResult result = feature.result();
            allResults.add(result);

            String normalized = TextNormalizer.normalize(result.municipality().name());
            byNormalizedName.computeIfAbsent(normalized, k -> new ArrayList<>()).add(result);
        }
    }

    /**
     * Search by exact municipality name (accent/case insensitive).
     */
    public List<ReverseGeocodeResult> searchByName(String name) {
        String normalized = TextNormalizer.normalize(name);
        List<ReverseGeocodeResult> results = byNormalizedName.get(normalized);
        return results != null ? List.copyOf(results) : List.of();
    }

    /**
     * Search by partial municipality name (accent/case insensitive, substring match).
     */
    public List<ReverseGeocodeResult> searchByNameContains(String query) {
        String normalizedQuery = TextNormalizer.normalize(query);
        if (normalizedQuery.isEmpty()) {
            return List.of();
        }
        List<ReverseGeocodeResult> matches = new ArrayList<>();
        for (var entry : byNormalizedName.entrySet()) {
            if (entry.getKey().contains(normalizedQuery)) {
                matches.addAll(entry.getValue());
            }
        }
        return List.copyOf(matches);
    }

    /**
     * Search by province (name or id) and municipality name.
     * Province matching is also accent/case insensitive.
     */
    public List<ReverseGeocodeResult> searchByProvinceAndName(String province, String municipality) {
        String normalizedProvince = TextNormalizer.normalize(province);
        String normalizedMunicipality = TextNormalizer.normalize(municipality);

        List<ReverseGeocodeResult> matches = new ArrayList<>();
        for (ReverseGeocodeResult result : allResults) {
            boolean provinceMatch = result.province().id().equals(province)
                    || TextNormalizer.normalize(result.province().name()).contains(normalizedProvince);

            if (!provinceMatch) {
                continue;
            }

            String normalizedName = TextNormalizer.normalize(result.municipality().name());
            if (normalizedName.contains(normalizedMunicipality)) {
                matches.add(result);
            }
        }
        return List.copyOf(matches);
    }

    /**
     * Returns the total number of indexed municipalities.
     */
    public int size() {
        return allResults.size();
    }
}
