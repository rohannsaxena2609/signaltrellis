package com.signaltrellis.signals;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SignalRepository extends JpaRepository<Signal, UUID> {

    List<Signal> findByWorkspaceIdAndDedupeKeyIn(UUID workspaceId, Collection<String> dedupeKeys);

    List<Signal> findByWorkspaceIdAndStatusIn(UUID workspaceId, Collection<SignalStatus> statuses);

    long countByWorkspaceIdAndStatus(UUID workspaceId, SignalStatus status);

    Optional<Signal> findByIdAndWorkspaceId(UUID id, UUID workspaceId);

    @Query("""
            select s from Signal s
            where s.workspaceId = :workspaceId
              and (:status is null or s.status = :status)
              and (:priority is null or s.priority = :priority)
              and (:type is null or s.signalType = :type)
            """)
    Page<Signal> search(@Param("workspaceId") UUID workspaceId,
                        @Param("status") SignalStatus status,
                        @Param("priority") SignalPriority priority,
                        @Param("type") SignalType type,
                        Pageable pageable);
}
