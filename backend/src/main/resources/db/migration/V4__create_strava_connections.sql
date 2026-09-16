CREATE TABLE strava_connections (
    id                  UUID PRIMARY KEY,
    user_id             UUID          NOT NULL UNIQUE REFERENCES users (id),
    strava_athlete_id   BIGINT        NOT NULL UNIQUE,
    access_token        VARCHAR(500)  NOT NULL,
    refresh_token       VARCHAR(500)  NOT NULL,
    token_expires_at    TIMESTAMPTZ   NOT NULL,
    connected_at        TIMESTAMPTZ   NOT NULL DEFAULT now()
);
