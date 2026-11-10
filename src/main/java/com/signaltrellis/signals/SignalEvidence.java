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
@Table(name = "signal_evidence", schema = "app")
public class SignalEvidence {

    @Id
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "signal_id", nullable = false)
    private UUID signalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_type", nullable = false, length = 20)
    private SignalEvidenceType evidenceType;

    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "commitment_id")
    private UUID commitmentId;

    @Column(nullable = false, length = 500)
    private String summary;

    @Column(name = "observed_value", length = 500)
    private String observedValue;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected SignalEvidence() { }

    public SignalEvidence(UUID workspaceId, UUID signalId, SignalEvidenceType evidenceType,
                          UUID eventId, UUID commitmentId, String summary, String observedValue) {
        this.id = UUID.randomUUID();
        this.workspaceId = workspaceId;
        this.signalId = signalId;
        this.evidenceType = evidenceType;
        this.eventId = eventId;
        this.commitmentId = commitmentId;
        this.summary = summary;
        this.observedValue = observedValue;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getWorkspaceId() { return workspaceId; }
    public UUID getSignalId() { return signalId; }
    public SignalEvidenceType getEvidenceType() { return evidenceType; }
    public UUID getEventId() { return eventId; }
    public UUID getCommitmentId() { return commitmentId; }
    public String getSummary() { return summary; }
    public String getObservedValue() { return observedValue; }
    public Instant getCreatedAt() { return createdAt; }
}
