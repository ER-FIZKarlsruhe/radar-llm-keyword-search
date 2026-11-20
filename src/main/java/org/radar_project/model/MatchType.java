package org.radar_project.model;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * How a keyword matched a normdata record, ordered by decreasing confidence.
 */
public enum MatchType {
    EXACT("exact", 1.0),
    ALTERNATIVE_LABEL("alternativeLabel", 0.9),
    NORMALIZED("normalized", 0.8),
    NORMALIZED_ALTERNATIVE_LABEL("normalizedAlternativeLabel", 0.7);

    private final String value;
    private final double score;

    MatchType(String value, double score) {
        this.value = value;
        this.score = score;
    }

    @JsonValue
    public String value() {
        return value;
    }

    public double score() {
        return score;
    }
}
