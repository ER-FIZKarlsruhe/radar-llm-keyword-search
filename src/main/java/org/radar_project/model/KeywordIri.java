package org.radar_project.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Best normdata match of one keyword in the response of {@code POST /api/keywords/extract}. Same fields as
 * radar-keyword-search's {@code /extract-iris} response, without {@code distance} and {@code matched}
 * (this service only returns actual matches).
 *
 * @param iri                URI of the record
 * @param label              preferred label of the record
 * @param bestTerm           the label or synonym/variant name that matched the keyword
 * @param ontologyName       TIB ontology, null for GND
 * @param keywordScheme      RADAR {@code keywordScheme}: "GND" or "Other"
 * @param schemeUri          RADAR {@code schemeURI}
 * @param classificationCode RADAR {@code classificationCode}
 */
public record KeywordIri(
        String iri,
        String label,
        @JsonProperty("best_term") String bestTerm,
        @JsonProperty("ontology_name") String ontologyName,
        NormdataSource source,
        @JsonProperty("keyword_scheme") String keywordScheme,
        @JsonProperty("scheme_uri") String schemeUri,
        @JsonProperty("classification_code") String classificationCode) {
}
