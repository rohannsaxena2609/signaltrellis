package com.signaltrellis.actions;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "action_history", schema = "app")
public class ActionHistory {

    @Id
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "proposal_id", nullable = false)
    private UUID proposalId;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 24)
    private ActionHistoryType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 24)
    private ActionProposalStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 24)
    private ActionProposalStatus newStatus;

    @Column(length = 500)
    private String detail;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected ActionHistory() { }

    public ActionHistory(UUID workspaceId, UUID proposalId, UUID actorUserId, ActionHistoryType eventType,
                         ActionProposalStatus previousStatus, ActionProposalStatus newStatus,
                         String detail, Instant occurredAt) {
        this.id = UUID.randomUUID();
        this.workspaceId = workspaceId;
        this.proposalId = proposalId;
        this.actorUserId = actorUserId;
        this.eventType = eventType;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.detail = detail;
        this.occurredAt = occurredAt;
    }

    public UUID getId() { return id; }
    public UUID getWorkspaceId() { return workspaceId; }
    public UUID getProposalId() { return proposalId; }
    public UUID getActorUserId() { return actorUserId; }
    public ActionHistoryType getEventType() { return eventType; }
    public ActionProposalStatus getPreviousStatus() { return previousStatus; }
    public ActionProposalStatus getNewStatus() { return newStatus; }
    public String getDetail() { return detail; }
    public Instant getOccurredAt() { return occurredAt; }
}
