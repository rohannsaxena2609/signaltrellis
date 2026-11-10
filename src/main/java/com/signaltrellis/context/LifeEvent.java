package com.signaltrellis.context;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "life_events", schema = "app")
public class LifeEvent {

    @Id
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "entity_id")
    private UUID entityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 32)
    private LifeEventType eventType;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(length = 2000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private LifeEventStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected LifeEvent() { }

    public LifeEvent(UUID workspaceId, UUID entityId, LifeEventType eventType, String title,
                     LocalDate eventDate, String notes) {
        this.id = UUID.randomUUID();
        this.workspaceId = workspaceId;
        this.entityId = entityId;
        this.eventType = eventType;
        this.title = title;
        this.eventDate = eventDate;
        this.notes = notes;
        this.status = LifeEventStatus.SCHEDULED;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getWorkspaceId() { return workspaceId; }
    public UUID getEntityId() { return entityId; }
    public LifeEventType getEventType() { return eventType; }
    public String getTitle() { return title; }
    public LocalDate getEventDate() { return eventDate; }
    public String getNotes() { return notes; }
    public LifeEventStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
