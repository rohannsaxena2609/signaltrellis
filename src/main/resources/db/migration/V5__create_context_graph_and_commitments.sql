CREATE TABLE app.life_entities (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES app.workspaces(id),
    entity_type VARCHAR(32) NOT NULL,
    name VARCHAR(160) NOT NULL,
    description VARCHAR(2000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_life_entities_id_workspace UNIQUE (id, workspace_id)
);

CREATE UNIQUE INDEX uq_life_entities_workspace_type_name
    ON app.life_entities(workspace_id, entity_type, lower(name));

CREATE TABLE app.entity_relationships (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES app.workspaces(id),
    source_entity_id UUID NOT NULL,
    target_entity_id UUID NOT NULL,
    relationship_type VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_relationship_distinct_entities CHECK (source_entity_id <> target_entity_id),
    CONSTRAINT fk_relationship_source_workspace FOREIGN KEY (source_entity_id, workspace_id)
        REFERENCES app.life_entities(id, workspace_id) ON DELETE CASCADE,
    CONSTRAINT fk_relationship_target_workspace FOREIGN KEY (target_entity_id, workspace_id)
        REFERENCES app.life_entities(id, workspace_id) ON DELETE CASCADE,
    CONSTRAINT uq_entity_relationship UNIQUE (workspace_id, source_entity_id, target_entity_id, relationship_type)
);

CREATE INDEX ix_relationship_source ON app.entity_relationships(workspace_id, source_entity_id);
CREATE INDEX ix_relationship_target ON app.entity_relationships(workspace_id, target_entity_id);

CREATE TABLE app.life_events (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES app.workspaces(id),
    entity_id UUID,
    event_type VARCHAR(32) NOT NULL,
    title VARCHAR(160) NOT NULL,
    event_date DATE NOT NULL,
    notes VARCHAR(2000),
    status VARCHAR(24) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_life_event_entity_workspace FOREIGN KEY (entity_id, workspace_id)
        REFERENCES app.life_entities(id, workspace_id) ON DELETE SET NULL (entity_id)
);

CREATE INDEX ix_life_events_workspace_date ON app.life_events(workspace_id, event_date);

CREATE TABLE app.commitments (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES app.workspaces(id),
    entity_id UUID,
    title VARCHAR(160) NOT NULL,
    amount NUMERIC(14,2),
    currency VARCHAR(3),
    due_date DATE NOT NULL,
    status VARCHAR(24) NOT NULL,
    notes VARCHAR(2000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_commitment_amount_currency CHECK ((amount IS NULL) = (currency IS NULL)),
    CONSTRAINT ck_commitment_nonnegative_amount CHECK (amount IS NULL OR amount >= 0),
    CONSTRAINT fk_commitment_entity_workspace FOREIGN KEY (entity_id, workspace_id)
        REFERENCES app.life_entities(id, workspace_id) ON DELETE SET NULL (entity_id)
);

CREATE INDEX ix_commitments_workspace_due_status ON app.commitments(workspace_id, due_date, status);
