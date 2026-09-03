package com.mysportsapp.activity.dto;

import java.time.Instant;

public record TrackPointDto(
        Instant timestamp,
        Double lat,
        Double lon,
        Double elevationMeters,
        Integer heartRate
) {
}
