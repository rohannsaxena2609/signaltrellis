package com.signaltrellis.context;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "entity_relationships", schema = "app")
public class EntityRelationship {

    @Id
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_entity_id", referencedColumnName = "id", nullable = false)
    private LifeEntity sourceEntity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_entity_id", referencedColumnName = "id", nullable = false)
    private LifeEntity targetEntity;

    @Enumerated(EnumType.STRING)
    @Column(name = "relationship_type", nullable = false, length = 32)
    private RelationshipType relationshipType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected EntityRelationship() { }

    public EntityRelationship(UUID workspaceId, LifeEntity sourceEntity, LifeEntity targetEntity,
                             RelationshipType relationshipType) {
        this.id = UUID.randomUUID();
        this.workspaceId = workspaceId;
        this.sourceEntity = sourceEntity;
        this.targetEntity = targetEntity;
        this.relationshipType = relationshipType;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getWorkspaceId() { return workspaceId; }
    public LifeEntity getSourceEntity() { return sourceEntity; }
    public LifeEntity getTargetEntity() { return targetEntity; }
    public RelationshipType getRelationshipType() { return relationshipType; }
    public Instant getCreatedAt() { return createdAt; }
}
