package org.radar_project.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

/**
 * Connection settings for the TIB Terminology Service search API.
 *
 * @param ontologies optional list of ontology ids to restrict the search to; empty means all ontologies
 */
@Validated
@ConfigurationProperties("radar.tib")
public record TibProperties(
        @NotBlank String baseUrl,
        @Min(1) int maxResults,
        List<String> ontologies,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout) {

    public TibProperties {
        ontologies = ontologies == null ? List.of() : List.copyOf(ontologies);
    }
}
