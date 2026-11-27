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
        createService(10, 10_000);
    }

    private void createService(int maxKeywords) {
        createService(maxKeywords, 10_000);
    }

    private void createService(int maxKeywords, int maxTextLength) {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://ollama.test/ollama");
        server = MockRestServiceServer.bindTo(builder).build();
        ollamaService = new OllamaService(builder.build(),
                new OllamaProperties("https://ollama.test/ollama", List.of("keyword-extractor", "keyword-extractor2"),
                        Duration.ofSeconds(1), Duration.ofSeconds(1), maxKeywords, maxTextLength),
                JsonMapper.builder().build());
    }

    @Test
    void truncatesTextLongerThanMaxTextLength() {
        createService(10, 5);
        server.expect(requestTo("https://ollama.test/ollama/api/chat"))
                .andExpect(jsonPath("$.messages[0].content").value("Zeoli"))
                .andRespond(chatResponse("""
                        {"language": "de", "keywords": ["Zeolith"]}"""));

        ollamaService.extract("Zeolithe", "keyword-extractor");

        server.verify();
    }

    @Test
    void keepsTextOfExactlyMaxTextLength() {
        createService(10, 8);
        server.expect(requestTo("https://ollama.test/ollama/api/chat"))
                .andExpect(jsonPath("$.messages[0].content").value("Zeolithe"))
                .andRespond(chatResponse("""
                        {"language": "de", "keywords": ["Zeolith"]}"""));

        ollamaService.extract("Zeolithe", "keyword-extractor");

        server.verify();
    }

    @Test
    void doesNotSplitSurrogatePairWhenTruncating() {
        createService(10, 2);
        server.expect(requestTo("https://ollama.test/ollama/api/chat"))
                .andExpect(jsonPath("$.messages[0].content").value("a"))
                .andRespond(chatResponse("""
                        {"language": "de", "keywords": ["a"]}"""));

        ollamaService.extract("a\uD835\uDC00b", "keyword-extractor");

        server.verify();
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

        ExtractionResult result = ollamaService.extract("Ein Text über Zeolithe.", "keyword-extractor");

        assertThat(result.language()).isEqualTo("de");
        assertThat(result.keywords()).containsExactly("Zeolith", "Katalyse");
        server.verify();
    }

    @Test
    void sendsSelectedModel() {
        server.expect(requestTo("https://ollama.test/ollama/api/chat"))
                .andExpect(jsonPath("$.model").value("keyword-extractor2"))
                .andRespond(chatResponse("""
                        {"language": "de", "keywords": ["Zeolith"]}"""));

        ollamaService.extract("Text", "keyword-extractor2");

        server.verify();
    }

    @Test
    void acceptsDefaultModelAsExplicitSelection() {
        server.expect(requestTo("https://ollama.test/ollama/api/chat"))
                .andExpect(jsonPath("$.model").value("keyword-extractor"))
                .andRespond(chatResponse("""
                        {"language": "de", "keywords": ["Zeolith"]}"""));

        ollamaService.extract("Text", "keyword-extractor");

        server.verify();
    }

    @Test
    void rejectsBlankModelWithoutCallingOllama() {
        assertThatThrownBy(() -> ollamaService.extract("Text", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown model");

        server.verify();
    }

    @Test
    void rejectsUnconfiguredModelWithoutCallingOllama() {
        assertThatThrownBy(() -> ollamaService.extract("Text", "llama3:70b"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown model");

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

        ollamaService.extract("A text about zeolites.", "keyword-extractor");

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

        assertThat(ollamaService.extract("Text", "keyword-extractor").keywords()).containsExactly("A");
    }

    @Test
    void rejectsInvalidModelOutput() {
        server.expect(requestTo("https://ollama.test/ollama/api/chat"))
                .andRespond(chatResponse("keine JSON-Antwort"));

        assertThatThrownBy(() -> ollamaService.extract("Text", "keyword-extractor"))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessage("Ollama returned invalid keyword JSON");
    }

    @Test
    void rejectsLegacyKeywordObjects() {
        server.expect(requestTo("https://ollama.test/ollama/api/chat"))
                .andRespond(chatResponse("""
                        {"language": "de", "keywords": [{"term": "Zeolith", "type": "direkt"}]}"""));

        assertThatThrownBy(() -> ollamaService.extract("Text", "keyword-extractor"))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessage("Ollama returned invalid keyword JSON");
    }

    @Test
    void wrapsServerErrors() {
        server.expect(requestTo("https://ollama.test/ollama/api/chat")).andRespond(withServerError());

        assertThatThrownBy(() -> ollamaService.extract("Text", "keyword-extractor"))
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
