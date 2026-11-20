package org.radar_project.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.radar_project.config.OllamaProperties;
import org.radar_project.model.ExtractionResult;
import org.radar_project.model.Keyword;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;

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
        RestClient.Builder builder = RestClient.builder().baseUrl("https://ollama.test/ollama");
        server = MockRestServiceServer.bindTo(builder).build();
        ollamaService = new OllamaService(builder.build(),
                new OllamaProperties("https://ollama.test/ollama", "keyword-extractor",
                        Duration.ofSeconds(1), Duration.ofSeconds(1)),
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
                        {"language": "de", "keywords": [
                          {"term": "Zeolith", "type": "direkt"},
                          {"term": " ", "type": "direkt"},
                          {"term": "Katalyse", "type": "erschlossen"}]}"""));

        ExtractionResult result = ollamaService.extract("Ein Text über Zeolithe.", null);

        assertThat(result.language()).isEqualTo("de");
        assertThat(result.keywords()).containsExactly(
                new Keyword("Zeolith", "direkt"), new Keyword("Katalyse", "erschlossen"));
        server.verify();
    }

    @Test
    void limitsNumberOfKeywords() {
        server.expect(requestTo("https://ollama.test/ollama/api/chat"))
                .andRespond(chatResponse("""
                        {"language": "de", "keywords": [
                          {"term": "A", "type": "direkt"}, {"term": "B", "type": "direkt"}]}"""));

        assertThat(ollamaService.extract("Text", 1).keywords()).extracting(Keyword::term).containsExactly("A");
    }

    @Test
    void rejectsInvalidModelOutput() {
        server.expect(requestTo("https://ollama.test/ollama/api/chat"))
                .andRespond(chatResponse("keine JSON-Antwort"));

        assertThatThrownBy(() -> ollamaService.extract("Text", null))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessage("Ollama returned invalid keyword JSON");
    }

    @Test
    void wrapsServerErrors() {
        server.expect(requestTo("https://ollama.test/ollama/api/chat")).andRespond(withServerError());

        assertThatThrownBy(() -> ollamaService.extract("Text", null))
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
