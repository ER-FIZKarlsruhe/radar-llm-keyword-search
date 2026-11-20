package org.radar_project.service;

import org.radar_project.config.MatchingProperties;
import org.radar_project.model.Keyword;
import org.radar_project.model.MatchRequest;
import org.radar_project.model.MatchResponse;
import org.radar_project.model.MatchedKeyword;
import org.radar_project.model.NormdataCandidate;
import org.radar_project.model.NormdataMatch;
import org.radar_project.model.NormdataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

/**
 * Searches GND and/or TIB for each keyword in parallel and matches the hits. A failing search
 * does not fail the request but is reported as warning.
 */
@Service
public class KeywordMatchService {

    private static final Logger log = LoggerFactory.getLogger(KeywordMatchService.class);

    private final GndService gndService;
    private final TibService tibService;
    private final MatchingService matchingService;
    private final Semaphore searchSlots;

    public KeywordMatchService(GndService gndService, TibService tibService, MatchingService matchingService,
                               MatchingProperties properties) {
        this.gndService = gndService;
        this.tibService = tibService;
        this.matchingService = matchingService;
        this.searchSlots = new Semaphore(properties.maxConcurrentRequests());
    }

    public MatchResponse match(MatchRequest request) {
        Set<NormdataSource> sources = request.sources().isEmpty()
                ? EnumSet.allOf(NormdataSource.class)
                : EnumSet.copyOf(request.sources());

        List<List<Future<SearchOutcome>>> futures = new ArrayList<>();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (Keyword keyword : request.keywords()) {
                List<Future<SearchOutcome>> keywordFutures = new ArrayList<>();
                for (NormdataSource source : sources) {
                    keywordFutures.add(executor.submit(() -> search(keyword.term(), source)));
                }
                futures.add(keywordFutures);
            }
        }

        List<MatchedKeyword> keywords = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        for (int i = 0; i < request.keywords().size(); i++) {
            Keyword keyword = request.keywords().get(i);
            List<NormdataMatch> matches = new ArrayList<>();
            for (Future<SearchOutcome> future : futures.get(i)) {
                SearchOutcome outcome = result(future);
                matches.addAll(outcome.matches());
                if (outcome.warning() != null) {
                    warnings.add(outcome.warning());
                }
            }
            matches.sort(Comparator.comparingDouble(NormdataMatch::score).reversed());
            keywords.add(new MatchedKeyword(keyword.term(), keyword.type(), List.copyOf(matches)));
        }
        return new MatchResponse(request.language(), keywords, warnings);
    }

    private SearchOutcome search(String term, NormdataSource source) {
        try {
            searchSlots.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return SearchOutcome.failed(source, term);
        }
        try {
            List<NormdataCandidate> candidates = switch (source) {
                case GND -> gndService.search(term);
                case TIB -> tibService.search(term);
            };
            return new SearchOutcome(matchingService.match(term, candidates), null);
        } catch (RestClientException e) {
            log.warn("{} search failed: {}", source.value(), e.getMessage());
            return SearchOutcome.failed(source, term);
        } finally {
            searchSlots.release();
        }
    }

    private static SearchOutcome result(Future<SearchOutcome> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for normdata search", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Normdata search failed unexpectedly", e.getCause());
        }
    }

    private record SearchOutcome(List<NormdataMatch> matches, @Nullable String warning) {

        static SearchOutcome failed(NormdataSource source, String term) {
            return new SearchOutcome(List.of(), source.value() + " search failed for term '" + term + "'");
        }
    }
}
