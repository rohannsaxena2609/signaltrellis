package com.signaltrellis.retrieval;

import com.signaltrellis.capture.InvalidDocumentException;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentRetrievalService {

    private final DocumentVectorRepository vectors;
    private final ObjectProvider<EmbeddingModel> embeddingModels;
    private final boolean embeddingsEnabled;
    private final String embeddingModelName;

    public DocumentRetrievalService(DocumentVectorRepository vectors, ObjectProvider<EmbeddingModel> embeddingModels,
                                    @Value("${signaltrellis.retrieval.embeddings-enabled:false}") boolean embeddingsEnabled,
                                    @Value("${signaltrellis.retrieval.embedding-model:gemini/gemini-embedding-2}")
                                    String embeddingModelName) {
        this.vectors = vectors;
        this.embeddingModels = embeddingModels;
        this.embeddingsEnabled = embeddingsEnabled;
        this.embeddingModelName = embeddingModelName;
    }

    public SearchResponse search(UUID workspaceId, String query, int requestedLimit) {
        if (query == null || query.isBlank() || query.length() > 1000) {
            throw new InvalidDocumentException("Search text must be between 1 and 1000 characters");
        }
        if (requestedLimit < 1 || requestedLimit > 20) {
            throw new InvalidDocumentException("Search limit must be between 1 and 20");
        }
        String normalized = query.trim();
        List<DocumentVectorRepository.SearchRow> rows;
        boolean semantic = false;
        if (embeddingsEnabled) {
            EmbeddingModel model = embeddingModels.getIfAvailable();
            if (model != null) {
                try {
                    float[] embedding = model.embed(normalized);
                    if (embedding.length == 3072) {
                        rows = vectors.semanticSearch(workspaceId, normalized, vectorLiteral(embedding),
                                embeddingModelName, requestedLimit);
                        semantic = true;
                    } else {
                        rows = vectors.lexicalSearch(workspaceId, normalized, requestedLimit);
                    }
                } catch (RuntimeException exception) {
                    rows = vectors.lexicalSearch(workspaceId, normalized, requestedLimit);
                }
            } else {
                rows = vectors.lexicalSearch(workspaceId, normalized, requestedLimit);
            }
        } else {
            rows = vectors.lexicalSearch(workspaceId, normalized, requestedLimit);
        }
        if (rows.isEmpty() && semantic) {
            rows = vectors.lexicalSearch(workspaceId, normalized, requestedLimit);
            semantic = false;
        }

        List<Citation> citations = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            var row = rows.get(i);
            citations.add(new Citation(i + 1, row.documentId(), row.filename(), row.pageNumber(), row.chunkIndex(),
                    row.startCharacter(), row.endCharacter(), row.content(), row.semanticScore(), row.lexicalScore()));
        }
        return new SearchResponse(normalized, semantic ? "HYBRID" : "LEXICAL", embeddingModelName,
                citations.size(), citations);
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

    public record SearchResponse(String query, String mode, String embeddingModel, int resultCount,
                                 List<Citation> citations) { }

    public record Citation(int reference, UUID documentId, String filename, int pageNumber, int chunkIndex,
                           int startCharacter, int endCharacter, String text,
                           double semanticScore, double lexicalScore) { }
}
