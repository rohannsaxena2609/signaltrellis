CREATE TABLE app.document_processing_jobs (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL UNIQUE REFERENCES app.documents(id) ON DELETE CASCADE,
    status VARCHAR(24) NOT NULL CHECK (status IN ('QUEUED', 'PROCESSING', 'PROCESSED', 'FAILED')),
    attempt_count INTEGER NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lease_expires_at TIMESTAMPTZ,
    last_error VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_document_processing_job_lease CHECK (
        (status = 'PROCESSING' AND lease_expires_at IS NOT NULL)
        OR (status <> 'PROCESSING' AND lease_expires_at IS NULL)
    )
);

CREATE INDEX ix_document_processing_jobs_claim
    ON app.document_processing_jobs(status, next_attempt_at, created_at);
CREATE INDEX ix_document_processing_jobs_lease
    ON app.document_processing_jobs(lease_expires_at)
    WHERE status = 'PROCESSING';
