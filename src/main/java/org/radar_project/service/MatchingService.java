package org.radar_project.service;

import org.radar_project.model.MatchType;
import org.radar_project.model.NormdataCandidate;
import org.radar_project.model.NormdataMatch;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Rule based matching of a keyword against search hits. Hits whose labels do not correspond
 * to the keyword are dropped, the remaining ones are sorted by decreasing score.
 */
@Service
public class MatchingService {

    /** Qualifiers like "Bank &lt;Möbel&gt;" (GND) or "Zelle (Biologie)". */
    private static final Pattern QUALIFIER = Pattern.compile("\\s*[<(\\[][^>)\\]]*[>)\\]]");
    private static final Pattern SEPARATORS = Pattern.compile("[\\p{Pd}_\\s]+");

    public List<NormdataMatch> match(String term, List<NormdataCandidate> candidates) {
        Set<String> seenUris = new HashSet<>();
        return candidates.stream()
                .flatMap(candidate -> matchType(term, candidate)
                        .map(matchType -> toMatch(candidate, matchType))
                        .stream())
                .sorted(Comparator.comparingDouble(NormdataMatch::score).reversed())
                .filter(match -> match.uri() == null || seenUris.add(match.uri()))
                .toList();
    }

    Optional<MatchType> matchType(String term, NormdataCandidate candidate) {
        if (term.equals(candidate.label())) {
            return Optional.of(MatchType.EXACT);
        }
        if (candidate.alternativeLabels().contains(term)) {
            return Optional.of(MatchType.ALTERNATIVE_LABEL);
        }
        String normalizedTerm = normalize(term);
        if (candidate.label() != null && normalizedTerm.equals(normalize(candidate.label()))) {
            return Optional.of(MatchType.NORMALIZED);
        }
        boolean alternativeMatches = candidate.alternativeLabels().stream()
                .filter(Objects::nonNull)
                .map(MatchingService::normalize)
                .anyMatch(normalizedTerm::equals);
        return alternativeMatches ? Optional.of(MatchType.NORMALIZED_ALTERNATIVE_LABEL) : Optional.empty();
    }

    /** Ignores case, Unicode compatibility forms, hyphens, underscores, whitespace and qualifiers. */
    static String normalize(String label) {
        String normalized = Normalizer.normalize(label, Normalizer.Form.NFKC);
        normalized = QUALIFIER.matcher(normalized).replaceAll("");
        normalized = SEPARATORS.matcher(normalized).replaceAll(" ");
        return normalized.strip().toLowerCase(Locale.ROOT);
    }

    private static NormdataMatch toMatch(NormdataCandidate candidate, MatchType matchType) {
        return new NormdataMatch(candidate.source(), candidate.id(), candidate.uri(), candidate.label(),
                candidate.vocabulary(), candidate.types(), matchType, matchType.score());
    }
}
