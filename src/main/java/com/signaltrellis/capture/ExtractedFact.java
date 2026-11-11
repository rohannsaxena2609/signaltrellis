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
@Table(name = "extracted_facts", schema = "app")
public class ExtractedFact {
    @Id
    private UUID id;
    @Column(name = "extraction_id", nullable = false)
    private UUID extractionId;
    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;
    @Column(name = "fact_type", nullable = false, length = 60)
    private String factType;
    @Column(name = "extracted_value", nullable = false, length = 1000)
    private String extractedValue;
    @Column(name = "source_page", nullable = false)
    private int sourcePage;
    @Column(name = "source_excerpt", nullable = false, length = 1000)
    private String sourceExcerpt;
    @Column(precision = 4, scale = 3)
    private java.math.BigDecimal confidence;
    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 24)
    private FactReviewStatus reviewStatus;
    @Column(name = "corrected_value", length = 1000)
    private String correctedValue;
    @Column(name = "reviewed_by")
    private UUID reviewedBy;
    @Column(name = "reviewed_at")
    private Instant reviewedAt;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ExtractedFact() { }

    public ExtractedFact(UUID extractionId, UUID workspaceId, String factType, String extractedValue,
                         int sourcePage, String sourceExcerpt, Instant now) {
        this(extractionId, workspaceId, factType, extractedValue, sourcePage, sourceExcerpt, null, now);
    }

    public ExtractedFact(UUID extractionId, UUID workspaceId, String factType, String extractedValue,
                         int sourcePage, String sourceExcerpt, java.math.BigDecimal confidence, Instant now) {
        this.id = UUID.randomUUID();
        this.extractionId = extractionId;
        this.workspaceId = workspaceId;
        this.factType = factType;
        this.extractedValue = extractedValue;
        this.sourcePage = sourcePage;
        this.sourceExcerpt = sourceExcerpt;
        this.confidence = confidence;
        this.reviewStatus = FactReviewStatus.PENDING_REVIEW;
        this.createdAt = now;
    }

    public void review(FactReviewStatus status, String correction, UUID reviewer, Instant now) {
        this.reviewStatus = status;
        this.correctedValue = correction;
        this.reviewedBy = reviewer;
        this.reviewedAt = now;
    }

    public UUID getId() { return id; }
    public UUID getExtractionId() { return extractionId; }
    public UUID getWorkspaceId() { return workspaceId; }
    public String getFactType() { return factType; }
    public String getExtractedValue() { return extractedValue; }
    public int getSourcePage() { return sourcePage; }
    public String getSourceExcerpt() { return sourceExcerpt; }
    public java.math.BigDecimal getConfidence() { return confidence; }
    public FactReviewStatus getReviewStatus() { return reviewStatus; }
    public String getCorrectedValue() { return correctedValue; }
    public UUID getReviewedBy() { return reviewedBy; }
    public Instant getReviewedAt() { return reviewedAt; }
}
