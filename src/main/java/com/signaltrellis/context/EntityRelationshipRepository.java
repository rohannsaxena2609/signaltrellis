package com.signaltrellis.context;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface EntityRelationshipRepository extends JpaRepository<EntityRelationship, UUID> {

    boolean existsByWorkspaceIdAndSourceEntity_IdAndTargetEntity_IdAndRelationshipType(
            UUID workspaceId, UUID sourceId, UUID targetId, RelationshipType relationshipType);

    @EntityGraph(attributePaths = {"sourceEntity", "targetEntity"})
    Optional<EntityRelationship> findByIdAndWorkspaceId(UUID id, UUID workspaceId);

    @EntityGraph(attributePaths = {"sourceEntity", "targetEntity"})
    @Query("""
            select r from EntityRelationship r
            where r.workspaceId = :workspaceId
              and (r.sourceEntity.id in :entityIds or r.targetEntity.id in :entityIds)
            order by r.createdAt, r.id
            """)
    Page<EntityRelationship> findTouching(@Param("workspaceId") UUID workspaceId,
                                          @Param("entityIds") Collection<UUID> entityIds,
                                          Pageable pageable);
}
