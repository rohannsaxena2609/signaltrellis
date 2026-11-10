package com.signaltrellis.context;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "commitments", schema = "app")
public class Commitment {

    @Id
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(length = 3)
    private String currency;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private CommitmentStatus status;

    @Column(length = 2000)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Commitment() { }

    public Commitment(UUID workspaceId, UUID entityId, String title, BigDecimal amount,
                      String currency, LocalDate dueDate, String notes) {
        this.id = UUID.randomUUID();
        this.workspaceId = workspaceId;
        this.entityId = entityId;
        this.title = title;
        this.amount = amount;
        this.currency = currency;
        this.dueDate = dueDate;
        this.notes = notes;
        this.status = CommitmentStatus.OPEN;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getWorkspaceId() { return workspaceId; }
    public UUID getEntityId() { return entityId; }
    public String getTitle() { return title; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public LocalDate getDueDate() { return dueDate; }
    public CommitmentStatus getStatus() { return status; }
    public String getNotes() { return notes; }
    public Instant getCreatedAt() { return createdAt; }
}
