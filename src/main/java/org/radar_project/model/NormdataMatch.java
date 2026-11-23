package org.radar_project.model;

import java.util.List;

/**
 * A normdata record matched to a keyword. All values except {@code matchedLabel}, {@code matchType}
 * and {@code score} are taken unchanged from the source.
 *
 * @param matchedLabel the label or alternative label of the record that matched the keyword
 * @param score        confidence computed by this service, not a value delivered by the source
 */
public record NormdataMatch(
        NormdataSource source,
        String id,
        String uri,
        String label,
        String matchedLabel,
        String vocabulary,
        List<String> types,
        MatchType matchType,
        double score) {
}
