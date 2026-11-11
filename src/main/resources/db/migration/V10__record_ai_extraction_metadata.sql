ALTER TABLE app.document_extractions
    ADD COLUMN ai_status VARCHAR(24) NOT NULL DEFAULT 'NOT_CONFIGURED',
    ADD COLUMN ai_model VARCHAR(120),
    ADD COLUMN ai_input_characters INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN ai_failure_reason VARCHAR(500);

ALTER TABLE app.document_extractions
    ADD CONSTRAINT ck_document_extraction_ai_input CHECK (ai_input_characters >= 0);
