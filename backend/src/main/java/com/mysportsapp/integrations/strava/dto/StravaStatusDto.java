package com.mysportsapp.integrations.strava.dto;

import java.time.Instant;

/** Response for {@code GET /api/v1/integrations/strava/status}. {@code connectedAt}
 * is null when {@code connected} is false. */
public record StravaStatusDto(boolean connected, Instant connectedAt) {
}
