package com.mysportsapp.provider.spi;

import java.time.Instant;
import java.util.List;

/**
 * A single activity (a run, a ride, ...) as extracted from a raw provider
 * file, before any persistence or dedup concerns are applied.
 */
public record ParsedActivity(
        String activityType,
        Instant startTime,
        long durationSeconds,
        Double distanceMeters,
        Integer avgHr,
        Integer maxHr,
        Integer calories,
        Double elevationGainMeters,
        List<ParsedTrackPoint> trackPoints
) {
}
