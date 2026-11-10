ALTER TABLE app.document_text_pages
    ADD COLUMN extraction_method VARCHAR(32) NOT NULL DEFAULT 'PDFBOX_TEXT';

ALTER TABLE app.document_text_pages
    ALTER COLUMN extraction_method DROP DEFAULT;
