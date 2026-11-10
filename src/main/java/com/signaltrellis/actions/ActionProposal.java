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
@Table(name = "action_proposals", schema = "app")
public class ActionProposal {

    @Id
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "signal_id")
    private UUID signalId;

    @Column(name = "requested_by", nullable = false)
    private UUID requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 40)
    private ActionType actionType;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(length = 1000)
    private String details;

    @Column(name = "scheduled_for", nullable = false)
    private Instant scheduledFor;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ActionProposalStatus status;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ActionProposal() { }

    public ActionProposal(UUID workspaceId, UUID signalId, UUID requestedBy, ActionType actionType,
                         String title, String details, Instant scheduledFor, Instant expiresAt,
                         String idempotencyKey, String requestHash, Instant now) {
        this.id = UUID.randomUUID();
        this.workspaceId = workspaceId;
        this.signalId = signalId;
        this.requestedBy = requestedBy;
        this.actionType = actionType;
        this.title = title;
        this.details = details;
        this.scheduledFor = scheduledFor;
        this.expiresAt = expiresAt;
        this.status = ActionProposalStatus.PENDING_APPROVAL;
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void approve(Instant now) {
        this.status = ActionProposalStatus.APPROVED;
        this.updatedAt = now;
    }

    public void reject(Instant now) {
        this.status = ActionProposalStatus.REJECTED;
        this.updatedAt = now;
    }

    public void expire(Instant now) {
        this.status = ActionProposalStatus.EXPIRED;
        this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public UUID getWorkspaceId() { return workspaceId; }
    public UUID getSignalId() { return signalId; }
    public UUID getRequestedBy() { return requestedBy; }
    public ActionType getActionType() { return actionType; }
    public String getTitle() { return title; }
    public String getDetails() { return details; }
    public Instant getScheduledFor() { return scheduledFor; }
    public Instant getExpiresAt() { return expiresAt; }
    public ActionProposalStatus getStatus() { return status; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getRequestHash() { return requestHash; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
