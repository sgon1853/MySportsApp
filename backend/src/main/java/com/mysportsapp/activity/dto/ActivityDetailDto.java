package com.mysportsapp.activity.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ActivityDetailDto(
        UUID id,
        String activityType,
        String visualizationType,
        Instant startTime,
        long durationSeconds,
        Double distanceMeters,
        Integer avgHr,
        Integer maxHr,
        Integer calories,
        Double elevationGainMeters,
        String sourceProviderId,
        List<TrackPointDto> trackPoints
) {
}
