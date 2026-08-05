CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE audit_event (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(120) NOT NULL,
    actor_id VARCHAR(120),
    tenant_id VARCHAR(120),
    project_id UUID,
    resource_type VARCHAR(120),
    resource_id VARCHAR(120),
    action VARCHAR(120) NOT NULL,
    correlation_id VARCHAR(120),
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX idx_audit_event_project_occurred ON audit_event (project_id, occurred_at DESC);
CREATE INDEX idx_audit_event_correlation ON audit_event (correlation_id);

CREATE TABLE outbox_event (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(160) NOT NULL,
    aggregate_type VARCHAR(120) NOT NULL,
    aggregate_id VARCHAR(120) NOT NULL,
    tenant_id VARCHAR(120),
    project_id UUID,
    actor_id VARCHAR(120),
    correlation_id VARCHAR(120),
    causation_id VARCHAR(120),
    idempotency_key VARCHAR(160),
    schema_version INTEGER NOT NULL DEFAULT 1,
    payload JSONB NOT NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'PENDING',
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX ux_outbox_idempotency_key ON outbox_event (idempotency_key) WHERE idempotency_key IS NOT NULL;
CREATE INDEX idx_outbox_status_occurred ON outbox_event (status, occurred_at);

CREATE TABLE async_job (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_type VARCHAR(160) NOT NULL,
    idempotency_key VARCHAR(160),
    tenant_id VARCHAR(120),
    project_id UUID,
    status VARCHAR(40) NOT NULL DEFAULT 'PENDING',
    attempts INTEGER NOT NULL DEFAULT 0,
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    last_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version INTEGER NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX ux_async_job_idempotency_key ON async_job (idempotency_key) WHERE idempotency_key IS NOT NULL;
CREATE INDEX idx_async_job_status_created ON async_job (status, created_at);

CREATE TABLE evidence_record (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(120),
    project_id UUID NOT NULL,
    evidence_type VARCHAR(80) NOT NULL,
    storage_provider VARCHAR(80) NOT NULL,
    object_reference VARCHAR(512) NOT NULL,
    content_hash VARCHAR(128),
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_by VARCHAR(120),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_evidence_project_created ON evidence_record (project_id, created_at DESC);

