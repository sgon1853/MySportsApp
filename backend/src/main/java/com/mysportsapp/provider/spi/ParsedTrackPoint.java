package com.mysportsapp.provider.spi;

import java.time.Instant;

/**
 * A single GPS/sensor sample within a {@link ParsedActivity}.
 */
public record ParsedTrackPoint(
        Instant timestamp,
        Double lat,
        Double lon,
        Double elevationMeters,
        Integer heartRate
) {
}
