ALTER TABLE app.document_chunks
    ALTER COLUMN content_sha256 TYPE VARCHAR(64)
    USING rtrim(content_sha256)::VARCHAR(64);
