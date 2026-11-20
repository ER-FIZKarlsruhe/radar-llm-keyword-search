package org.radar_project.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Connection settings for the lobid-gnd search API.
 */
@Validated
@ConfigurationProperties("radar.gnd")
public record GndProperties(
        @NotBlank String baseUrl,
        @Min(1) int maxResults,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout) {
}
