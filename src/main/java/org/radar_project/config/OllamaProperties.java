package org.radar_project.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Connection settings for the Ollama server hosting the keyword extraction model.
 *
 * @param maxKeywords upper bound of keywords returned by {@code POST /api/keywords/extract}
 */
@Validated
@ConfigurationProperties("radar.ollama")
public record OllamaProperties(
        @NotBlank String baseUrl,
        @NotBlank String model,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout,
        @NotNull @Positive Integer maxKeywords) {
}
