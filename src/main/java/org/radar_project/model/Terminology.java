package org.radar_project.model;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.EnumSet;
import java.util.Set;

/**
 * Where the extracted keywords are looked up; same values as radar-keyword-search's {@code terminology}.
 */
public enum Terminology {
    TIB("tib", EnumSet.of(NormdataSource.TIB)),
    GND("gnd", EnumSet.of(NormdataSource.GND)),
    BOTH("both", EnumSet.allOf(NormdataSource.class)),
    /** Keywords only, no normdata lookup. */
    NONE("none", EnumSet.noneOf(NormdataSource.class));

    private final String value;
    private final Set<NormdataSource> sources;

    Terminology(String value, Set<NormdataSource> sources) {
        this.value = value;
        this.sources = Set.copyOf(sources);
    }

    @JsonValue
    public String value() {
        return value;
    }

    /** The sources to search; empty for {@link #NONE}. */
    public Set<NormdataSource> sources() {
        return sources;
    }
}
