CREATE TABLE app.action_proposals (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES app.workspaces(id),
    signal_id UUID,
    requested_by UUID NOT NULL,
    action_type VARCHAR(40) NOT NULL,
    title VARCHAR(160) NOT NULL,
    details VARCHAR(1000),
    scheduled_for TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(24) NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_action_proposals_id_workspace UNIQUE (id, workspace_id),
    CONSTRAINT uq_action_proposal_idempotency UNIQUE (workspace_id, idempotency_key),
    CONSTRAINT ck_action_proposal_expiry CHECK (expires_at < scheduled_for),
    CONSTRAINT fk_action_proposal_signal FOREIGN KEY (signal_id, workspace_id)
        REFERENCES app.signals(id, workspace_id),
    CONSTRAINT fk_action_proposal_requester FOREIGN KEY (workspace_id, requested_by)
        REFERENCES app.workspaces(id, owner_id)
);

CREATE INDEX ix_action_proposals_workspace_status_created
    ON app.action_proposals(workspace_id, status, created_at DESC);
CREATE INDEX ix_action_proposals_expiration
    ON app.action_proposals(status, expires_at);

CREATE TABLE app.action_approvals (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL,
    proposal_id UUID NOT NULL UNIQUE,
    actor_user_id UUID,
    decision VARCHAR(12) NOT NULL,
    reason VARCHAR(500),
    decided_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_action_approval_proposal FOREIGN KEY (proposal_id, workspace_id)
        REFERENCES app.action_proposals(id, workspace_id) ON DELETE CASCADE,
    CONSTRAINT fk_action_approval_actor FOREIGN KEY (workspace_id, actor_user_id)
        REFERENCES app.workspaces(id, owner_id)
);

CREATE TABLE app.reminders (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL,
    proposal_id UUID NOT NULL UNIQUE,
    title VARCHAR(160) NOT NULL,
    scheduled_for TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_reminders_id_workspace UNIQUE (id, workspace_id),
    CONSTRAINT fk_reminder_proposal FOREIGN KEY (proposal_id, workspace_id)
        REFERENCES app.action_proposals(id, workspace_id) ON DELETE CASCADE
);

CREATE INDEX ix_reminders_workspace_schedule
    ON app.reminders(workspace_id, status, scheduled_for);

CREATE TABLE app.action_history (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL,
    proposal_id UUID NOT NULL,
    actor_user_id UUID,
    event_type VARCHAR(24) NOT NULL,
    previous_status VARCHAR(24),
    new_status VARCHAR(24) NOT NULL,
    detail VARCHAR(500),
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_action_history_proposal FOREIGN KEY (proposal_id, workspace_id)
        REFERENCES app.action_proposals(id, workspace_id) ON DELETE CASCADE,
    CONSTRAINT fk_action_history_actor FOREIGN KEY (workspace_id, actor_user_id)
        REFERENCES app.workspaces(id, owner_id)
);

CREATE INDEX ix_action_history_workspace_time
    ON app.action_history(workspace_id, occurred_at DESC);
