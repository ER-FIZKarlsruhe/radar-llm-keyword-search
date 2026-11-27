package org.radar_project.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

/**
 * Connection settings for the Ollama server hosting the keyword extraction model.
 *
 * @param models      models a request may select; any other model name is rejected
 * @param maxKeywords   upper bound of keywords returned by {@code POST /api/keywords/extract}
 * @param maxTextLength maximum number of characters sent to the model; longer texts are truncated so
 *                      that system prompt and text fit into the model's {@code num_ctx}
 */
@Validated
@ConfigurationProperties("radar.ollama")
public record OllamaProperties(
        @NotBlank String baseUrl,
        @NotEmpty List<@NotBlank String> models,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout,
        @NotNull @Positive Integer maxKeywords,
        @NotNull @Positive Integer maxTextLength) {

    public OllamaProperties {
        models = models == null ? List.of() : List.copyOf(models);
    }

    /** Whether a request may select the given model, i.e. it is one of {@link #models()}. */
    public boolean isSelectable(String name) {
        return models.contains(name);
    }
}
