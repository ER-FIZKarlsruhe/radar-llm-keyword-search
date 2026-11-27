package org.radar_project.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.radar_project.config.GndProperties;
import org.radar_project.model.NormdataCandidate;
import org.radar_project.model.NormdataSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.GET;

class GndServiceTest {

    private MockRestServiceServer server;
    private GndService gndService;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://gnd.test/gnd");
        server = MockRestServiceServer.bindTo(builder).build();
        gndService = new GndService(builder.build(),
                new GndProperties("https://gnd.test/gnd", 5, null, Duration.ofSeconds(1), Duration.ofSeconds(1)));
    }

    @Test
    void mapsSearchHitsToCandidates() {
        server.expect(requestTo("https://gnd.test/gnd/search?q=Keratosis%20pilaris&format=json&size=5"))
                .andExpect(method(GET))
                .andRespond(withSuccess("""
                        {"totalItems": 1, "member": [{
                          "id": "https://d-nb.info/gnd/4316298-8",
                          "gndIdentifier": "4316298-8",
                          "preferredName": "Keratosis pilaris",
                          "variantName": ["Lichen pilaris"],
                          "type": ["SubjectHeading", "AuthorityResource"],
                          "unknownField": true
                        }]}
                        """, MediaType.APPLICATION_JSON));

        List<NormdataCandidate> candidates = gndService.search("Keratosis pilaris");

        assertThat(candidates).containsExactly(new NormdataCandidate(NormdataSource.GND, "4316298-8",
                "https://d-nb.info/gnd/4316298-8", "Keratosis pilaris", List.of("Lichen pilaris"), null,
                List.of("SubjectHeading", "AuthorityResource")));
        server.verify();
    }

    @Test
    void escapesQuerySyntax() {
        server.expect(queryParam("q", "Vitamin%20A%5C-S%C3%A4ure%20%5C%28topisch%5C%29"))
                .andRespond(withSuccess("{\"member\": []}", MediaType.APPLICATION_JSON));

        assertThat(gndService.search("Vitamin A-Säure (topisch)")).isEmpty();
        server.verify();
    }

    @Test
    void sendsConfiguredTypeFilter() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://gnd.test/gnd");
        MockRestServiceServer filterServer = MockRestServiceServer.bindTo(builder).build();
        GndService filteringService = new GndService(builder.build(), new GndProperties("https://gnd.test/gnd", 5,
                "NOT type:Work", Duration.ofSeconds(1), Duration.ofSeconds(1)));
        filterServer.expect(requestTo("https://gnd.test/gnd/search?q=Faust&format=json&size=5&filter=NOT%20type%3AWork"))
                .andRespond(withSuccess("{\"member\": []}", MediaType.APPLICATION_JSON));

        assertThat(filteringService.search("Faust")).isEmpty();
        filterServer.verify();
    }

    @Test
    void omitsFilterWhenNotConfigured() {
        server.expect(requestTo("https://gnd.test/gnd/search?q=Faust&format=json&size=5"))
                .andRespond(withSuccess("{\"member\": []}", MediaType.APPLICATION_JSON));

        assertThat(gndService.search("Faust")).isEmpty();
        server.verify();
    }

    @Test
    void propagatesServerErrors() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith("https://gnd.test/gnd/search")))
                .andRespond(withServerError());

        assertThatThrownBy(() -> gndService.search("Katalyse")).isInstanceOf(RestClientException.class);
    }
}
