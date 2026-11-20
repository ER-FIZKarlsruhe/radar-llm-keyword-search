package org.radar_project.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Settings for the normdata search.
 *
 * @param maxConcurrentRequests upper bound of simultaneous requests to GND and TIB, shared by all incoming requests
 */
@Validated
@ConfigurationProperties("radar.matching")
public record MatchingProperties(@Min(1) int maxConcurrentRequests) {
}
