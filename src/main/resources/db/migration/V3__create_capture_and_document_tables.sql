CREATE TABLE app.source_items (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES app.workspaces(id),
    source_type VARCHAR(24) NOT NULL,
    status VARCHAR(24) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX ix_source_items_workspace_created
    ON app.source_items(workspace_id, created_at DESC);

CREATE TABLE app.documents (
    id UUID PRIMARY KEY,
    source_item_id UUID NOT NULL UNIQUE REFERENCES app.source_items(id) ON DELETE CASCADE,
    storage_key UUID NOT NULL UNIQUE,
    detected_content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL CHECK (size_bytes > 0),
    checksum_sha256 CHAR(64) NOT NULL,
    category VARCHAR(32) NOT NULL,
    processing_status VARCHAR(24) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX ix_documents_category_status
    ON app.documents(category, processing_status);
