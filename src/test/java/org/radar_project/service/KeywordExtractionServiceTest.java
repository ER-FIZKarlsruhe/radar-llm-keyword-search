package org.radar_project.service;

import org.junit.jupiter.api.Test;
import org.radar_project.model.ExtractRequest;
import org.radar_project.model.ExtractionResult;
import org.radar_project.model.KeywordIri;
import org.radar_project.model.MatchType;
import org.radar_project.model.NormdataMatch;
import org.radar_project.model.NormdataSource;
import org.radar_project.model.Terminology;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class KeywordExtractionServiceTest {

    private final OllamaService ollamaService = mock(OllamaService.class);
    private final KeywordMatchService keywordMatchService = mock(KeywordMatchService.class);
    private final KeywordExtractionService service = new KeywordExtractionService(ollamaService, keywordMatchService);

    @Test
    void returnsKeywordsWithoutLookupForTerminologyNone() {
        when(ollamaService.extract("Text")).thenReturn(new ExtractionResult("de", List.of("Zeolith", "Katalyse")));

        Map<String, KeywordIri> result = service.extract(new ExtractRequest("Text", Terminology.NONE));

        assertThat(result.keySet()).containsExactly("Zeolith", "Katalyse");
        assertThat(result.values()).containsOnlyNulls();
        verifyNoInteractions(keywordMatchService);
    }

    @Test
    void defaultsToTerminologyNone() {
        assertThat(new ExtractRequest("Text", null).terminology()).isEqualTo(Terminology.NONE);
    }

    @Test
    void mapsGndMatchAndKeepsKeywordOrderAndNulls() {
        when(ollamaService.extract("Text")).thenReturn(new ExtractionResult("de", List.of("Katalyse", "Zeolith")));
        Map<String, NormdataMatch> matches = new HashMap<>();
        matches.put("Zeolith", new NormdataMatch(NormdataSource.GND, "4190779-6", "https://d-nb.info/gnd/4190779-6",
                "Zeolith", "Zeolithe", null, List.of("SubjectHeading"), MatchType.ALTERNATIVE_LABEL, 0.9));
        matches.put("Katalyse", null);
        when(keywordMatchService.bestMatches(List.of("Katalyse", "Zeolith"), Set.of(NormdataSource.GND),
                NormdataSource.GND)).thenReturn(matches);

        Map<String, KeywordIri> result = service.extract(new ExtractRequest("Text", Terminology.GND));

        assertThat(result.keySet()).containsExactly("Katalyse", "Zeolith");
        assertThat(result.get("Katalyse")).isNull();
        assertThat(result.get("Zeolith")).isEqualTo(new KeywordIri("https://d-nb.info/gnd/4190779-6", "Zeolith",
                "Zeolithe", null, NormdataSource.GND, "GND", "https://d-nb.info/gnd/", "4190779-6"));
    }

    @Test
    void mapsTibMatchAndPrefersTibOnTieForNonGermanText() {
        when(ollamaService.extract("Text")).thenReturn(new ExtractionResult("en", List.of("zeolite")));
        when(keywordMatchService.bestMatches(List.of("zeolite"), EnumSet.allOf(NormdataSource.class), NormdataSource.TIB))
                .thenReturn(Map.of("zeolite", new NormdataMatch(NormdataSource.TIB, "CHEBI_48729",
                        "http://purl.obolibrary.org/obo/CHEBI_48729", "zeolite", "zeolite", "chebi", List.of("class"),
                        MatchType.EXACT, 1.0)));

        Map<String, KeywordIri> result = service.extract(new ExtractRequest("Text", Terminology.BOTH));

        assertThat(result.get("zeolite")).isEqualTo(new KeywordIri("http://purl.obolibrary.org/obo/CHEBI_48729",
                "zeolite", "zeolite", "chebi", NormdataSource.TIB, "Other", "http://purl.obolibrary.org/obo/",
                "CHEBI_48729"));
    }

    @Test
    void prefersGndOnTieForGermanText() {
        when(ollamaService.extract("Text")).thenReturn(new ExtractionResult("de", List.of("Zeolith")));
        when(keywordMatchService.bestMatches(anyList(), anySet(), any())).thenReturn(Map.of());

        service.extract(new ExtractRequest("Text", Terminology.BOTH));

        verify(keywordMatchService).bestMatches(List.of("Zeolith"), EnumSet.allOf(NormdataSource.class),
                NormdataSource.GND);
    }

    @Test
    void searchesOnlyTibForTerminologyTib() {
        when(ollamaService.extract("Text")).thenReturn(new ExtractionResult("de", List.of("Zeolith")));
        when(keywordMatchService.bestMatches(anyList(), anySet(), any())).thenReturn(Map.of());

        Map<String, KeywordIri> result = service.extract(new ExtractRequest("Text", Terminology.TIB));

        verify(keywordMatchService).bestMatches(List.of("Zeolith"), Set.of(NormdataSource.TIB), NormdataSource.GND);
        assertThat(result).containsEntry("Zeolith", null);
    }

    @Test
    void usesLastIriSegmentWhenTibShortFormIsMissing() {
        when(ollamaService.extract("Text")).thenReturn(new ExtractionResult("en", List.of("zeolite")));
        when(keywordMatchService.bestMatches(anyList(), anySet(), any()))
                .thenReturn(Map.of("zeolite", new NormdataMatch(NormdataSource.TIB, null,
                        "http://purl.obolibrary.org/obo/CHEBI_48729", "zeolite", "zeolite", "chebi", List.of(),
                        MatchType.EXACT, 1.0)));

        KeywordIri iri = service.extract(new ExtractRequest("Text", Terminology.TIB)).get("zeolite");

        assertThat(iri.classificationCode()).isEqualTo("CHEBI_48729");
        assertThat(iri.schemeUri()).isEqualTo("http://purl.obolibrary.org/obo/");
    }
}
