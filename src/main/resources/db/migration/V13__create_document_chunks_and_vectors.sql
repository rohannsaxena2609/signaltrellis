CREATE EXTENSION IF NOT EXISTS vector;

ALTER TABLE app.document_extractions
    ADD CONSTRAINT uq_document_extraction_id_document_workspace UNIQUE (id, document_id, workspace_id);

ALTER TABLE app.document_text_pages
    ADD CONSTRAINT uq_document_text_page_extraction_page_workspace
    UNIQUE (extraction_id, page_number, workspace_id);

CREATE TABLE app.document_chunks (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL,
    document_id UUID NOT NULL,
    extraction_id UUID NOT NULL,
    page_number INTEGER NOT NULL,
    chunk_index INTEGER NOT NULL CHECK (chunk_index >= 0),
    start_character INTEGER NOT NULL CHECK (start_character >= 0),
    end_character INTEGER NOT NULL CHECK (end_character > start_character),
    content TEXT NOT NULL,
    content_sha256 CHAR(64) NOT NULL,
    search_vector TSVECTOR GENERATED ALWAYS AS (to_tsvector('simple', content)) STORED,
    embedding HALFvec(3072),
    embedding_model VARCHAR(120),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_document_chunk_position UNIQUE (extraction_id, page_number, chunk_index),
    CONSTRAINT fk_document_chunk_extraction FOREIGN KEY (extraction_id, document_id, workspace_id)
        REFERENCES app.document_extractions(id, document_id, workspace_id) ON DELETE CASCADE,
    CONSTRAINT fk_document_chunk_page FOREIGN KEY (extraction_id, page_number, workspace_id)
        REFERENCES app.document_text_pages(extraction_id, page_number, workspace_id) ON DELETE CASCADE,
    CONSTRAINT ck_document_chunk_embedding_metadata CHECK (
        (embedding IS NULL AND embedding_model IS NULL)
        OR (embedding IS NOT NULL AND embedding_model IS NOT NULL)
    )
);

CREATE INDEX ix_document_chunks_workspace_document
    ON app.document_chunks(workspace_id, document_id, page_number, chunk_index);
CREATE INDEX ix_document_chunks_search_vector
    ON app.document_chunks USING GIN(search_vector);
CREATE INDEX ix_document_chunks_embedding_hnsw
    ON app.document_chunks USING HNSW(embedding HALFvec_cosine_ops)
    WHERE embedding IS NOT NULL;
