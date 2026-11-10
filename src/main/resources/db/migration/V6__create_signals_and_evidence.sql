ALTER TABLE app.workspaces
    ADD CONSTRAINT uq_workspaces_id_owner UNIQUE (id, owner_id);

ALTER TABLE app.life_events
    ADD CONSTRAINT uq_life_events_id_workspace UNIQUE (id, workspace_id);

ALTER TABLE app.commitments
    ADD CONSTRAINT uq_commitments_id_workspace UNIQUE (id, workspace_id);

CREATE TABLE app.signals (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES app.workspaces(id),
    signal_type VARCHAR(40) NOT NULL,
    priority VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    dedupe_key VARCHAR(160) NOT NULL,
    title VARCHAR(160) NOT NULL,
    explanation VARCHAR(1000) NOT NULL,
    detected_at TIMESTAMPTZ NOT NULL,
    last_detected_at TIMESTAMPTZ NOT NULL,
    last_evaluated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_signals_id_workspace UNIQUE (id, workspace_id),
    CONSTRAINT uq_signals_workspace_dedupe UNIQUE (workspace_id, dedupe_key)
);

CREATE INDEX ix_signals_workspace_status_priority
    ON app.signals(workspace_id, status, priority, last_detected_at DESC);

CREATE TABLE app.signal_evidence (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL,
    signal_id UUID NOT NULL,
    evidence_type VARCHAR(20) NOT NULL,
    event_id UUID,
    commitment_id UUID,
    summary VARCHAR(500) NOT NULL,
    observed_value VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_signal_evidence_signal FOREIGN KEY (signal_id, workspace_id)
        REFERENCES app.signals(id, workspace_id) ON DELETE CASCADE,
    CONSTRAINT fk_signal_evidence_event FOREIGN KEY (event_id, workspace_id)
        REFERENCES app.life_events(id, workspace_id) ON DELETE CASCADE,
    CONSTRAINT fk_signal_evidence_commitment FOREIGN KEY (commitment_id, workspace_id)
        REFERENCES app.commitments(id, workspace_id) ON DELETE CASCADE,
    CONSTRAINT ck_signal_evidence_source CHECK (
        (evidence_type = 'EVENT' AND event_id IS NOT NULL AND commitment_id IS NULL)
        OR (evidence_type = 'COMMITMENT' AND commitment_id IS NOT NULL AND event_id IS NULL)
    )
);

CREATE INDEX ix_signal_evidence_signal ON app.signal_evidence(workspace_id, signal_id);

CREATE TABLE app.signal_history (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL,
    signal_id UUID NOT NULL,
    actor_user_id UUID NOT NULL,
    event_type VARCHAR(20) NOT NULL,
    previous_status VARCHAR(16),
    new_status VARCHAR(16) NOT NULL,
    detail VARCHAR(500),
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_signal_history_signal FOREIGN KEY (signal_id, workspace_id)
        REFERENCES app.signals(id, workspace_id) ON DELETE CASCADE,
    CONSTRAINT fk_signal_history_actor_owner FOREIGN KEY (workspace_id, actor_user_id)
        REFERENCES app.workspaces(id, owner_id)
);

CREATE INDEX ix_signal_history_signal ON app.signal_history(workspace_id, signal_id, occurred_at);
