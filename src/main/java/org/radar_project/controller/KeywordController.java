package org.radar_project.controller;

import jakarta.validation.Valid;
import org.radar_project.model.ExtractRequest;
import org.radar_project.model.ExtractionResult;
import org.radar_project.model.MatchRequest;
import org.radar_project.model.MatchResponse;
import org.radar_project.service.KeywordMatchService;
import org.radar_project.service.OllamaService;
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

    private final OllamaService ollamaService;
    private final KeywordMatchService keywordMatchService;

    public KeywordController(OllamaService ollamaService, KeywordMatchService keywordMatchService) {
        this.ollamaService = ollamaService;
        this.keywordMatchService = keywordMatchService;
    }

    @PostMapping("/extract")
    public ExtractionResult extract(@Valid @RequestBody ExtractRequest request) {
        return ollamaService.extract(request.text(), request.maxKeywords());
    }

    @PostMapping("/match")
    public MatchResponse match(@Valid @RequestBody MatchRequest request) {
        return keywordMatchService.match(request);
    }
}
