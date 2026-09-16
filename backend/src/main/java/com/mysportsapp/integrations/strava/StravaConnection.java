package com.mysportsapp.integrations.strava;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * One user's link to their Strava account. There is at most one of these per
 * {@code user_id} (enforced at the DB level) and at most one per
 * {@code strava_athlete_id} - a given Strava account can't be linked to two
 * MySportsApp users.
 *
 * <p>{@code access_token}/{@code refresh_token} are real credentials to the
 * user's Strava account and are encrypted at rest via
 * {@link EncryptedStringConverter} - the value in this object is always the
 * plaintext token; the converter handles encryption transparently at the
 * JPA/JDBC boundary.
 */
@Entity
@Table(name = "strava_connections")
public class StravaConnection {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "strava_athlete_id", nullable = false, unique = true)
    private long stravaAthleteId;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "access_token", nullable = false)
    private String accessToken;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "refresh_token", nullable = false)
    private String refreshToken;

    @Column(name = "token_expires_at", nullable = false)
    private Instant tokenExpiresAt;

    @Column(name = "connected_at", nullable = false, updatable = false)
    private Instant connectedAt;

    protected StravaConnection() {
        // JPA
    }

    public StravaConnection(UUID id, UUID userId, long stravaAthleteId, String accessToken,
                             String refreshToken, Instant tokenExpiresAt, Instant connectedAt) {
        this.id = id;
        this.userId = userId;
        this.stravaAthleteId = stravaAthleteId;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenExpiresAt = tokenExpiresAt;
        this.connectedAt = connectedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public long getStravaAthleteId() {
        return stravaAthleteId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public Instant getTokenExpiresAt() {
        return tokenExpiresAt;
    }

    public Instant getConnectedAt() {
        return connectedAt;
    }

    /** Called after a token refresh - Strava issues a new access token, a new
     * refresh token, and a new expiry together as one unit. */
    public void updateTokens(String accessToken, String refreshToken, Instant tokenExpiresAt) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenExpiresAt = tokenExpiresAt;
    }
}
