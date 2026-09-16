package com.mysportsapp.integrations.strava.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Strava's OAuth token exchange/refresh response
 * (https://developers.strava.com/docs/authentication/). {@code athlete} is
 * only present on the initial code exchange, not on a refresh - null then.
 * {@code ignoreUnknown = true} because Strava's real payload carries many
 * more fields (on {@code athlete} especially) than this app has any use for.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record StravaTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("expires_at") long expiresAtEpochSeconds,
        StravaAthleteRef athlete
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StravaAthleteRef(long id) {
    }
}
