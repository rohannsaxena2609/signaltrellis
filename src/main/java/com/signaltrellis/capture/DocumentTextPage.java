package com.signaltrellis.capture;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "document_text_pages", schema = "app")
public class DocumentTextPage {
    @Id
    private UUID id;
    @Column(name = "extraction_id", nullable = false)
    private UUID extractionId;
    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;
    @Column(name = "page_number", nullable = false)
    private int pageNumber;
    @Column(nullable = false, columnDefinition = "text")
    private String content;
    @Column(name = "character_count", nullable = false)
    private int characterCount;
    @Column(name = "extraction_method", nullable = false, length = 32)
    private String extractionMethod;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected DocumentTextPage() { }

    public DocumentTextPage(UUID extractionId, UUID workspaceId, int pageNumber, String content,
                            String extractionMethod, Instant now) {
        this.id = UUID.randomUUID();
        this.extractionId = extractionId;
        this.workspaceId = workspaceId;
        this.pageNumber = pageNumber;
        this.content = content;
        this.characterCount = content.length();
        this.extractionMethod = extractionMethod;
        this.createdAt = now;
    }

    public UUID getId() { return id; }
    public UUID getExtractionId() { return extractionId; }
    public UUID getWorkspaceId() { return workspaceId; }
    public int getPageNumber() { return pageNumber; }
    public String getContent() { return content; }
    public int getCharacterCount() { return characterCount; }
    public String getExtractionMethod() { return extractionMethod; }
}
