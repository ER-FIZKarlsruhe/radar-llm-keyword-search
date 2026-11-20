package org.radar_project.model;

import java.util.List;

/**
 * Response of {@code POST /api/keywords/match}.
 *
 * @param warnings searches that failed; the matches of these searches are missing
 */
public record MatchResponse(String language, List<MatchedKeyword> keywords, List<String> warnings) {
}
