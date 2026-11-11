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
@Table(name = "extracted_fact_reviews", schema = "app")
public class ExtractedFactReview {
    @Id
    private UUID id;
    @Column(name = "fact_id", nullable = false)
    private UUID factId;
    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;
    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 24)
    private FactReviewStatus reviewStatus;
    @Column(name = "previous_value", length = 1000)
    private String previousValue;
    @Column(name = "corrected_value", length = 1000)
    private String correctedValue;
    @Column(name = "reviewed_by", nullable = false)
    private UUID reviewedBy;
    @Column(name = "reviewed_at", nullable = false, updatable = false)
    private Instant reviewedAt;

    protected ExtractedFactReview() { }

    public ExtractedFactReview(ExtractedFact fact, FactReviewStatus status, String correctedValue,
                               UUID reviewer, Instant now) {
        this.id = UUID.randomUUID();
        this.factId = fact.getId();
        this.workspaceId = fact.getWorkspaceId();
        this.reviewStatus = status;
        this.previousValue = fact.getCorrectedValue() == null
                ? fact.getExtractedValue() : fact.getCorrectedValue();
        this.correctedValue = correctedValue;
        this.reviewedBy = reviewer;
        this.reviewedAt = now;
    }
}
