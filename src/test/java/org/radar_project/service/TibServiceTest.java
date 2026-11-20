package org.radar_project.service;

import org.junit.jupiter.api.Test;
import org.radar_project.config.TibProperties;
import org.radar_project.model.NormdataCandidate;
import org.radar_project.model.NormdataSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParamCount;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TibServiceTest {

    private static final String RESPONSE = """
            {"response": {"numFound": 1, "docs": [{
              "iri": "http://purl.obolibrary.org/obo/CHEBI_48729",
              "label": "zeolite",
              "synonym": ["Zeolith", "zeolites"],
              "ontology_name": "chebi",
              "short_form": "CHEBI_48729",
              "type": "class"
            }]}}
            """;

    @Test
    void mapsSearchHitsToCandidates() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://tib.test/api");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TibService tibService = new TibService(builder.build(), properties(List.of()));
        server.expect(requestTo(org.hamcrest.Matchers.startsWith("https://tib.test/api/search?q=Zeolith&rows=5")))
                .andExpect(queryParamCount(3))
                .andRespond(withSuccess(RESPONSE, MediaType.APPLICATION_JSON));

        List<NormdataCandidate> candidates = tibService.search("Zeolith");

        assertThat(candidates).containsExactly(new NormdataCandidate(NormdataSource.TIB, "CHEBI_48729",
                "http://purl.obolibrary.org/obo/CHEBI_48729", "zeolite", List.of("Zeolith", "zeolites"), "chebi",
                List.of("class")));
        server.verify();
    }

    @Test
    void restrictsSearchToConfiguredOntologies() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://tib.test/api");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TibService tibService = new TibService(builder.build(), properties(List.of("chebi", "gnd")));
        server.expect(queryParam("ontology", "chebi,gnd"))
                .andRespond(withSuccess(RESPONSE, MediaType.APPLICATION_JSON));

        assertThat(tibService.search("Zeolith")).hasSize(1);
        server.verify();
    }

    private static TibProperties properties(List<String> ontologies) {
        return new TibProperties("https://tib.test/api", 5, ontologies, Duration.ofSeconds(1), Duration.ofSeconds(1));
    }
}
