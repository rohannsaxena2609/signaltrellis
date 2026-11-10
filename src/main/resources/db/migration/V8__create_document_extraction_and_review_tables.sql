CREATE TABLE app.document_extractions (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL UNIQUE REFERENCES app.documents(id) ON DELETE CASCADE,
    workspace_id UUID NOT NULL REFERENCES app.workspaces(id),
    status VARCHAR(24) NOT NULL,
    extractor VARCHAR(80) NOT NULL,
    extracted_character_count INTEGER NOT NULL DEFAULT 0,
    failure_reason VARCHAR(500),
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    CONSTRAINT uq_document_extractions_id_workspace UNIQUE (id, workspace_id)
);

CREATE TABLE app.document_text_pages (
    id UUID PRIMARY KEY,
    extraction_id UUID NOT NULL,
    workspace_id UUID NOT NULL,
    page_number INTEGER NOT NULL CHECK (page_number > 0),
    content TEXT NOT NULL,
    character_count INTEGER NOT NULL CHECK (character_count >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_document_text_page UNIQUE (extraction_id, page_number),
    CONSTRAINT fk_document_text_page_extraction FOREIGN KEY (extraction_id, workspace_id)
        REFERENCES app.document_extractions(id, workspace_id) ON DELETE CASCADE
);

CREATE TABLE app.extracted_facts (
    id UUID PRIMARY KEY,
    extraction_id UUID NOT NULL,
    workspace_id UUID NOT NULL,
    fact_type VARCHAR(60) NOT NULL,
    extracted_value VARCHAR(1000) NOT NULL,
    source_page INTEGER NOT NULL CHECK (source_page > 0),
    source_excerpt VARCHAR(1000) NOT NULL,
    confidence NUMERIC(4,3),
    review_status VARCHAR(24) NOT NULL,
    corrected_value VARCHAR(1000),
    reviewed_by UUID,
    reviewed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_extracted_fact_page FOREIGN KEY (extraction_id, source_page)
        REFERENCES app.document_text_pages(extraction_id, page_number) ON DELETE CASCADE,
    CONSTRAINT fk_extracted_fact_extraction FOREIGN KEY (extraction_id, workspace_id)
        REFERENCES app.document_extractions(id, workspace_id) ON DELETE CASCADE,
    CONSTRAINT fk_extracted_fact_reviewer FOREIGN KEY (workspace_id, reviewed_by)
        REFERENCES app.workspaces(id, owner_id),
    CONSTRAINT ck_extracted_fact_confidence CHECK (confidence IS NULL OR confidence BETWEEN 0 AND 1)
);

CREATE INDEX ix_document_extractions_workspace_status
    ON app.document_extractions(workspace_id, status, started_at DESC);
CREATE INDEX ix_document_text_pages_extraction
    ON app.document_text_pages(extraction_id, page_number);
CREATE INDEX ix_extracted_facts_extraction_review
    ON app.extracted_facts(extraction_id, review_status);
