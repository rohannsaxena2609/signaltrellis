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
@Table(name = "document_processing_jobs", schema = "app")
public class DocumentProcessingJob {

    @Id
    private UUID id;
    @Column(name = "document_id", nullable = false, unique = true)
    private UUID documentId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private DocumentProcessingStatus status;
    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;
    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;
    @Column(name = "lease_expires_at")
    private Instant leaseExpiresAt;
    @Column(name = "last_error", length = 500)
    private String lastError;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DocumentProcessingJob() { }

    public DocumentProcessingJob(UUID documentId, Instant now) {
        this.id = UUID.randomUUID();
        this.documentId = documentId;
        this.status = DocumentProcessingStatus.QUEUED;
        this.attemptCount = 0;
        this.nextAttemptAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public UUID getDocumentId() { return documentId; }
    public DocumentProcessingStatus getStatus() { return status; }
    public int getAttemptCount() { return attemptCount; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public Instant getLeaseExpiresAt() { return leaseExpiresAt; }
    public String getLastError() { return lastError; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void markProcessing(Instant now, Instant leaseExpiresAt) {
        this.status = DocumentProcessingStatus.PROCESSING;
        this.attemptCount++;
        this.nextAttemptAt = now;
        this.leaseExpiresAt = leaseExpiresAt;
        this.lastError = null;
        this.updatedAt = now;
    }

    public void markQueued(Instant now, Instant retryAt, String safeError) {
        this.status = DocumentProcessingStatus.QUEUED;
        this.nextAttemptAt = retryAt;
        this.leaseExpiresAt = null;
        this.lastError = safeError;
        this.updatedAt = now;
    }

    public void markCompleted(Instant now) {
        this.status = DocumentProcessingStatus.PROCESSED;
        this.leaseExpiresAt = null;
        this.lastError = null;
        this.updatedAt = now;
    }

    public void markFailed(Instant now, String safeError) {
        this.status = DocumentProcessingStatus.FAILED;
        this.leaseExpiresAt = null;
        this.lastError = safeError;
        this.updatedAt = now;
    }

    public void requeue(Instant now) {
        this.status = DocumentProcessingStatus.QUEUED;
        this.attemptCount = 0;
        this.nextAttemptAt = now;
        this.leaseExpiresAt = null;
        this.lastError = null;
        this.updatedAt = now;
    }
}
