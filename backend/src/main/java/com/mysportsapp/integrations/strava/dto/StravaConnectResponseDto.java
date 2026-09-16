package com.mysportsapp.integrations.strava.dto;

/** Response for {@code POST /api/v1/integrations/strava/connect} - the frontend
 * navigates the browser to {@code authorizeUrl} to start the OAuth flow. */
public record StravaConnectResponseDto(String authorizeUrl) {
}
