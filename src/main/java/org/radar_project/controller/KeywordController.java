package org.radar_project.controller;

import jakarta.validation.Valid;
import org.radar_project.model.ExtractRequest;
import org.radar_project.model.ExtractionResult;
import org.radar_project.model.MatchRequest;
import org.radar_project.model.MatchResponse;
import org.radar_project.service.KeywordMatchService;
import org.radar_project.service.OllamaService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints for keyword extraction and normdata matching. The response of {@code /extract}
 * can be posted unchanged to {@code /match}.
 */
@RestController
@RequestMapping("/api/keywords")
public class KeywordController {

    private static final Resource EXTRACT_PAGE = new ClassPathResource("web/extract.html");

    private final OllamaService ollamaService;
    private final KeywordMatchService keywordMatchService;

    public KeywordController(OllamaService ollamaService, KeywordMatchService keywordMatchService) {
        this.ollamaService = ollamaService;
        this.keywordMatchService = keywordMatchService;
    }

    /**
     * Returns a simple HTML page with a form that posts the entered text to {@code POST /extract}.
     */
    @GetMapping(value = "/extract", produces = MediaType.TEXT_HTML_VALUE)
    public Resource extractPage() {
        return EXTRACT_PAGE;
    }

    @PostMapping("/extract")
    public ExtractionResult extract(@Valid @RequestBody ExtractRequest request) {
        return ollamaService.extract(request.text());
    }

    @PostMapping("/match")
    public MatchResponse match(@Valid @RequestBody MatchRequest request) {
        return keywordMatchService.match(request);
    }
}
