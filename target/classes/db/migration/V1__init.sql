-- Initial Flyway migration creating core tables for the certificate
-- management domain model.

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS tenants (
    id          uuid PRIMARY KEY,
    name        varchar(255) NOT NULL UNIQUE,
    slug        varchar(255) NOT NULL UNIQUE,
    created_at  timestamptz  NOT NULL,
    updated_at  timestamptz  NOT NULL
);

CREATE TABLE IF NOT EXISTS users (
    id            uuid PRIMARY KEY,
    tenant_id     uuid NOT NULL,
    email         varchar(320) NOT NULL UNIQUE,
    password_hash varchar(255) NOT NULL,
    role          varchar(32)  NOT NULL,
    full_name     varchar(255) NOT NULL,
    created_at    timestamptz  NOT NULL,
    updated_at    timestamptz  NOT NULL
);

CREATE TABLE IF NOT EXISTS certificate_templates (
    id                uuid PRIMARY KEY,
    tenant_id         uuid        NOT NULL,
    name              varchar(255) NOT NULL,
    description       varchar(1024),
    html_template     text        NOT NULL,
    placeholders_json jsonb       NOT NULL,
    active            boolean     NOT NULL,
    version           int         NOT NULL,
    created_at        timestamptz NOT NULL,
    updated_at        timestamptz NOT NULL
);

CREATE TABLE IF NOT EXISTS certificates (
    id           uuid PRIMARY KEY,
    tenant_id    uuid        NOT NULL,
    template_id  uuid        NOT NULL,
    data_json    jsonb       NOT NULL,
    status       varchar(32) NOT NULL,
    storage_path varchar(1024) NOT NULL,
    hash         varchar(128)  NOT NULL,
    created_by   varchar(320)  NOT NULL,
    created_at   timestamptz   NOT NULL,
    updated_at   timestamptz   NOT NULL
);

CREATE TABLE IF NOT EXISTS certificate_verification_tokens (
    public_id     uuid PRIMARY KEY,
    certificate_id uuid      NOT NULL REFERENCES certificates(id),
    checksum      varchar(128) NOT NULL,
    expires_at    timestamptz,
    created_at    timestamptz NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_templates_tenant ON certificate_templates (tenant_id);
CREATE INDEX IF NOT EXISTS idx_certificates_tenant ON certificates (tenant_id);
CREATE INDEX IF NOT EXISTS idx_users_tenant ON users (tenant_id);
