package org.radar_project.service;

import org.radar_project.config.MatchingProperties;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

/**
 * Searches GND and/or TIB for each keyword in parallel and picks the best match per keyword. A failing
 * search does not fail the request: it is logged and that source simply contributes no match.
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

    /**
     * Returns the best match per keyword, in keyword order. Keywords with the same normalized form
     * (see {@link MatchingService#normalize}) are merged: only the first of them is searched and returned
     * as key, so the returned map has exactly one entry per normalized form. The value is null if no
     * source has a match.
     *
     * @param sources        the sources to search
     * @param preferredOnTie the source whose match wins if the best matches of both sources score equally
     */
    public Map<String, @Nullable NormdataMatch> bestMatches(List<String> keywords, Set<NormdataSource> sources,
                                                            NormdataSource preferredOnTie) {
        List<String> distinctKeywords = distinctByNormalizedForm(keywords);
        List<List<Future<List<NormdataMatch>>>> futures = new ArrayList<>();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (String keyword : distinctKeywords) {
                List<Future<List<NormdataMatch>>> keywordFutures = new ArrayList<>();
                for (NormdataSource source : sources) {
                    keywordFutures.add(executor.submit(() -> search(keyword, source)));
                }
                futures.add(keywordFutures);
            }
        }

        Comparator<NormdataMatch> bestFirst = Comparator.comparingDouble(NormdataMatch::score).reversed()
                .thenComparing(match -> match.source() == preferredOnTie ? 0 : 1);
        Map<String, @Nullable NormdataMatch> bestMatches = new LinkedHashMap<>();
        for (int i = 0; i < distinctKeywords.size(); i++) {
            List<NormdataMatch> matches = new ArrayList<>();
            for (Future<List<NormdataMatch>> future : futures.get(i)) {
                matches.addAll(result(future));
            }
            bestMatches.put(distinctKeywords.get(i), matches.stream().min(bestFirst).orElse(null));
        }
        return bestMatches;
    }

    /** The first keyword of each normalized form, in keyword order. */
    private static List<String> distinctByNormalizedForm(List<String> keywords) {
        Map<String, String> firstByNormalizedForm = new LinkedHashMap<>();
        for (String keyword : keywords) {
            firstByNormalizedForm.putIfAbsent(MatchingService.normalize(keyword), keyword);
        }
        return List.copyOf(firstByNormalizedForm.values());
    }

    private List<NormdataMatch> search(String term, NormdataSource source) {
        try {
            searchSlots.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return List.of();
        }
        try {
            List<NormdataCandidate> candidates = switch (source) {
                case GND -> gndService.search(term);
                case TIB -> tibService.search(term);
            };
            return matchingService.match(term, candidates);
        } catch (RestClientException e) {
            log.warn("{} search failed: {}", source.value(), e.getMessage());
            return List.of();
        } finally {
            searchSlots.release();
        }
    }

    private static List<NormdataMatch> result(Future<List<NormdataMatch>> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for normdata search", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Normdata search failed unexpectedly", e.getCause());
        }
    }
}
