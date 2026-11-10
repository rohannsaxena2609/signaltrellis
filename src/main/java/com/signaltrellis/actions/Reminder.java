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
@Table(name = "reminders", schema = "app")
public class Reminder {

    @Id
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "proposal_id", nullable = false, unique = true)
    private UUID proposalId;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(name = "scheduled_for", nullable = false)
    private Instant scheduledFor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReminderStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Reminder() { }

    public Reminder(UUID workspaceId, UUID proposalId, String title, Instant scheduledFor, Instant createdAt) {
        this.id = UUID.randomUUID();
        this.workspaceId = workspaceId;
        this.proposalId = proposalId;
        this.title = title;
        this.scheduledFor = scheduledFor;
        this.status = ReminderStatus.SCHEDULED;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getWorkspaceId() { return workspaceId; }
    public UUID getProposalId() { return proposalId; }
    public String getTitle() { return title; }
    public Instant getScheduledFor() { return scheduledFor; }
    public ReminderStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
