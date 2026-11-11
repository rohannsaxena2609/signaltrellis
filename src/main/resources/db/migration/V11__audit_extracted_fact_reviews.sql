ALTER TABLE app.extracted_facts
    ADD CONSTRAINT uq_extracted_facts_id_workspace UNIQUE (id, workspace_id);

CREATE TABLE app.extracted_fact_reviews (
    id UUID PRIMARY KEY,
    fact_id UUID NOT NULL,
    workspace_id UUID NOT NULL,
    review_status VARCHAR(24) NOT NULL,
    previous_value VARCHAR(1000),
    corrected_value VARCHAR(1000),
    reviewed_by UUID NOT NULL,
    reviewed_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_fact_review_fact FOREIGN KEY (fact_id, workspace_id)
        REFERENCES app.extracted_facts(id, workspace_id) ON DELETE CASCADE,
    CONSTRAINT fk_fact_review_reviewer FOREIGN KEY (workspace_id, reviewed_by)
        REFERENCES app.workspaces(id, owner_id)
);

CREATE INDEX ix_extracted_fact_reviews_fact_time
    ON app.extracted_fact_reviews(fact_id, reviewed_at DESC);
