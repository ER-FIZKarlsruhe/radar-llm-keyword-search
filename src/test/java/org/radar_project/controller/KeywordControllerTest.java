package org.radar_project.controller;

import org.junit.jupiter.api.Test;
import org.radar_project.model.ExtractRequest;
import org.radar_project.model.KeywordIri;
import org.radar_project.model.NormdataSource;
import org.radar_project.model.Terminology;
import org.radar_project.service.ExternalServiceException;
import org.radar_project.service.KeywordExtractionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(KeywordController.class)
class KeywordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KeywordExtractionService keywordExtractionService;

    @Test
    void extractPageReturnsHtmlForm() throws Exception {
        mockMvc.perform(get("/api/keywords/extract").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("<textarea")))
                .andExpect(content().string(containsString("fetch(\"extract\"")))
                .andExpect(content().string(containsString("name=\"terminology\"")))
                .andExpect(content().string(not(containsString("fetch(\"match\""))));
        verifyNoInteractions(keywordExtractionService);
    }

    @Test
    void extractReturnsKeywordMapInSnakeCaseIncludingNullValues() throws Exception {
        Map<String, KeywordIri> result = new LinkedHashMap<>();
        result.put("Zeolith", new KeywordIri("https://d-nb.info/gnd/4190779-6", "Zeolith", "Zeolith", null,
                NormdataSource.GND, "GND", "https://d-nb.info/gnd/", "4190779-6"));
        result.put("Katalyse", null);
        when(keywordExtractionService.extract(new ExtractRequest("Ein Text", Terminology.GND))).thenReturn(result);

        mockMvc.perform(post("/api/keywords/extract").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\": \"Ein Text\", \"terminology\": \"gnd\"}"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"Zeolith": {"iri": "https://d-nb.info/gnd/4190779-6", "label": "Zeolith",
                                     "best_term": "Zeolith", "ontology_name": null, "source": "gnd",
                                     "keyword_scheme": "GND", "scheme_uri": "https://d-nb.info/gnd/",
                                     "classification_code": "4190779-6"},
                         "Katalyse": null}""", true))
                .andExpect(content().string(containsString("\"Katalyse\":null")));
    }

    @Test
    void extractDefaultsTerminologyToNone() throws Exception {
        when(keywordExtractionService.extract(any())).thenReturn(Map.of());

        mockMvc.perform(post("/api/keywords/extract").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\": \"Ein Text\"}"))
                .andExpect(status().isOk());
        verify(keywordExtractionService).extract(new ExtractRequest("Ein Text", Terminology.NONE));
    }

    @Test
    void extractAcceptsAllTerminologies() throws Exception {
        when(keywordExtractionService.extract(any())).thenReturn(Map.of());

        for (Terminology terminology : Terminology.values()) {
            mockMvc.perform(post("/api/keywords/extract").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"text\": \"Ein Text\", \"terminology\": \"" + terminology.value() + "\"}"))
                    .andExpect(status().isOk());
            verify(keywordExtractionService).extract(new ExtractRequest("Ein Text", terminology));
        }
    }

    @Test
    void extractRejectsUnknownTerminology() throws Exception {
        mockMvc.perform(post("/api/keywords/extract").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\": \"Ein Text\", \"terminology\": \"wikidata\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(keywordExtractionService);
    }

    @Test
    void extractIgnoresLegacyMaxKeywordsParameter() throws Exception {
        when(keywordExtractionService.extract(any())).thenReturn(Map.of());

        mockMvc.perform(post("/api/keywords/extract").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\": \"Ein Text\", \"maxKeywords\": 1}"))
                .andExpect(status().isOk());
        verify(keywordExtractionService).extract(new ExtractRequest("Ein Text", Terminology.NONE));
    }

    @Test
    void extractRejectsBlankText() throws Exception {
        mockMvc.perform(post("/api/keywords/extract").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\": \" \", \"terminology\": \"gnd\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(keywordExtractionService);
    }

    @Test
    void extractMapsOllamaFailureToBadGateway() throws Exception {
        when(keywordExtractionService.extract(any())).thenThrow(new ExternalServiceException("Ollama request failed"));

        mockMvc.perform(post("/api/keywords/extract").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\": \"Text\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value("Ollama request failed"));
    }

    @Test
    void matchEndpointNoLongerExists() throws Exception {
        mockMvc.perform(post("/api/keywords/match").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keywords\": [\"Zeolith\"]}"))
                .andExpect(status().is4xxClientError());
        verifyNoInteractions(keywordExtractionService);
    }
}
