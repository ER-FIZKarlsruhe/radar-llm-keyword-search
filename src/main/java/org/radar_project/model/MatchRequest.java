package org.radar_project.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Set;

/**
 * Request of {@code POST /api/keywords/match}. Accepts the response of {@code /api/keywords/extract} as is.
 *
 * @param language optional language of the keywords, passed through to the response
 * @param keywords the keywords to look up
 * @param sources  optional sources to search; all sources if empty
 */
public record MatchRequest(
        String language,
        @NotEmpty @Size(max = 50) List<@NotNull @Valid Keyword> keywords,
        Set<NormdataSource> sources) {

    public MatchRequest {
        sources = sources == null ? Set.of() : Set.copyOf(sources);
    }
}
