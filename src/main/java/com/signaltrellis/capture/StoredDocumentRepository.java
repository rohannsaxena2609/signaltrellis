package com.signaltrellis.capture;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface StoredDocumentRepository extends JpaRepository<StoredDocument, UUID> {

    @EntityGraph(attributePaths = "sourceItem")
    Optional<StoredDocument> findByIdAndSourceItem_WorkspaceId(UUID id, UUID workspaceId);

    @Query("""
            select d from StoredDocument d join d.sourceItem s
            where s.workspaceId = :workspaceId
              and (:category is null or d.category = :category)
              and (:status is null or d.processingStatus = :status)
              and (:query is null or lower(s.originalFilename) like lower(concat('%', :query, '%')))
            """)
    @EntityGraph(attributePaths = "sourceItem")
    Page<StoredDocument> search(@Param("workspaceId") UUID workspaceId,
                                @Param("query") String query,
                                @Param("category") DocumentCategory category,
                                @Param("status") DocumentProcessingStatus status,
                                Pageable pageable);

    @Query("""
            select d from StoredDocument d join d.sourceItem s
            where s.workspaceId = :workspaceId and d.processingStatus <> :processed
            """)
    @EntityGraph(attributePaths = "sourceItem")
    Page<StoredDocument> inbox(@Param("workspaceId") UUID workspaceId,
                               @Param("processed") DocumentProcessingStatus processed,
                               Pageable pageable);
}
