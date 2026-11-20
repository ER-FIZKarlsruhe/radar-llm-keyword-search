package org.radar_project.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * A keyword candidate as produced by the extraction model.
 *
 * @param term the keyword
 * @param type "direkt" if the term occurs in the text, "erschlossen" if it was inferred
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Keyword(@NotBlank @Size(max = 500) String term, String type) {
}
