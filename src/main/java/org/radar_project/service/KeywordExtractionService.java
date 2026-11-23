package org.radar_project.service;

import org.jspecify.annotations.Nullable;
import org.radar_project.model.ExtractRequest;
import org.radar_project.model.ExtractionResult;
import org.radar_project.model.KeywordIri;
import org.radar_project.model.NormdataMatch;
import org.radar_project.model.NormdataSource;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Extracts keywords with the LLM and looks them up in the requested terminology, in the response
 * format of radar-keyword-search's {@code /extract-iris}.
 */
@Service
public class KeywordExtractionService {

    static final String GND_SCHEME_URI = "https://d-nb.info/gnd/";
    static final String KEYWORD_SCHEME_GND = "GND";
    static final String KEYWORD_SCHEME_OTHER = "Other";

    private final OllamaService ollamaService;
    private final KeywordMatchService keywordMatchService;

    public KeywordExtractionService(OllamaService ollamaService, KeywordMatchService keywordMatchService) {
        this.ollamaService = ollamaService;
        this.keywordMatchService = keywordMatchService;
    }

    /**
     * Returns the best match per keyword, in the order the model returned the keywords. The value is
     * null if no match was found, a search failed, or the terminology is {@code none}.
     *
     * @throws ExternalServiceException if the keyword extraction fails
     */
    public Map<String, @Nullable KeywordIri> extract(ExtractRequest request) {
        ExtractionResult extraction = ollamaService.extract(request.text());
        Set<NormdataSource> sources = request.terminology().sources();
        Map<String, @Nullable NormdataMatch> matches = sources.isEmpty()
                ? Map.of()
                : keywordMatchService.bestMatches(extraction.keywords(), sources, preferredOnTie(extraction.language()));

        Map<String, @Nullable KeywordIri> result = new LinkedHashMap<>();
        for (String keyword : extraction.keywords()) {
            NormdataMatch match = matches.get(keyword);
            result.put(keyword, match == null ? null : toKeywordIri(match));
        }
        return result;
    }

    /** Like radar-keyword-search: on a tie the GND wins for German texts, TIB otherwise. */
    private static NormdataSource preferredOnTie(@Nullable String language) {
        return language != null && language.toLowerCase(Locale.ROOT).startsWith("de")
                ? NormdataSource.GND
                : NormdataSource.TIB;
    }

    private static KeywordIri toKeywordIri(NormdataMatch match) {
        return switch (match.source()) {
            case GND -> new KeywordIri(match.uri(), match.label(), match.matchedLabel(), null, NormdataSource.GND,
                    KEYWORD_SCHEME_GND, GND_SCHEME_URI,
                    match.id() != null ? match.id() : lastSegment(match.uri()));
            case TIB -> new KeywordIri(match.uri(), match.label(), match.matchedLabel(), match.vocabulary(),
                    NormdataSource.TIB, KEYWORD_SCHEME_OTHER, schemeUri(match.uri()),
                    match.id() != null ? match.id() : lastSegment(match.uri()));
        };
    }

    /** The IRI up to and including its last '/', as radar-keyword-search and radar-frontend split TIB IRIs. */
    private static @Nullable String schemeUri(@Nullable String iri) {
        int slash = iri == null ? -1 : iri.lastIndexOf('/');
        return slash < 0 ? null : iri.substring(0, slash + 1);
    }

    private static @Nullable String lastSegment(@Nullable String iri) {
        return iri == null ? null : iri.substring(iri.lastIndexOf('/') + 1);
    }
}
