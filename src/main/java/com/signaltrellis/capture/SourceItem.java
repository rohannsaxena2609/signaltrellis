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
@Table(name = "source_items", schema = "app")
public class SourceItem {

    @Id
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 24)
    private SourceType sourceType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private SourceStatus status;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected SourceItem() {
    }

    public SourceItem(UUID workspaceId, String originalFilename) {
        this.id = UUID.randomUUID();
        this.workspaceId = workspaceId;
        this.sourceType = SourceType.DOCUMENT;
        this.status = SourceStatus.RECEIVED;
        this.originalFilename = originalFilename;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getWorkspaceId() { return workspaceId; }
    public SourceType getSourceType() { return sourceType; }
    public SourceStatus getStatus() { return status; }
    public String getOriginalFilename() { return originalFilename; }
    public Instant getCreatedAt() { return createdAt; }
}
