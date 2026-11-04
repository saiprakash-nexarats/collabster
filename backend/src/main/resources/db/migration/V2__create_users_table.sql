-- V2: Create users table if it does not already exist.
-- This table is owned by Madhuri's auth service. Communities module only reads from it
-- to resolve user identity from JWT tokens.
-- If Madhuri's service has already created this table, this migration is a safe no-op.

CREATE TABLE IF NOT EXISTS users (
    id                BIGSERIAL PRIMARY KEY,
    first_name        VARCHAR(50)  NOT NULL,
    last_name         VARCHAR(50)  NOT NULL,
    email             VARCHAR(100) NOT NULL UNIQUE,
    password          VARCHAR(255),
    profile_picture   VARCHAR(500),
    provider          VARCHAR(20)  NOT NULL DEFAULT 'LOCAL',
    email_verified    BOOLEAN      NOT NULL DEFAULT FALSE,
    account_enabled   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    date_of_birth     DATE,
    gender            VARCHAR(20),
    profile_completed BOOLEAN      NOT NULL DEFAULT FALSE,
    username          VARCHAR(30)  UNIQUE,
    country           VARCHAR(100),
    city              VARCHAR(100),
    bio               VARCHAR(300)
);
