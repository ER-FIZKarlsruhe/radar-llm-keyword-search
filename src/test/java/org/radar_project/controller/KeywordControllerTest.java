package org.radar_project.controller;

import org.junit.jupiter.api.Test;
import org.radar_project.model.ExtractionResult;
import org.radar_project.model.Keyword;
import org.radar_project.model.MatchRequest;
import org.radar_project.model.MatchResponse;
import org.radar_project.model.NormdataSource;
import org.radar_project.service.ExternalServiceException;
import org.radar_project.service.KeywordMatchService;
import org.radar_project.service.OllamaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(KeywordController.class)
class KeywordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OllamaService ollamaService;

    @MockitoBean
    private KeywordMatchService keywordMatchService;

    @Test
    void extractReturnsKeywords() throws Exception {
        when(ollamaService.extract("Ein Text", 5))
                .thenReturn(new ExtractionResult("de", List.of(new Keyword("Text", "direkt"))));

        mockMvc.perform(post("/api/keywords/extract").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\": \"Ein Text\", \"maxKeywords\": 5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.language").value("de"))
                .andExpect(jsonPath("$.keywords[0].term").value("Text"))
                .andExpect(jsonPath("$.keywords[0].type").value("direkt"));
    }

    @Test
    void extractRejectsBlankText() throws Exception {
        mockMvc.perform(post("/api/keywords/extract").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\": \" \"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(ollamaService);
    }

    @Test
    void extractMapsOllamaFailureToBadGateway() throws Exception {
        when(ollamaService.extract("Text", null)).thenThrow(new ExternalServiceException("Ollama request failed"));

        mockMvc.perform(post("/api/keywords/extract").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\": \"Text\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value("Ollama request failed"));
    }

    @Test
    void matchAcceptsExtractResponseWithSources() throws Exception {
        when(keywordMatchService.match(any())).thenReturn(new MatchResponse("de", List.of(), List.of()));

        mockMvc.perform(post("/api/keywords/match").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"language": "de", "sources": ["gnd"],
                                 "keywords": [{"term": "Zeolith", "type": "direkt"}]}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.language").value("de"));
        verify(keywordMatchService).match(new MatchRequest("de",
                List.of(new Keyword("Zeolith", "direkt")), Set.of(NormdataSource.GND)));
    }

    @Test
    void matchRejectsUnknownSource() throws Exception {
        mockMvc.perform(post("/api/keywords/match").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sources\": [\"wikidata\"], \"keywords\": [{\"term\": \"Zeolith\"}]}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(keywordMatchService);
    }

    @Test
    void matchRejectsMissingKeywords() throws Exception {
        mockMvc.perform(post("/api/keywords/match").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keywords\": []}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(keywordMatchService);
    }
}
