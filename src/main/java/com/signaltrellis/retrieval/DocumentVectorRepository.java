package com.signaltrellis.retrieval;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class DocumentVectorRepository {

    private final JdbcTemplate jdbc;

    public DocumentVectorRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void saveEmbedding(UUID chunkId, String vectorLiteral, String embeddingModel) {
        jdbc.update("UPDATE app.document_chunks SET embedding = CAST(? AS halfvec(3072)), embedding_model = ? WHERE id = ?",
                vectorLiteral, embeddingModel, chunkId);
    }

    public List<SearchRow> semanticSearch(UUID workspaceId, String query, String vectorLiteral,
                                          String embeddingModel, int limit) {
        return jdbc.query("""
                SELECT c.id AS chunk_id, c.document_id, s.original_filename, c.page_number,
                       c.chunk_index, c.start_character, c.end_character, c.content,
                       1 - (c.embedding <=> CAST(? AS halfvec(3072))) AS semantic_score,
                       ts_rank_cd(c.search_vector, plainto_tsquery('simple', ?)) AS lexical_score
                FROM app.document_chunks c
                JOIN app.documents d ON d.id = c.document_id
                JOIN app.source_items s ON s.id = d.source_item_id AND s.workspace_id = c.workspace_id
                WHERE c.workspace_id = ? AND c.embedding IS NOT NULL AND c.embedding_model = ?
                ORDER BY (0.8 * (1 - (c.embedding <=> CAST(? AS halfvec(3072))))
                          + 0.2 * (ts_rank_cd(c.search_vector, plainto_tsquery('simple', ?))
                                   / (1 + ts_rank_cd(c.search_vector, plainto_tsquery('simple', ?))))) DESC
                LIMIT ?
                """, (rs, rowNum) -> new SearchRow(rs.getObject("chunk_id", UUID.class),
                rs.getObject("document_id", UUID.class), rs.getString("original_filename"),
                rs.getInt("page_number"), rs.getInt("chunk_index"), rs.getInt("start_character"),
                rs.getInt("end_character"), rs.getString("content"), rs.getDouble("semantic_score"),
                rs.getDouble("lexical_score")), vectorLiteral, query, workspaceId,
                vectorLiteral, query, query, embeddingModel, vectorLiteral, query, query, limit);
    }

    public List<SearchRow> lexicalSearch(UUID workspaceId, String query, int limit) {
        return jdbc.query("""
                SELECT c.id AS chunk_id, c.document_id, s.original_filename, c.page_number,
                       c.chunk_index, c.start_character, c.end_character, c.content,
                       0.0 AS semantic_score,
                       ts_rank_cd(c.search_vector, plainto_tsquery('simple', ?)) AS lexical_score
                FROM app.document_chunks c
                JOIN app.documents d ON d.id = c.document_id
                JOIN app.source_items s ON s.id = d.source_item_id AND s.workspace_id = c.workspace_id
                WHERE c.workspace_id = ?
                  AND c.search_vector @@ plainto_tsquery('simple', ?)
                ORDER BY ts_rank_cd(c.search_vector, plainto_tsquery('simple', ?)) DESC
                LIMIT ?
                """, (rs, rowNum) -> new SearchRow(rs.getObject("chunk_id", UUID.class),
                rs.getObject("document_id", UUID.class), rs.getString("original_filename"),
                rs.getInt("page_number"), rs.getInt("chunk_index"), rs.getInt("start_character"),
                rs.getInt("end_character"), rs.getString("content"), rs.getDouble("semantic_score"),
                rs.getDouble("lexical_score")), query, workspaceId, query, query, limit);
    }

    public record SearchRow(UUID chunkId, UUID documentId, String filename, int pageNumber, int chunkIndex,
                            int startCharacter, int endCharacter, String content, double semanticScore,
                            double lexicalScore) { }
}
