package com.collabsphere_first.explore.search;

import com.collabsphere_first.explore.config.ExploreProperties;
import com.collabsphere_first.explore.exception.InvalidExploreQueryException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.text.Normalizer;

@Component
@RequiredArgsConstructor
public class ExploreQueryValidator {

    private final ExploreProperties exploreProperties;

    /**
     * Validates and normalizes the search query string.
     *
     * @param query raw input query
     * @return trimmed, NFC-normalized query
     * @throws InvalidExploreQueryException on invalid input
     */
    public String validateAndNormalize(String query) {
        if (query == null) {
            throw new InvalidExploreQueryException("Search query cannot be null or empty");
        }

        // Trim leading and trailing whitespace
        String trimmed = query.trim();
        if (trimmed.isEmpty()) {
            throw new InvalidExploreQueryException("Search query cannot be empty");
        }

        // Unicode normalization (NFC)
        String normalized = Normalizer.normalize(trimmed, Normalizer.Form.NFC);

        int minLength = exploreProperties.getSearch().getMinQueryLength();
        int maxLength = exploreProperties.getSearch().getMaxQueryLength();

        if (normalized.length() < minLength) {
            throw new InvalidExploreQueryException("Search query must be at least " + minLength + " characters long");
        }

        if (normalized.length() > maxLength) {
            throw new InvalidExploreQueryException("Search query cannot exceed " + maxLength + " characters");
        }

        return normalized;
    }
}
