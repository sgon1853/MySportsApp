package com.mysportsapp.integrations.strava;

/**
 * Thrown when the acting user asks to sync but has no {@link StravaConnection}
 * yet - they need to connect their account first.
 */
public class StravaNotConnectedException extends RuntimeException {

    public StravaNotConnectedException() {
        super("No Strava account is connected for this user");
    }
}
