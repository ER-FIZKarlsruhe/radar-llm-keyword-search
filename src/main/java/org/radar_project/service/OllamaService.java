package org.radar_project.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.radar_project.config.OllamaProperties;
import org.radar_project.model.ExtractionResult;
import org.radar_project.model.Keyword;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

/**
 * Extracts keyword candidates from a text using the keyword-extractor model on Ollama.
 */
@Service
public class OllamaService {

    /** JSON schema passed as {@code format} so that Ollama constrains the model output. */
    private static final Map<String, Object> RESPONSE_SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of(
                    "language", Map.of("type", "string"),
                    "keywords", Map.of(
                            "type", "array",
                            "items", Map.of(
                                    "type", "object",
                                    "properties", Map.of(
                                            "term", Map.of("type", "string"),
                                            "type", Map.of("type", "string", "enum", List.of("direkt", "erschlossen"))),
                                    "required", List.of("term", "type")))),
            "required", List.of("language", "keywords"));

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
     *
     * @param text the text to extract keywords from
     * @throws ExternalServiceException if Ollama fails or returns no valid keyword JSON
     */
    public ExtractionResult extract(String text) {
        String content = chat(text);
        ExtractionResult result;
        try {
            result = jsonMapper.readValue(content, ExtractionResult.class);
        } catch (JacksonException e) {
            throw new ExternalServiceException("Ollama returned invalid keyword JSON", e);
        }
        List<Keyword> keywords = result.keywords().stream()
                .filter(keyword -> keyword != null && keyword.term() != null && !keyword.term().isBlank())
                .limit(properties.maxKeywords())
                .toList();
        return new ExtractionResult(result.language(), keywords);
    }

    private String chat(String text) {
        Map<String, Object> request = Map.of(
                "model", properties.model(),
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
