CREATE TABLE activities (
    id                      UUID PRIMARY KEY,
    user_id                 UUID          NOT NULL REFERENCES users (id),
    source_provider_id      VARCHAR(100)  NOT NULL,
    source_import_batch_id  UUID          NOT NULL REFERENCES import_batches (id),
    activity_type           VARCHAR(50)   NOT NULL,
    start_time              TIMESTAMPTZ   NOT NULL,
    duration_seconds        BIGINT        NOT NULL,
    distance_meters         DOUBLE PRECISION NULL,
    avg_hr                  INT           NULL,
    max_hr                  INT           NULL,
    calories                INT           NULL,
    elevation_gain_meters   DOUBLE PRECISION NULL,
    track_points            JSONB         NOT NULL,
    dedup_key               VARCHAR(64)   NOT NULL,
    created_at              TIMESTAMPTZ   NOT NULL DEFAULT now(),
    UNIQUE (user_id, dedup_key)
);

CREATE INDEX ix_activities_user_id ON activities (user_id);
CREATE INDEX ix_activities_user_id_start_time ON activities (user_id, start_time);
