package org.radar_project.service;

import org.junit.jupiter.api.Test;
import org.radar_project.model.MatchType;
import org.radar_project.model.NormdataCandidate;
import org.radar_project.model.NormdataMatch;
import org.radar_project.model.NormdataSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MatchingServiceTest {

    private final MatchingService matchingService = new MatchingService();

    @Test
    void ranksExactBeforeAlternativeAndNormalizedMatches() {
        List<NormdataCandidate> candidates = List.of(
                candidate("1", "Keratosis-Pilaris", List.of()),
                candidate("2", "Lichen pilaris", List.of("Keratosis pilaris")),
                candidate("3", "Keratosis pilaris", List.of()),
                candidate("4", "Pityriasis", List.of("keratosis  pilaris")));

        List<NormdataMatch> matches = matchingService.match("Keratosis pilaris", candidates);

        assertThat(matches).extracting(NormdataMatch::id).containsExactly("3", "2", "1", "4");
        assertThat(matches).extracting(NormdataMatch::matchType).containsExactly(
                MatchType.EXACT, MatchType.ALTERNATIVE_LABEL,
                MatchType.NORMALIZED, MatchType.NORMALIZED_ALTERNATIVE_LABEL);
        assertThat(matches).extracting(NormdataMatch::score).containsExactly(1.0, 0.9, 0.8, 0.7);
    }

    @Test
    void dropsHitsThatDoNotCorrespondToTheTerm() {
        List<NormdataCandidate> candidates = List.of(
                candidate("1", "Pityriasis rubra pilaris", List.of("Stachelflechte")),
                candidate("2", null, List.of()));

        assertThat(matchingService.match("Keratosis pilaris", candidates)).isEmpty();
    }

    @Test
    void keepsAllHomonymsDistinguishedOnlyByQualifier() {
        List<NormdataCandidate> candidates = List.of(
                candidate("1", "Bank <Möbel>", List.of()),
                candidate("2", "Bank <Kreditinstitut>", List.of()));

        List<NormdataMatch> matches = matchingService.match("Bank", candidates);

        assertThat(matches).extracting(NormdataMatch::id).containsExactly("1", "2");
        assertThat(matches).allMatch(match -> match.matchType() == MatchType.NORMALIZED);
    }

    @Test
    void removesDuplicateUrisKeepingTheBestMatch() {
        List<NormdataCandidate> candidates = List.of(
                new NormdataCandidate(NormdataSource.TIB, "a", "http://x/1", "zeolite", List.of("Zeolith"), "chebi", List.of()),
                new NormdataCandidate(NormdataSource.TIB, "b", "http://x/1", "Zeolith", List.of(), "other", List.of()));

        List<NormdataMatch> matches = matchingService.match("Zeolith", candidates);

        assertThat(matches).singleElement().satisfies(match -> {
            assertThat(match.id()).isEqualTo("b");
            assertThat(match.matchType()).isEqualTo(MatchType.EXACT);
        });
    }

    private static NormdataCandidate candidate(String id, String label, List<String> alternativeLabels) {
        return new NormdataCandidate(NormdataSource.GND, id, "https://d-nb.info/gnd/" + id, label,
                alternativeLabels, null, List.of("SubjectHeading"));
    }
}
