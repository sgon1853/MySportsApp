CREATE TABLE users (
    id                       UUID PRIMARY KEY,
    email                    VARCHAR(255) NOT NULL,
    password_hash            VARCHAR(255) NOT NULL,
    role                     VARCHAR(20)  NOT NULL,
    active                   BOOLEAN      NOT NULL DEFAULT false,
    invited_by               UUID         NULL,
    invite_token             VARCHAR(255) NULL,
    invite_token_expires_at  TIMESTAMPTZ  NULL,
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_users_email ON users (email);
CREATE UNIQUE INDEX ux_users_invite_token ON users (invite_token) WHERE invite_token IS NOT NULL;
