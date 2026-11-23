package org.radar_project.controller;

import jakarta.validation.Valid;
import org.jspecify.annotations.Nullable;
import org.radar_project.model.ExtractRequest;
import org.radar_project.model.KeywordIri;
import org.radar_project.service.KeywordExtractionService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Keyword extraction with optional normdata lookup, in the response format of radar-keyword-search's
 * {@code /extract-iris}.
 */
@RestController
@RequestMapping("/api/keywords")
public class KeywordController {

    private static final Resource EXTRACT_PAGE = new ClassPathResource("web/extract.html");

    private final KeywordExtractionService keywordExtractionService;

    public KeywordController(KeywordExtractionService keywordExtractionService) {
        this.keywordExtractionService = keywordExtractionService;
    }

    /**
     * Returns a simple HTML page with a form that posts the entered text to {@code POST /extract}.
     */
    @GetMapping(value = "/extract", produces = MediaType.TEXT_HTML_VALUE)
    public Resource extractPage() {
        return EXTRACT_PAGE;
    }

    /**
     * Returns the best match per keyword ({@code null} if none), in the order the model returned the keywords.
     */
    @PostMapping("/extract")
    public Map<String, @Nullable KeywordIri> extract(@Valid @RequestBody ExtractRequest request) {
        return keywordExtractionService.extract(request);
    }
}
