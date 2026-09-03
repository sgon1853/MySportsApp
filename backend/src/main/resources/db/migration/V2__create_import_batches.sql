CREATE TABLE import_batches (
    id                  UUID PRIMARY KEY,
    user_id             UUID         NOT NULL REFERENCES users (id),
    provider_id         VARCHAR(100) NOT NULL,
    original_filename   VARCHAR(500) NOT NULL,
    status              VARCHAR(20)  NOT NULL,
    records_parsed      INT          NOT NULL DEFAULT 0,
    records_inserted    INT          NOT NULL DEFAULT 0,
    records_deduped     INT          NOT NULL DEFAULT 0,
    records_failed       INT         NOT NULL DEFAULT 0,
    error_details       TEXT         NULL,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX ix_import_batches_user_id ON import_batches (user_id);
