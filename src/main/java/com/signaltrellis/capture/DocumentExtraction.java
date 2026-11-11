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
@Table(name = "document_extractions", schema = "app")
public class DocumentExtraction {
    @Id
    private UUID id;
    @Column(name = "document_id", nullable = false, unique = true)
    private UUID documentId;
    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ExtractionStatus status;
    @Column(nullable = false, length = 80)
    private String extractor;
    @Column(name = "extracted_character_count", nullable = false)
    private int extractedCharacterCount;
    @Column(name = "failure_reason", length = 500)
    private String failureReason;
    @Column(name = "started_at", nullable = false)
    private Instant startedAt;
    @Column(name = "completed_at")
    private Instant completedAt;
    @Enumerated(EnumType.STRING)
    @Column(name = "ai_status", nullable = false, length = 24)
    private AiExtractionStatus aiStatus;
    @Column(name = "ai_model", length = 120)
    private String aiModel;
    @Column(name = "ai_input_characters", nullable = false)
    private int aiInputCharacters;
    @Column(name = "ai_failure_reason", length = 500)
    private String aiFailureReason;

    protected DocumentExtraction() { }

    public DocumentExtraction(UUID documentId, UUID workspaceId, String extractor, Instant now) {
        this.id = UUID.randomUUID();
        this.documentId = documentId;
        this.workspaceId = workspaceId;
        this.status = ExtractionStatus.COMPLETED;
        this.extractor = extractor;
        this.startedAt = now;
        this.completedAt = now;
        this.aiStatus = AiExtractionStatus.NOT_CONFIGURED;
    }

    public UUID getId() { return id; }
    public UUID getDocumentId() { return documentId; }
    public UUID getWorkspaceId() { return workspaceId; }
    public ExtractionStatus getStatus() { return status; }
    public String getExtractor() { return extractor; }
    public int getExtractedCharacterCount() { return extractedCharacterCount; }
    public String getFailureReason() { return failureReason; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public AiExtractionStatus getAiStatus() { return aiStatus; }
    public String getAiModel() { return aiModel; }
    public int getAiInputCharacters() { return aiInputCharacters; }
    public String getAiFailureReason() { return aiFailureReason; }

    public void complete(int characterCount, Instant now) {
        this.status = ExtractionStatus.COMPLETED;
        this.extractedCharacterCount = characterCount;
        this.completedAt = now;
        this.failureReason = null;
        this.aiStatus = AiExtractionStatus.NOT_CONFIGURED;
        this.aiModel = null;
        this.aiInputCharacters = 0;
        this.aiFailureReason = null;
    }

    public void reprocess(String extractor, Instant now) {
        this.status = ExtractionStatus.COMPLETED;
        this.extractor = extractor;
        this.extractedCharacterCount = 0;
        this.failureReason = null;
        this.startedAt = now;
        this.completedAt = now;
    }

    public void recordAiResult(AiExtractionStatus status, String model, int inputCharacters, String failureReason) {
        this.aiStatus = status;
        this.aiModel = model;
        this.aiInputCharacters = inputCharacters;
        this.aiFailureReason = failureReason;
    }
}
