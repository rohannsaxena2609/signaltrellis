package com.signaltrellis.context;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LifeEntityRepository extends JpaRepository<LifeEntity, UUID> {

    boolean existsByWorkspaceIdAndEntityTypeAndNameIgnoreCase(UUID workspaceId, LifeEntityType entityType,
                                                               String name);

    Optional<LifeEntity> findByIdAndWorkspaceId(UUID id, UUID workspaceId);

    @Query("""
            select e from LifeEntity e
            where e.workspaceId = :workspaceId
              and (:entityType is null or e.entityType = :entityType)
              and (:query is null or lower(e.name) like lower(concat('%', :query, '%')))
            """)
    Page<LifeEntity> search(@Param("workspaceId") UUID workspaceId,
                            @Param("entityType") LifeEntityType entityType,
                            @Param("query") String query,
                            Pageable pageable);

    List<LifeEntity> findAllByWorkspaceIdAndIdIn(UUID workspaceId, Collection<UUID> ids);
}
