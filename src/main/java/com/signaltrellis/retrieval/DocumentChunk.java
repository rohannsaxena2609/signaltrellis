package com.signaltrellis.retrieval;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "document_chunks", schema = "app")
public class DocumentChunk {
    @Id
    private UUID id;
    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;
    @Column(name = "document_id", nullable = false)
    private UUID documentId;
    @Column(name = "extraction_id", nullable = false)
    private UUID extractionId;
    @Column(name = "page_number", nullable = false)
    private int pageNumber;
    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;
    @Column(name = "start_character", nullable = false)
    private int startCharacter;
    @Column(name = "end_character", nullable = false)
    private int endCharacter;
    @Column(nullable = false, columnDefinition = "text")
    private String content;
    @Column(name = "content_sha256", nullable = false, length = 64)
    private String contentSha256;
    @Column(name = "embedding_model", length = 120)
    private String embeddingModel;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected DocumentChunk() { }

    public DocumentChunk(UUID workspaceId, UUID documentId, UUID extractionId, int pageNumber,
                         int chunkIndex, int startCharacter, int endCharacter,
                         String content, String contentSha256, Instant createdAt) {
        this.id = UUID.randomUUID();
        this.workspaceId = workspaceId;
        this.documentId = documentId;
        this.extractionId = extractionId;
        this.pageNumber = pageNumber;
        this.chunkIndex = chunkIndex;
        this.startCharacter = startCharacter;
        this.endCharacter = endCharacter;
        this.content = content;
        this.contentSha256 = contentSha256;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getWorkspaceId() { return workspaceId; }
    public UUID getDocumentId() { return documentId; }
    public UUID getExtractionId() { return extractionId; }
    public int getPageNumber() { return pageNumber; }
    public int getChunkIndex() { return chunkIndex; }
    public int getStartCharacter() { return startCharacter; }
    public int getEndCharacter() { return endCharacter; }
    public String getContent() { return content; }
    public String getContentSha256() { return contentSha256; }
    public String getEmbeddingModel() { return embeddingModel; }
    public Instant getCreatedAt() { return createdAt; }

    public boolean needsEmbedding(String model) {
        return !model.equals(embeddingModel);
    }

    public void markEmbedded(String model) {
        this.embeddingModel = model;
    }
}
