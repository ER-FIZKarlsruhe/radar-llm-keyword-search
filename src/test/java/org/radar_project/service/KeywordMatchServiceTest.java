package org.radar_project.service;

import org.junit.jupiter.api.Test;
import org.radar_project.config.MatchingProperties;
import org.radar_project.model.Keyword;
import org.radar_project.model.MatchRequest;
import org.radar_project.model.MatchResponse;
import org.radar_project.model.MatchedKeyword;
import org.radar_project.model.NormdataCandidate;
import org.radar_project.model.NormdataMatch;
import org.radar_project.model.NormdataSource;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KeywordMatchServiceTest {

    private final GndService gndService = mock(GndService.class);
    private final TibService tibService = mock(TibService.class);
    private final KeywordMatchService service = new KeywordMatchService(gndService, tibService,
            new MatchingService(), new MatchingProperties(2));

    @Test
    void searchesAllSourcesByDefaultAndKeepsKeywordOrder() {
        when(gndService.search("Zeolith")).thenReturn(List.of(gnd("Zeolith")));
        when(gndService.search("Katalyse")).thenReturn(List.of(gnd("Katalyse")));
        when(tibService.search("Zeolith")).thenReturn(List.of(
                new NormdataCandidate(NormdataSource.TIB, "CHEBI_48729", "http://chebi/48729", "zeolite",
                        List.of("Zeolith"), "chebi", List.of("class"))));
        when(tibService.search("Katalyse")).thenReturn(List.of());

        MatchResponse response = service.match(new MatchRequest("de",
                List.of(new Keyword("Zeolith", "direkt"), new Keyword("Katalyse", "erschlossen")), null));

        assertThat(response.language()).isEqualTo("de");
        assertThat(response.warnings()).isEmpty();
        assertThat(response.keywords()).extracting(MatchedKeyword::term).containsExactly("Zeolith", "Katalyse");
        assertThat(response.keywords().get(0).matches()).extracting(NormdataMatch::source)
                .containsExactly(NormdataSource.GND, NormdataSource.TIB);
        assertThat(response.keywords().get(1).type()).isEqualTo("erschlossen");
    }

    @Test
    void searchesOnlyRequestedSources() {
        when(gndService.search("Zeolith")).thenReturn(List.of(gnd("Zeolith")));

        MatchResponse response = service.match(new MatchRequest(null,
                List.of(new Keyword("Zeolith", "direkt")), Set.of(NormdataSource.GND)));

        assertThat(response.keywords().get(0).matches()).hasSize(1);
        verify(tibService, never()).search(anyString());
    }

    @Test
    void reportsFailedSourceAsWarningAndKeepsOtherMatches() {
        when(gndService.search("Zeolith")).thenReturn(List.of(gnd("Zeolith")));
        when(tibService.search("Zeolith")).thenThrow(new ResourceAccessException("timeout"));

        MatchResponse response = service.match(new MatchRequest(null, List.of(new Keyword("Zeolith", "direkt")), null));

        assertThat(response.keywords().get(0).matches()).extracting(NormdataMatch::source)
                .containsExactly(NormdataSource.GND);
        assertThat(response.warnings()).containsExactly("tib search failed for term 'Zeolith'");
    }

    @Test
    void releasesSearchSlotsAfterFailures() {
        when(gndService.search(anyString())).thenThrow(new ResourceAccessException("timeout"));
        when(tibService.search(anyString())).thenThrow(new ResourceAccessException("timeout"));
        MatchRequest request = new MatchRequest(null, List.of(new Keyword("A", null), new Keyword("B", null)), null);

        service.match(request);
        MatchResponse second = service.match(request);

        assertThat(second.warnings()).hasSize(4);
    }

    private static NormdataCandidate gnd(String label) {
        return new NormdataCandidate(NormdataSource.GND, label, "https://d-nb.info/gnd/" + label, label,
                List.of(), null, List.of("SubjectHeading"));
    }
}
