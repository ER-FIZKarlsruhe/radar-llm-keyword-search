package org.radar_project.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.radar_project.config.OllamaProperties;
import org.radar_project.model.ExtractionResult;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OllamaServiceTest {

    private MockRestServiceServer server;
    private OllamaService ollamaService;

    @BeforeEach
    void setUp() {
        createService(10);
    }

    private void createService(int maxKeywords) {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://ollama.test/ollama");
        server = MockRestServiceServer.bindTo(builder).build();
        ollamaService = new OllamaService(builder.build(),
                new OllamaProperties("https://ollama.test/ollama", "keyword-extractor",
                        Duration.ofSeconds(1), Duration.ofSeconds(1), maxKeywords),
                JsonMapper.builder().build());
    }

    @Test
    void sendsTextToModelAndParsesKeywords() {
        server.expect(requestTo("https://ollama.test/ollama/api/chat"))
                .andExpect(method(POST))
                .andExpect(jsonPath("$.model").value("keyword-extractor"))
                .andExpect(jsonPath("$.stream").value(false))
                .andExpect(jsonPath("$.format.required[1]").value("keywords"))
                .andExpect(jsonPath("$.messages[0].content").value("Ein Text über Zeolithe."))
                .andRespond(chatResponse("""
                        {"language": "de", "keywords": ["Zeolith", " ", "Katalyse"]}"""));

        ExtractionResult result = ollamaService.extract("Ein Text über Zeolithe.");

        assertThat(result.language()).isEqualTo("de");
        assertThat(result.keywords()).containsExactly("Zeolith", "Katalyse");
        server.verify();
    }

    @Test
    void sendsSchemaPropertiesInFixedOrderSoThatLanguageIsGeneratedFirst() {
        // Ollama generates fields in schema order; "language" must precede "keywords",
        // otherwise the model writes keywords before it has determined the text language.
        server.expect(requestTo("https://ollama.test/ollama/api/chat"))
                .andExpect(request -> {
                    String body = ((MockClientHttpRequest) request).getBodyAsString();
                    JsonNode format = JsonMapper.builder().build().readTree(body).get("format");
                    assertThat(propertyNames(format.get("properties"))).containsExactly("language", "keywords");
                    assertThat(format.at("/properties/keywords/items/type").asString()).isEqualTo("string");
                })
                .andRespond(chatResponse("""
                        {"language": "en", "keywords": ["Zeolite"]}"""));

        ollamaService.extract("A text about zeolites.");

        server.verify();
    }

    private static List<String> propertyNames(JsonNode node) {
        return node.properties().stream().map(Map.Entry::getKey).toList();
    }

    @Test
    void limitsNumberOfKeywordsToConfiguredMaximum() {
        createService(1);
        server.expect(requestTo("https://ollama.test/ollama/api/chat"))
                .andRespond(chatResponse("""
                        {"language": "de", "keywords": ["A", "B"]}"""));

        assertThat(ollamaService.extract("Text").keywords()).containsExactly("A");
    }

    @Test
    void rejectsInvalidModelOutput() {
        server.expect(requestTo("https://ollama.test/ollama/api/chat"))
                .andRespond(chatResponse("keine JSON-Antwort"));

        assertThatThrownBy(() -> ollamaService.extract("Text"))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessage("Ollama returned invalid keyword JSON");
    }

    @Test
    void rejectsLegacyKeywordObjects() {
        server.expect(requestTo("https://ollama.test/ollama/api/chat"))
                .andRespond(chatResponse("""
                        {"language": "de", "keywords": [{"term": "Zeolith", "type": "direkt"}]}"""));

        assertThatThrownBy(() -> ollamaService.extract("Text"))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessage("Ollama returned invalid keyword JSON");
    }

    @Test
    void wrapsServerErrors() {
        server.expect(requestTo("https://ollama.test/ollama/api/chat")).andRespond(withServerError());

        assertThatThrownBy(() -> ollamaService.extract("Text"))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessage("Ollama request failed");
    }

    private static org.springframework.test.web.client.ResponseCreator chatResponse(String content) {
        String body = JsonMapper.builder().build().writeValueAsString(java.util.Map.of(
                "model", "keyword-extractor",
                "done", true,
                "message", java.util.Map.of("role", "assistant", "content", content)));
        return withSuccess(body, MediaType.APPLICATION_JSON);
    }
}
