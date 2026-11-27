package org.radar_project.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.radar_project.config.OllamaProperties;
import org.radar_project.model.ExtractionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Extracts keyword candidates from a text using the keyword-extractor model on Ollama.
 */
@Service
public class OllamaService {

    private static final Logger log = LoggerFactory.getLogger(OllamaService.class);

    /**
     * JSON schema passed as {@code format} so that Ollama constrains the model output.
     * Ollama generates the fields in schema order, so the property order matters: "language" must
     * come before "keywords", otherwise the model may emit keywords before it has determined the
     * language of the text. {@code Map.of} has no stable iteration order (it varies between JVM
     * starts), hence the ordered maps.
     */
    private static final Map<String, Object> RESPONSE_SCHEMA = createResponseSchema();

    private static Map<String, Object> createResponseSchema() {
        Map<String, Object> keywords = new LinkedHashMap<>();
        keywords.put("type", "array");
        keywords.put("items", Map.of("type", "string"));

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("language", Map.of("type", "string"));
        properties.put("keywords", keywords);

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", List.of("language", "keywords"));
        return Collections.unmodifiableMap(schema);
    }

    private final RestClient restClient;
    private final OllamaProperties properties;
    private final JsonMapper jsonMapper;

    public OllamaService(@Qualifier("ollama") RestClient restClient, OllamaProperties properties, JsonMapper jsonMapper) {
        this.restClient = restClient;
        this.properties = properties;
        this.jsonMapper = jsonMapper;
    }

    /**
     * Extracts keywords from the given text, limited to {@code radar.ollama.max-keywords} entries.
     * Texts longer than {@code radar.ollama.max-text-length} characters are truncated.
     *
     * @param text  the text to extract keywords from
     * @param model the model to use; must be one of {@code radar.ollama.models}
     * @throws IllegalArgumentException if the model is not configured as selectable
     * @throws ExternalServiceException if Ollama fails or returns no valid keyword JSON
     */
    public ExtractionResult extract(String text, String model) {
        requireSelectable(model);
        String content = chat(truncate(text, model), model);
        ExtractionResult result;
        try {
            result = jsonMapper.readValue(content, ExtractionResult.class);
        } catch (JacksonException e) {
            throw new ExternalServiceException("Ollama returned invalid keyword JSON", e);
        }
        List<String> keywords = result.keywords().stream()
                .filter(keyword -> keyword != null && !keyword.isBlank())
                .limit(properties.maxKeywords())
                .toList();
        return new ExtractionResult(result.language(), keywords);
    }

    /**
     * Cuts the text to {@code radar.ollama.max-text-length} characters. Otherwise Ollama would
     * silently drop the beginning of the prompt, including the system prompt, once it exceeds num_ctx.
     */
    private String truncate(String text, String model) {
        int maxLength = properties.maxTextLength();
        if (text.length() <= maxLength) {
            return text;
        }
        int end = Character.isHighSurrogate(text.charAt(maxLength - 1)) ? maxLength - 1 : maxLength;
        log.warn("Text for model {} truncated from {} to {} characters", model, text.length(), end);
        return text.substring(0, end);
    }

    /** Only configured models may be used, so that clients cannot make Ollama load arbitrary models. */
    private void requireSelectable(String model) {
        if (!properties.isSelectable(model)) {
            throw new IllegalArgumentException("Unknown model");
        }
    }

    private String chat(String text, String model) {
        Map<String, Object> request = Map.of(
                "model", model,
                "stream", false,
                "think", false,
                "format", RESPONSE_SCHEMA,
                "messages", List.of(Map.of("role", "user", "content", text)));
        ChatResponse response;
        try {
            response = restClient.post()
                    .uri("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ChatResponse.class);
        } catch (RestClientException e) {
            throw new ExternalServiceException("Ollama request failed", e);
        }
        if (response == null || response.message() == null
                || response.message().content() == null || response.message().content().isBlank()) {
            throw new ExternalServiceException("Ollama returned no content");
        }
        return response.message().content();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChatResponse(Message message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Message(String role, String content) {
    }
}
