package com.signaltrellis.retrieval;

import com.signaltrellis.capture.DocumentExtraction;
import com.signaltrellis.capture.DocumentExtractionRepository;
import com.signaltrellis.capture.DocumentNotFoundException;
import com.signaltrellis.capture.DocumentTextPage;
import com.signaltrellis.capture.DocumentTextPageRepository;
import com.signaltrellis.capture.InvalidDocumentException;
import com.signaltrellis.capture.StoredDocument;
import com.signaltrellis.capture.StoredDocumentRepository;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentIndexService {

    private static final int CHUNK_SIZE = 1200;
    private static final int CHUNK_OVERLAP = 200;
    private static final int EMBEDDING_BATCH_SIZE = 16;
    private static final int EMBEDDING_DIMENSIONS = 3072;

    private final StoredDocumentRepository documents;
    private final DocumentExtractionRepository extractions;
    private final DocumentTextPageRepository pages;
    private final DocumentChunkRepository chunks;
    private final DocumentVectorRepository vectors;
    private final ObjectProvider<EmbeddingModel> embeddingModels;
    private final Clock clock;
    private final boolean embeddingsEnabled;
    private final String embeddingModelName;
    private final int maxDocumentCharacters;

    public DocumentIndexService(StoredDocumentRepository documents, DocumentExtractionRepository extractions,
                                DocumentTextPageRepository pages, DocumentChunkRepository chunks,
                                DocumentVectorRepository vectors, ObjectProvider<EmbeddingModel> embeddingModels,
                                Clock clock,
                                @Value("${signaltrellis.retrieval.embeddings-enabled:false}") boolean embeddingsEnabled,
                                @Value("${signaltrellis.retrieval.embedding-model:gemini/gemini-embedding-2}")
                                String embeddingModelName,
                                @Value("${signaltrellis.retrieval.max-document-characters:20000}")
                                int maxDocumentCharacters) {
        this.documents = documents;
        this.extractions = extractions;
        this.pages = pages;
        this.chunks = chunks;
        this.vectors = vectors;
        this.embeddingModels = embeddingModels;
        this.clock = clock;
        this.embeddingsEnabled = embeddingsEnabled;
        this.embeddingModelName = embeddingModelName;
        this.maxDocumentCharacters = maxDocumentCharacters;
    }

    @Transactional
    public IndexResponse index(UUID workspaceId, UUID documentId) {
        StoredDocument document = documents.findByIdAndSourceItem_WorkspaceId(documentId, workspaceId)
                .orElseThrow(DocumentNotFoundException::new);
        DocumentExtraction extraction = extractions.findByDocumentIdAndWorkspaceId(documentId, workspaceId)
                .orElseThrow(DocumentNotFoundException::new);
        List<DocumentChunk> documentChunks = chunks.findByExtractionIdAndWorkspaceIdOrderByPageNumberAscChunkIndexAsc(
                extraction.getId(), workspaceId);
        if (documentChunks.isEmpty()) {
            List<DocumentTextPage> pageTexts = pages.findByExtractionIdOrderByPageNumber(extraction.getId());
            int totalCharacters = pageTexts.stream().mapToInt(page -> page.getContent().length()).sum();
            if (totalCharacters > maxDocumentCharacters) {
                throw new InvalidDocumentException("Document text exceeds the configured retrieval indexing limit");
            }
            List<DocumentChunk> newChunks = chunkPages(workspaceId, document, extraction, pageTexts);
            documentChunks = chunks.saveAll(newChunks);
        }

        if (!embeddingsEnabled) {
            return response(documentId, documentChunks, "DISABLED", "Embeddings are disabled; text chunks are ready");
        }
        EmbeddingModel embeddingModel = embeddingModels.getIfAvailable();
        if (embeddingModel == null) {
            return response(documentId, documentChunks, "UNAVAILABLE", "No embedding model is configured");
        }

        List<DocumentChunk> pending = documentChunks.stream()
                .filter(chunk -> chunk.needsEmbedding(embeddingModelName)).toList();
        int newlyEmbedded = 0;
        for (int offset = 0; offset < pending.size(); offset += EMBEDDING_BATCH_SIZE) {
            List<DocumentChunk> batch = pending.subList(offset,
                    Math.min(offset + EMBEDDING_BATCH_SIZE, pending.size()));
            try {
                List<float[]> results = embeddingModel.embed(batch.stream().map(DocumentChunk::getContent).toList());
                if (results.size() != batch.size()) {
                    return response(documentId, documentChunks, "FAILED", "Embedding response count did not match the batch");
                }
                for (int i = 0; i < batch.size(); i++) {
                    float[] embedding = results.get(i);
                    if (embedding.length != EMBEDDING_DIMENSIONS) {
                        return response(documentId, documentChunks, "FAILED",
                                "Embedding dimension did not match the configured vector index");
                    }
                    vectors.saveEmbedding(batch.get(i).getId(), vectorLiteral(embedding), embeddingModelName);
                    batch.get(i).markEmbedded(embeddingModelName);
                    newlyEmbedded++;
                }
                chunks.saveAll(batch);
            } catch (RuntimeException exception) {
                return response(documentId, documentChunks, "FAILED",
                        "Embedding request failed; retrying will skip completed chunks");
            }
        }
        List<DocumentChunk> refreshed = chunks.findByExtractionIdAndWorkspaceIdOrderByPageNumberAscChunkIndexAsc(
                extraction.getId(), workspaceId);
        return new IndexResponse(documentId, refreshed.size(),
                (int) refreshed.stream().filter(chunk -> !chunk.needsEmbedding(embeddingModelName)).count(),
                newlyEmbedded, "READY", null);
    }

    private List<DocumentChunk> chunkPages(UUID workspaceId, StoredDocument document,
                                           DocumentExtraction extraction, List<DocumentTextPage> pages) {
        List<DocumentChunk> result = new ArrayList<>();
        for (DocumentTextPage page : pages) {
            String text = page.getContent();
            int index = 0;
            for (int start = 0; start < text.length(); start += CHUNK_SIZE - CHUNK_OVERLAP) {
                int end = Math.min(start + CHUNK_SIZE, text.length());
                String content = text.substring(start, end).trim();
                if (!content.isEmpty()) {
                    int leadingWhitespace = text.substring(start, end).length()
                            - text.substring(start, end).stripLeading().length();
                    result.add(new DocumentChunk(workspaceId, document.getId(), extraction.getId(),
                            page.getPageNumber(), index++, start + leadingWhitespace,
                            start + leadingWhitespace + content.length(), content, sha256(content), clock.instant()));
                }
                if (end == text.length()) break;
            }
        }
        return result;
    }

    private IndexResponse response(UUID documentId, List<DocumentChunk> chunks, String status, String message) {
        int complete = (int) chunks.stream().filter(chunk -> !chunk.needsEmbedding(embeddingModelName)).count();
        return new IndexResponse(documentId, chunks.size(), complete, 0, status, message);
    }

    private String vectorLiteral(float[] vector) {
        StringBuilder literal = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (!Float.isFinite(vector[i])) throw new InvalidDocumentException("Embedding contains an invalid value");
            if (i > 0) literal.append(',');
            literal.append(Float.toString(vector[i]));
        }
        return literal.append(']').toString();
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public record IndexResponse(UUID documentId, int chunks, int embeddedChunks, int newlyEmbeddedChunks,
                                String status, String message) { }
}
