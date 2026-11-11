CREATE TABLE app.extracted_fact_context_links (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL,
    fact_id UUID NOT NULL,
    target_type VARCHAR(24) NOT NULL,
    target_id UUID NOT NULL,
    linked_by UUID NOT NULL,
    linked_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_extracted_fact_context_link UNIQUE (workspace_id, fact_id, target_type, target_id),
    CONSTRAINT fk_fact_context_link_fact FOREIGN KEY (fact_id, workspace_id)
        REFERENCES app.extracted_facts(id, workspace_id) ON DELETE CASCADE,
    CONSTRAINT fk_fact_context_link_actor FOREIGN KEY (workspace_id, linked_by)
        REFERENCES app.workspaces(id, owner_id)
);

CREATE INDEX ix_fact_context_links_target
    ON app.extracted_fact_context_links(workspace_id, target_type, target_id);
