package com.futesat.spaingeo.core;

import java.text.Normalizer;

/**
 * Normalizes text for accent-insensitive, case-insensitive searching.
 * Strips diacritics (á→a, ñ→n, ü→u, ç→c), lowercases, and removes
 * non-alphanumeric characters.
 */
public final class TextNormalizer {
    private TextNormalizer() {
    }

    /**
     * Normalize a string for search comparison.
     *
     * @param input the input string
     * @return normalized lowercase string with diacritics and special chars removed
     */
    public static String normalize(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        // NFD decomposition splits characters like é into e + combining accent
        String decomposed = Normalizer.normalize(input, Normalizer.Form.NFD);
        // Remove combining diacritical marks (Unicode category Mn)
        String stripped = decomposed.replaceAll("\\p{Mn}", "");
        // Lowercase
        String lower = stripped.toLowerCase();
        // Remove non-alphanumeric except spaces
        String cleaned = lower.replaceAll("[^a-z0-9 ]", "");
        // Collapse multiple spaces
        return cleaned.replaceAll("\\s+", " ").trim();
    }
}
