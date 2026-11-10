package com.signaltrellis.capture;

import com.signaltrellis.identity.Workspace;
import com.signaltrellis.identity.WorkspaceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

@Service
public class DocumentService {

    public static final long MAX_FILE_BYTES = 10L * 1024 * 1024;

    private final SourceItemRepository sourceItems;
    private final StoredDocumentRepository documents;
    private final WorkspaceRepository workspaces;
    private final DocumentFileStorage storage;
    private final TransactionTemplate transactions;

    public DocumentService(SourceItemRepository sourceItems, StoredDocumentRepository documents,
                           WorkspaceRepository workspaces, DocumentFileStorage storage,
                           PlatformTransactionManager transactionManager) {
        this.sourceItems = sourceItems;
        this.documents = documents;
        this.workspaces = workspaces;
        this.storage = storage;
        this.transactions = new TransactionTemplate(transactionManager);
        this.transactions.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
    }

    public DocumentResponse upload(UUID workspaceId, MultipartFile file, String requestedCategory) {
        Workspace workspace = workspaces.findById(workspaceId)
                .orElseThrow(DocumentNotFoundException::new);
        if (file == null || file.isEmpty()) {
            throw new InvalidDocumentException("Choose a non-empty PDF, PNG, or JPEG file");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new InvalidDocumentException("File exceeds the 10 MB upload limit");
        }

        String filename = safeFilename(file.getOriginalFilename());
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException exception) {
            throw new DocumentStorageException("Unable to read uploaded file", exception);
        }
        if (content.length == 0 || content.length > MAX_FILE_BYTES) {
            throw new InvalidDocumentException("File exceeds the 10 MB upload limit or is empty");
        }

        String mediaType = detectMediaType(content, filename);
        DocumentCategory category = parseCategory(requestedCategory);
        UUID storageKey = UUID.randomUUID();
        String checksum = sha256(content);

        try {
            storage.store(storageKey, content);
        } catch (IOException exception) {
            throw new DocumentStorageException("Unable to store uploaded file", exception);
        }

        try {
            DocumentResponse saved = transactions.execute(status -> {
                SourceItem source = sourceItems.save(new SourceItem(workspace.getId(), filename));
                StoredDocument document = documents.save(new StoredDocument(source, storageKey, mediaType,
                        content.length, checksum, category));
                return toResponse(document);
            });
            if (saved == null) {
                throw new IllegalStateException("Document persistence returned no result");
            }
            return saved;
        } catch (RuntimeException exception) {
            try {
                storage.delete(storageKey);
            } catch (IOException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public Page<DocumentResponse> search(UUID workspaceId, String query, String category, String processingStatus,
                                         int page, int size) {
        validatePage(page, size);
        String normalizedQuery = query == null || query.isBlank() ? null : query.trim();
        if (normalizedQuery != null && normalizedQuery.length() > 200) {
            throw new InvalidDocumentException("Search text must be at most 200 characters");
        }
        DocumentCategory categoryFilter = category == null || category.isBlank() ? null : parseCategory(category);
        DocumentProcessingStatus statusFilter = processingStatus == null || processingStatus.isBlank()
                ? null : parseProcessingStatus(processingStatus);
        return documents.search(workspaceId, normalizedQuery, categoryFilter, statusFilter,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<DocumentResponse> inbox(UUID workspaceId, int page, int size) {
        validatePage(page, size);
        return documents.inbox(workspaceId, DocumentProcessingStatus.PROCESSED,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public DocumentResponse get(UUID workspaceId, UUID documentId) {
        return toResponse(findOwnedDocument(workspaceId, documentId));
    }

    @Transactional
    public DocumentResponse reclassify(UUID workspaceId, UUID documentId, String category) {
        StoredDocument document = findOwnedDocument(workspaceId, documentId);
        document.reclassify(parseCategory(category));
        return toResponse(document);
    }

    public DocumentDownload open(UUID workspaceId, UUID documentId) {
        StoredDocument document = findOwnedDocument(workspaceId, documentId);
        try {
            return new DocumentDownload(toResponse(document), storage.open(document.getStorageKey()));
        } catch (IOException exception) {
            throw new DocumentStorageException("Unable to read stored document", exception);
        }
    }

    private StoredDocument findOwnedDocument(UUID workspaceId, UUID documentId) {
        return documents.findByIdAndSourceItem_WorkspaceId(documentId, workspaceId)
                .orElseThrow(DocumentNotFoundException::new);
    }

    private DocumentResponse toResponse(StoredDocument document) {
        SourceItem source = document.getSourceItem();
        return new DocumentResponse(document.getId(), source.getId(), source.getOriginalFilename(),
                document.getDetectedContentType(), document.getSizeBytes(), document.getChecksumSha256(),
                document.getCategory(), document.getProcessingStatus(), document.getCreatedAt());
    }

    private String safeFilename(String suppliedFilename) {
        if (suppliedFilename == null || suppliedFilename.isBlank()) {
            throw new InvalidDocumentException("A filename is required");
        }
        String leaf = suppliedFilename.replace('\\', '/');
        leaf = leaf.substring(leaf.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "_").trim();
        if (leaf.isEmpty() || leaf.length() > 255) {
            throw new InvalidDocumentException("Filename must be between 1 and 255 characters");
        }
        return leaf;
    }

    private String detectMediaType(byte[] content, String filename) {
        String extension = filename.contains(".")
                ? filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        if (startsWith(content, new byte[]{'%', 'P', 'D', 'F', '-'})) {
            if (!extension.equals("pdf")) throw new InvalidDocumentException("File extension does not match PDF content");
            return "application/pdf";
        }
        if (startsWith(content, new byte[]{(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10})) {
            if (!extension.equals("png")) throw new InvalidDocumentException("File extension does not match PNG content");
            return "image/png";
        }
        if (content.length >= 3 && content[0] == (byte) 0xff && content[1] == (byte) 0xd8
                && content[2] == (byte) 0xff) {
            if (!extension.equals("jpg") && !extension.equals("jpeg")) {
                throw new InvalidDocumentException("File extension does not match JPEG content");
            }
            return "image/jpeg";
        }
        throw new InvalidDocumentException("Only PDF, PNG, and JPEG files are supported");
    }

    private boolean startsWith(byte[] content, byte[] signature) {
        if (content.length < signature.length) return false;
        for (int i = 0; i < signature.length; i++) {
            if (content[i] != signature[i]) return false;
        }
        return true;
    }

    private DocumentCategory parseCategory(String value) {
        try {
            return DocumentCategory.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            throw new InvalidDocumentException("Unknown document category");
        }
    }

    private DocumentProcessingStatus parseProcessingStatus(String value) {
        try {
            return DocumentProcessingStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            throw new InvalidDocumentException("Unknown processing status");
        }
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new InvalidDocumentException("Page must be non-negative and page size must be between 1 and 100");
        }
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public record DocumentResponse(UUID id, UUID sourceItemId, String filename, String contentType,
                                   long sizeBytes, String checksumSha256, DocumentCategory category,
                                   DocumentProcessingStatus processingStatus, java.time.Instant createdAt) {
    }

    public record DocumentDownload(DocumentResponse metadata, InputStream content) {
    }
}
