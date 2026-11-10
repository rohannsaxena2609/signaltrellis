package com.signaltrellis.capture;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "documents", schema = "app")
public class StoredDocument {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_item_id", nullable = false, unique = true)
    private SourceItem sourceItem;

    @Column(name = "storage_key", nullable = false, unique = true)
    private UUID storageKey;

    @Column(name = "detected_content_type", nullable = false, length = 100)
    private String detectedContentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "checksum_sha256", nullable = false, length = 64)
    private String checksumSha256;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DocumentCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 24)
    private DocumentProcessingStatus processingStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected StoredDocument() {
    }

    public StoredDocument(SourceItem sourceItem, UUID storageKey, String detectedContentType,
                          long sizeBytes, String checksumSha256, DocumentCategory category) {
        this.id = UUID.randomUUID();
        this.sourceItem = sourceItem;
        this.storageKey = storageKey;
        this.detectedContentType = detectedContentType;
        this.sizeBytes = sizeBytes;
        this.checksumSha256 = checksumSha256;
        this.category = category;
        this.processingStatus = DocumentProcessingStatus.UPLOADED;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public SourceItem getSourceItem() { return sourceItem; }
    public UUID getStorageKey() { return storageKey; }
    public String getDetectedContentType() { return detectedContentType; }
    public long getSizeBytes() { return sizeBytes; }
    public String getChecksumSha256() { return checksumSha256; }
    public DocumentCategory getCategory() { return category; }
    public DocumentProcessingStatus getProcessingStatus() { return processingStatus; }
    public Instant getCreatedAt() { return createdAt; }

    public void reclassify(DocumentCategory category) {
        this.category = category;
    }
}
