package com.mysportsapp.integrations.strava;

import com.mysportsapp.integrations.strava.dto.StravaStreamSet;
import com.mysportsapp.integrations.strava.dto.StravaSummaryActivity;
import com.mysportsapp.integrations.strava.dto.StravaTokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Thin wrapper over the two Strava endpoints Phase 1 needs. Refreshes the
 * access token first if it's expired or close to it (Strava's tokens are
 * short-lived - a matter of hours), persisting the new tokens back to the
 * {@link StravaConnection} so the next call doesn't have to refresh again.
 *
 * <p>Deliberately does not fetch calories: that's only present on Strava's
 * "detailed" single-activity representation ({@code GET /activities/{id}}),
 * a third API call per activity this phase skips to keep the request budget
 * at two calls per activity (summary list + streams) - well within Strava's
 * 200-per-15-minutes limit at personal-account activity volumes, and
 * consistent with the GPX provider, which also never populates calories.
 */
@Component
public class StravaApiClient {

    private static final String API_BASE = "https://www.strava.com/api/v3";
    private static final String TOKEN_URL = "https://www.strava.com/oauth/token";
    private static final Duration REFRESH_MARGIN = Duration.ofMinutes(5);
    private static final int PAGE_SIZE = 200;

    private final String clientId;
    private final String clientSecret;
    private final StravaConnectionRepository connectionRepository;
    private final RestClient restClient;

    public StravaApiClient(
            @Value("${app.strava.client-id}") String clientId,
            @Value("${app.strava.client-secret}") String clientSecret,
            StravaConnectionRepository connectionRepository) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.connectionRepository = connectionRepository;
        this.restClient = RestClient.create();
    }

    /** One page of the athlete's activities, newest first (Strava's default
     * order), up to {@value PAGE_SIZE} per page (Strava's own max). Returns an
     * empty list once {@code page} is past the end of the athlete's history -
     * callers page until they see that. */
    public List<StravaSummaryActivity> listActivities(StravaConnection connection, int page) {
        String accessToken = accessTokenFor(connection);
        StravaSummaryActivity[] activities = restClient.get()
                .uri(API_BASE + "/athlete/activities?page={page}&per_page={perPage}", page, PAGE_SIZE)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(StravaSummaryActivity[].class);
        return activities == null ? List.of() : List.of(activities);
    }

    public StravaStreamSet getStreams(StravaConnection connection, long activityId) {
        String accessToken = accessTokenFor(connection);
        StravaStreamSet streams = restClient.get()
                .uri(API_BASE + "/activities/{id}/streams?keys=latlng,altitude,heartrate,time&key_by_type=true",
                        activityId)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(StravaStreamSet.class);
        return streams == null ? new StravaStreamSet(null, null, null, null) : streams;
    }

    private String accessTokenFor(StravaConnection connection) {
        if (connection.getTokenExpiresAt().isAfter(Instant.now().plus(REFRESH_MARGIN))) {
            return connection.getAccessToken();
        }
        return refresh(connection);
    }

    @Transactional
    String refresh(StravaConnection connection) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("refresh_token", connection.getRefreshToken());
        form.add("grant_type", "refresh_token");

        StravaTokenResponse response = restClient.post()
                .uri(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(StravaTokenResponse.class);

        if (response == null) {
            throw new IllegalStateException("Strava token refresh did not return the expected response");
        }

        Instant expiresAt = Instant.ofEpochSecond(response.expiresAtEpochSeconds());
        connection.updateTokens(response.accessToken(), response.refreshToken(), expiresAt);
        connectionRepository.save(connection);
        return response.accessToken();
    }
}
