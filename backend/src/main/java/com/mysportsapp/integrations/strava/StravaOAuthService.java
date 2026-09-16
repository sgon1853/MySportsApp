package com.mysportsapp.integrations.strava;

import com.mysportsapp.integrations.strava.dto.StravaTokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Drives the OAuth2 authorization-code flow with Strava. Splits into two
 * halves because of how this happens across the browser:
 *
 * <p>1. {@link #buildAuthorizeUrl} is called from a normal *authenticated*
 * XHR (the frontend's "Connect to Strava" button) - the app already knows
 * which user this is for, the same way every other endpoint does.
 *
 * <p>2. Strava's own redirect back to {@code STRAVA_REDIRECT_URI} is a plain
 * *browser navigation* with no way to attach this app's JWT (it's stored in
 * localStorage, not a cookie - see {@code frontend/src/api/client.ts}). The
 * OAuth {@code state} parameter is what survives that round trip, so it
 * doubles as the thread back to "which user initiated this" - a short-lived,
 * in-memory, single-use mapping from a random state value to the user id
 * that requested it. A table would be overkill at this scale and for
 * something this short-lived (a few minutes at most).
 */
@Service
public class StravaOAuthService {

    private static final Duration STATE_TTL = Duration.ofMinutes(10);
    private static final String AUTHORIZE_URL = "https://www.strava.com/oauth/authorize";
    private static final String TOKEN_URL = "https://www.strava.com/oauth/token";

    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final StravaConnectionRepository connectionRepository;
    private final RestClient restClient;
    private final Map<String, PendingState> pendingStates = new ConcurrentHashMap<>();

    public StravaOAuthService(
            @Value("${app.strava.client-id}") String clientId,
            @Value("${app.strava.client-secret}") String clientSecret,
            @Value("${app.strava.redirect-uri}") String redirectUri,
            StravaConnectionRepository connectionRepository) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.connectionRepository = connectionRepository;
        this.restClient = RestClient.create();
    }

    private boolean isConfigured() {
        return !clientId.isBlank() && !clientSecret.isBlank();
    }

    public String buildAuthorizeUrl(UUID userId) {
        if (!isConfigured()) {
            throw new StravaNotConfiguredException();
        }

        String state = UUID.randomUUID().toString();
        pendingStates.put(state, new PendingState(userId, Instant.now().plus(STATE_TTL)));
        expireOldStates();

        return UriComponentsBuilder.fromHttpUrl(AUTHORIZE_URL)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("approval_prompt", "auto")
                // activity:read_all (not just activity:read) is required to read
                // private activities, which most personal Strava accounts have at
                // least some of.
                .queryParam("scope", "read,activity:read_all")
                .queryParam("state", state)
                .build()
                .toUriString();
    }

    /**
     * Exchanges the authorization code Strava's redirect carried for tokens,
     * resolves {@code state} back to the user who initiated the connection,
     * and saves the {@link StravaConnection}. One connection per user: a
     * second connect attempt replaces the previous tokens for that user
     * rather than erroring, since re-connecting is a reasonable way to
     * recover from a revoked/expired refresh token.
     *
     * @throws IllegalArgumentException if {@code state} is unknown or expired
     */
    @Transactional
    public void completeConnection(String code, String state) {
        PendingState pending = pendingStates.remove(state);
        if (pending == null || pending.expiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Invalid or expired Strava OAuth state");
        }

        // Strava's token endpoint follows OAuth2's RFC 6749 §4.1.3, which
        // requires application/x-www-form-urlencoded, not JSON - a plain Map
        // body would default to JSON via Spring's Jackson converter, which
        // Strava would reject. MultiValueMap + explicit content type is what
        // actually triggers form encoding.
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("code", code);
        form.add("grant_type", "authorization_code");

        StravaTokenResponse tokenResponse = restClient.post()
                .uri(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(StravaTokenResponse.class);

        if (tokenResponse == null || tokenResponse.athlete() == null) {
            throw new IllegalStateException("Strava token exchange did not return the expected response");
        }

        Instant expiresAt = Instant.ofEpochSecond(tokenResponse.expiresAtEpochSeconds());
        long athleteId = tokenResponse.athlete().id();

        StravaConnection connection = connectionRepository.findByUserId(pending.userId())
                .orElse(null);
        if (connection != null) {
            connection.updateTokens(tokenResponse.accessToken(), tokenResponse.refreshToken(), expiresAt);
        } else {
            connection = new StravaConnection(
                    UUID.randomUUID(), pending.userId(), athleteId,
                    tokenResponse.accessToken(), tokenResponse.refreshToken(), expiresAt, Instant.now());
        }
        connectionRepository.save(connection);
    }

    private void expireOldStates() {
        Instant now = Instant.now();
        pendingStates.values().removeIf(p -> p.expiresAt().isBefore(now));
    }

    private record PendingState(UUID userId, Instant expiresAt) {
    }
}
