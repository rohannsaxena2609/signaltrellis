package com.signaltrellis.capture;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "extracted_fact_context_links", schema = "app")
public class ExtractedFactContextLink {
    @Id
    private UUID id;
    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;
    @Column(name = "fact_id", nullable = false)
    private UUID factId;
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 24)
    private FactContextTargetType targetType;
    @Column(name = "target_id", nullable = false)
    private UUID targetId;
    @Column(name = "linked_by", nullable = false)
    private UUID linkedBy;
    @Column(name = "linked_at", nullable = false, updatable = false)
    private Instant linkedAt;

    protected ExtractedFactContextLink() { }

    public ExtractedFactContextLink(UUID workspaceId, UUID factId, FactContextTargetType targetType,
                                   UUID targetId, UUID linkedBy, Instant linkedAt) {
        this.id = UUID.randomUUID();
        this.workspaceId = workspaceId;
        this.factId = factId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.linkedBy = linkedBy;
        this.linkedAt = linkedAt;
    }

    public UUID getId() { return id; }
    public UUID getWorkspaceId() { return workspaceId; }
    public UUID getFactId() { return factId; }
    public FactContextTargetType getTargetType() { return targetType; }
    public UUID getTargetId() { return targetId; }
    public UUID getLinkedBy() { return linkedBy; }
    public Instant getLinkedAt() { return linkedAt; }
}
