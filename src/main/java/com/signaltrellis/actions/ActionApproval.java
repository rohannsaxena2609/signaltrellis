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
@Table(name = "action_approvals", schema = "app")
public class ActionApproval {

    @Id
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "proposal_id", nullable = false, unique = true)
    private UUID proposalId;

    @Column(name = "actor_user_id", nullable = false)
    private UUID actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private ApprovalDecision decision;

    @Column(length = 500)
    private String reason;

    @Column(name = "decided_at", nullable = false, updatable = false)
    private Instant decidedAt;

    protected ActionApproval() { }

    public ActionApproval(UUID workspaceId, UUID proposalId, UUID actorUserId,
                          ApprovalDecision decision, String reason, Instant decidedAt) {
        this.id = UUID.randomUUID();
        this.workspaceId = workspaceId;
        this.proposalId = proposalId;
        this.actorUserId = actorUserId;
        this.decision = decision;
        this.reason = reason;
        this.decidedAt = decidedAt;
    }

    public UUID getId() { return id; }
    public UUID getWorkspaceId() { return workspaceId; }
    public UUID getProposalId() { return proposalId; }
    public UUID getActorUserId() { return actorUserId; }
    public ApprovalDecision getDecision() { return decision; }
    public String getReason() { return reason; }
    public Instant getDecidedAt() { return decidedAt; }
}
