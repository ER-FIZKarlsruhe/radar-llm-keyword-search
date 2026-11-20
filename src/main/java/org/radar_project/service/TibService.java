package org.radar_project.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.radar_project.config.TibProperties;
import org.radar_project.model.NormdataCandidate;
import org.radar_project.model.NormdataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * Searches ontology terms via the TIB Terminology Service.
 */
@Service
public class TibService {

    private static final String FIELD_LIST = "iri,label,synonym,ontology_name,short_form,type";

    private final RestClient restClient;
    private final TibProperties properties;

    public TibService(@Qualifier("tib") RestClient restClient, TibProperties properties) {
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
                            .queryParam("rows", properties.maxResults())
                            .queryParam("fieldList", FIELD_LIST);
                    if (!properties.ontologies().isEmpty()) {
                        builder.queryParam("ontology", String.join(",", properties.ontologies()));
                    }
                    return builder.build(term);
                })
                .retrieve()
                .body(SearchResponse.class);
        if (response == null || response.response() == null || response.response().docs() == null) {
            return List.of();
        }
        return response.response().docs().stream()
                .map(doc -> new NormdataCandidate(NormdataSource.TIB, doc.shortForm(), doc.iri(), doc.label(),
                        doc.synonym(), doc.ontologyName(), doc.type() == null ? List.of() : List.of(doc.type())))
                .toList();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SearchResponse(Result response) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Result(List<Doc> docs) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Doc(String iri,
               String label,
               List<String> synonym,
               @JsonProperty("ontology_name") String ontologyName,
               @JsonProperty("short_form") String shortForm,
               String type) {
    }
}
