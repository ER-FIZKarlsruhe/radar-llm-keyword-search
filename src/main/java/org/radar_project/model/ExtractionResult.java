package org.radar_project.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Keywords extracted from a text, also the response of {@code POST /api/keywords/extract}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExtractionResult(String language, List<String> keywords) {

    public ExtractionResult {
        keywords = keywords == null ? List.of() : List.copyOf(keywords);
    }
}
