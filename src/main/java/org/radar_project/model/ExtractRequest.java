package org.radar_project.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request of {@code POST /api/keywords/extract}.
 *
 * @param text the text to extract keywords from
 */
public record ExtractRequest(@NotBlank @Size(max = 100_000) String text) {
}
