package org.radar_project.model;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Normdata sources that can be searched.
 */
public enum NormdataSource {
    GND("gnd"),
    TIB("tib");

    private final String value;

    NormdataSource(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }
}
