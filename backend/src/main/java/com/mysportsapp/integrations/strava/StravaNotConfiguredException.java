package com.mysportsapp.integrations.strava;

/**
 * Thrown when a Strava API app hasn't been registered yet (no
 * {@code STRAVA_CLIENT_ID}/{@code STRAVA_CLIENT_SECRET} configured) but a
 * user tries to connect anyway. Distinct from {@link StravaNotConnectedException}
 * - this one means the feature itself isn't usable yet on this deployment.
 */
public class StravaNotConfiguredException extends RuntimeException {

    public StravaNotConfiguredException() {
        super("Strava integration is not configured on this deployment");
    }
}
