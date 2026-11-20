package org.radar_project.model;

import java.util.List;

/**
 * A raw search hit from a normdata source, before matching.
 *
 * @param id                identifier within the source (GND id, TIB short form)
 * @param uri               URI of the record
 * @param alternativeLabels variant names (GND) or synonyms (TIB)
 * @param vocabulary        ontology of a TIB hit, null for GND
 * @param types             record types as delivered by the source
 */
public record NormdataCandidate(
        NormdataSource source,
        String id,
        String uri,
        String label,
        List<String> alternativeLabels,
        String vocabulary,
        List<String> types) {

    public NormdataCandidate {
        alternativeLabels = alternativeLabels == null ? List.of() : List.copyOf(alternativeLabels);
        types = types == null ? List.of() : List.copyOf(types);
    }
}
