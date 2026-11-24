package org.radar_project.service;

import org.junit.jupiter.api.Test;
import org.radar_project.config.MatchingProperties;
import org.radar_project.model.NormdataCandidate;
import org.radar_project.model.NormdataMatch;
import org.radar_project.model.NormdataSource;
import org.springframework.web.client.ResourceAccessException;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KeywordMatchServiceTest {

    private static final Set<NormdataSource> ALL = EnumSet.allOf(NormdataSource.class);

    private final GndService gndService = mock(GndService.class);
    private final TibService tibService = mock(TibService.class);
    private final KeywordMatchService service = new KeywordMatchService(gndService, tibService,
            new MatchingService(), new MatchingProperties(2));

    @Test
    void returnsBestMatchPerKeywordInKeywordOrderAndNullWithoutMatch() {
        when(gndService.search("Zeolith")).thenReturn(List.of(gnd("Zeolith")));
        when(tibService.search("Zeolith")).thenReturn(List.of(tib("zeolite", List.of("Zeolith"))));
        when(gndService.search("Katalyse")).thenReturn(List.of(gnd("Katalysator")));
        when(tibService.search("Katalyse")).thenReturn(List.of());

        Map<String, NormdataMatch> matches = service.bestMatches(List.of("Zeolith", "Katalyse"), ALL, NormdataSource.TIB);

        assertThat(matches).containsOnlyKeys("Zeolith", "Katalyse");
        assertThat(matches.keySet()).containsExactly("Zeolith", "Katalyse");
        // GND matches exactly (score 1.0), TIB only via synonym (0.9): the higher score wins over the preference.
        assertThat(matches.get("Zeolith").source()).isEqualTo(NormdataSource.GND);
        assertThat(matches.get("Katalyse")).isNull();
    }

    @Test
    void prefersGivenSourceOnEqualScore() {
        when(gndService.search("Zeolith")).thenReturn(List.of(gnd("Zeolith")));
        when(tibService.search("Zeolith")).thenReturn(List.of(tib("Zeolith", List.of())));

        assertThat(service.bestMatches(List.of("Zeolith"), ALL, NormdataSource.GND).get("Zeolith").source())
                .isEqualTo(NormdataSource.GND);
        assertThat(service.bestMatches(List.of("Zeolith"), ALL, NormdataSource.TIB).get("Zeolith").source())
                .isEqualTo(NormdataSource.TIB);
    }

    @Test
    void searchesOnlyRequestedSources() {
        when(gndService.search("Zeolith")).thenReturn(List.of(gnd("Zeolith")));

        Map<String, NormdataMatch> matches = service.bestMatches(List.of("Zeolith"), Set.of(NormdataSource.GND),
                NormdataSource.TIB);

        assertThat(matches.get("Zeolith").source()).isEqualTo(NormdataSource.GND);
        verify(tibService, never()).search(anyString());
    }

    @Test
    void keepsMatchOfOtherSourceWhenOneSourceFails() {
        when(gndService.search("Zeolith")).thenReturn(List.of(gnd("Zeolith")));
        when(tibService.search("Zeolith")).thenThrow(new ResourceAccessException("timeout"));

        assertThat(service.bestMatches(List.of("Zeolith"), ALL, NormdataSource.TIB).get("Zeolith").source())
                .isEqualTo(NormdataSource.GND);
    }

    @Test
    void returnsNullWhenAllSourcesFail() {
        when(gndService.search(anyString())).thenThrow(new ResourceAccessException("timeout"));
        when(tibService.search(anyString())).thenThrow(new ResourceAccessException("timeout"));

        Map<String, NormdataMatch> matches = service.bestMatches(List.of("Zeolith"), ALL, NormdataSource.TIB);

        assertThat(matches).containsEntry("Zeolith", null);
    }

    @Test
    void searchesDuplicateKeywordsOnce() {
        when(gndService.search("Zeolith")).thenReturn(List.of(gnd("Zeolith")));

        Map<String, NormdataMatch> matches = service.bestMatches(List.of("Zeolith", "Zeolith"),
                Set.of(NormdataSource.GND), NormdataSource.TIB);

        assertThat(matches).containsOnlyKeys("Zeolith");
        verify(gndService, times(1)).search("Zeolith");
    }

    @Test
    void mergesKeywordsWithSameNormalizedFormKeepingFirstVariant() {
        when(gndService.search("Cyclic Voltammetry")).thenReturn(List.of(gnd("Cyclovoltammetrie")));
        when(gndService.search("Friedel-Crafts-Acylierung")).thenReturn(List.of(gnd("Friedel-Crafts-Acylierung")));

        Map<String, NormdataMatch> matches = service.bestMatches(List.of("Cyclic Voltammetry", "Friedel-Crafts-Acylierung",
                "cyclic voltammetry", "Friedel Crafts Acylierung"), Set.of(NormdataSource.GND), NormdataSource.TIB);

        assertThat(matches.keySet()).containsExactly("Cyclic Voltammetry", "Friedel-Crafts-Acylierung");
        assertThat(matches.get("Friedel-Crafts-Acylierung").label()).isEqualTo("Friedel-Crafts-Acylierung");
        verify(gndService, times(1)).search("Cyclic Voltammetry");
        verify(gndService, never()).search("cyclic voltammetry");
        verify(gndService, never()).search("Friedel Crafts Acylierung");
    }

    @Test
    void keepsKeywordsWithDifferentNormalizedForm() {
        Map<String, NormdataMatch> matches = service.bestMatches(List.of("Kulturdenkmal", "Kulturdenkmale"),
                Set.of(NormdataSource.GND), NormdataSource.TIB);

        assertThat(matches.keySet()).containsExactly("Kulturdenkmal", "Kulturdenkmale");
        verify(gndService).search("Kulturdenkmal");
        verify(gndService).search("Kulturdenkmale");
    }

    @Test
    void releasesSearchSlotsAfterFailures() {
        when(gndService.search(anyString())).thenThrow(new ResourceAccessException("timeout"));
        when(tibService.search(anyString())).thenThrow(new ResourceAccessException("timeout"));

        service.bestMatches(List.of("A", "B"), ALL, NormdataSource.TIB);
        // With leaked slots (2 configured, 4 searches) this second call would block forever.
        Map<String, NormdataMatch> second = service.bestMatches(List.of("A", "B"), ALL, NormdataSource.TIB);

        assertThat(second).containsEntry("A", null).containsEntry("B", null);
    }

    private static NormdataCandidate gnd(String label) {
        return new NormdataCandidate(NormdataSource.GND, label, "https://d-nb.info/gnd/" + label, label,
                List.of(), null, List.of("SubjectHeading"));
    }

    private static NormdataCandidate tib(String label, List<String> synonyms) {
        return new NormdataCandidate(NormdataSource.TIB, "CHEBI_48729", "http://purl.obolibrary.org/obo/CHEBI_48729",
                label, synonyms, "chebi", List.of("class"));
    }
}
