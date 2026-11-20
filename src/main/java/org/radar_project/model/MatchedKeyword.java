package org.radar_project.model;

import java.util.List;

/**
 * A keyword together with its normdata matches, best match first.
 */
public record MatchedKeyword(String term, String type, List<NormdataMatch> matches) {
}
