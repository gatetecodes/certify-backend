CREATE TABLE IF NOT EXISTS certificate_jobs (
    id                uuid PRIMARY KEY,
    tenant_id         uuid        NOT NULL,
    template_id       uuid        NOT NULL,
    request_data_json jsonb       NOT NULL,
    status            varchar(32) NOT NULL,
    certificate_id    uuid,
    requested_by      varchar(320) NOT NULL,
    error_message     text,
    created_at        timestamptz NOT NULL,
    updated_at        timestamptz NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_certificate_jobs_tenant ON certificate_jobs (tenant_id);
CREATE INDEX IF NOT EXISTS idx_certificate_jobs_status_created_at ON certificate_jobs (status, created_at);
