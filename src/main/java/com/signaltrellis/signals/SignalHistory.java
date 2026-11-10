package com.signaltrellis.signals;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "signal_history", schema = "app")
public class SignalHistory {

    @Id
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "signal_id", nullable = false)
    private UUID signalId;

    @Column(name = "actor_user_id", nullable = false)
    private UUID actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20)
    private SignalHistoryType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 16)
    private SignalStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 16)
    private SignalStatus newStatus;

    @Column(length = 500)
    private String detail;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected SignalHistory() { }

    public SignalHistory(UUID workspaceId, UUID signalId, UUID actorUserId, SignalHistoryType eventType,
                         SignalStatus previousStatus, SignalStatus newStatus, String detail, Instant occurredAt) {
        this.id = UUID.randomUUID();
        this.workspaceId = workspaceId;
        this.signalId = signalId;
        this.actorUserId = actorUserId;
        this.eventType = eventType;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.detail = detail;
        this.occurredAt = occurredAt;
    }

    public UUID getId() { return id; }
    public UUID getWorkspaceId() { return workspaceId; }
    public UUID getSignalId() { return signalId; }
    public UUID getActorUserId() { return actorUserId; }
    public SignalHistoryType getEventType() { return eventType; }
    public SignalStatus getPreviousStatus() { return previousStatus; }
    public SignalStatus getNewStatus() { return newStatus; }
    public String getDetail() { return detail; }
    public Instant getOccurredAt() { return occurredAt; }
}
