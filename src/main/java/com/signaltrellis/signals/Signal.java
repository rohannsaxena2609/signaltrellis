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
@Table(name = "signals", schema = "app")
public class Signal {

    @Id
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "signal_type", nullable = false, length = 40)
    private SignalType signalType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SignalPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SignalStatus status;

    @Column(name = "dedupe_key", nullable = false, length = 160)
    private String dedupeKey;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 1000)
    private String explanation;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    @Column(name = "last_detected_at", nullable = false)
    private Instant lastDetectedAt;

    @Column(name = "last_evaluated_at", nullable = false)
    private Instant lastEvaluatedAt;

    protected Signal() { }

    public Signal(UUID workspaceId, String dedupeKey, SignalType signalType, SignalPriority priority,
                  String title, String explanation, Instant now) {
        this.id = UUID.randomUUID();
        this.workspaceId = workspaceId;
        this.dedupeKey = dedupeKey;
        this.signalType = signalType;
        this.priority = priority;
        this.title = title;
        this.explanation = explanation;
        this.status = SignalStatus.ACTIVE;
        this.detectedAt = now;
        this.lastDetectedAt = now;
        this.lastEvaluatedAt = now;
    }

    public void refresh(SignalPriority priority, String title, String explanation, Instant now) {
        this.priority = priority;
        this.title = title;
        this.explanation = explanation;
        this.lastDetectedAt = now;
        this.lastEvaluatedAt = now;
    }

    public void reopen(SignalPriority priority, String title, String explanation, Instant now) {
        this.priority = priority;
        this.title = title;
        this.explanation = explanation;
        this.status = SignalStatus.ACTIVE;
        this.detectedAt = now;
        this.lastDetectedAt = now;
        this.lastEvaluatedAt = now;
    }

    public void setStatus(SignalStatus status) { this.status = status; }
    public void markEvaluated(Instant now) { this.lastEvaluatedAt = now; }
    public UUID getId() { return id; }
    public UUID getWorkspaceId() { return workspaceId; }
    public SignalType getSignalType() { return signalType; }
    public SignalPriority getPriority() { return priority; }
    public SignalStatus getStatus() { return status; }
    public String getDedupeKey() { return dedupeKey; }
    public String getTitle() { return title; }
    public String getExplanation() { return explanation; }
    public Instant getDetectedAt() { return detectedAt; }
    public Instant getLastDetectedAt() { return lastDetectedAt; }
    public Instant getLastEvaluatedAt() { return lastEvaluatedAt; }
}
