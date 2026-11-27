package org.radar_project.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.radar_project.config.GndProperties;
import org.radar_project.model.NormdataCandidate;
import org.radar_project.model.NormdataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * Searches the GND via the lobid-gnd API.
 */
@Service
public class GndService {

    private static final String LUCENE_SPECIAL_CHARACTERS = "+-&|!(){}[]^\"~*?:\\/";

    private final RestClient restClient;
    private final GndProperties properties;

    public GndService(@Qualifier("gnd") RestClient restClient, GndProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    /**
     * @throws RestClientException if the search fails
     */
    public List<NormdataCandidate> search(String term) {
        SearchResponse response = restClient.get()
                .uri(builder -> {
                    builder.path("/search")
                            .queryParam("q", "{q}")
                            .queryParam("format", "json")
                            .queryParam("size", properties.maxResults());
                    if (properties.filter().isEmpty()) {
                        return builder.build(escapeLucene(term));
                    }
                    return builder.queryParam("filter", "{filter}").build(escapeLucene(term), properties.filter());
                })
                .retrieve()
                .body(SearchResponse.class);
        if (response == null || response.member() == null) {
            return List.of();
        }
        return response.member().stream()
                .map(entry -> new NormdataCandidate(NormdataSource.GND, entry.gndIdentifier(), entry.id(),
                        entry.preferredName(), entry.variantName(), null, entry.type()))
                .toList();
    }

    /** lobid passes {@code q} to Elasticsearch as query string, so query syntax characters have to be escaped. */
    static String escapeLucene(String term) {
        StringBuilder escaped = new StringBuilder(term.length());
        for (char c : term.toCharArray()) {
            if (LUCENE_SPECIAL_CHARACTERS.indexOf(c) >= 0) {
                escaped.append('\\');
            }
            escaped.append(c);
        }
        return escaped.toString();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SearchResponse(List<Entry> member) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Entry(String id, String gndIdentifier, String preferredName, List<String> variantName, List<String> type) {
    }
}
