package com.signaltrellis.actions;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ActionProposalRepository extends JpaRepository<ActionProposal, UUID> {

    Optional<ActionProposal> findByWorkspaceIdAndIdempotencyKey(UUID workspaceId, String idempotencyKey);

    Optional<ActionProposal> findByIdAndWorkspaceId(UUID id, UUID workspaceId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ActionProposal p where p.id = :id and p.workspaceId = :workspaceId")
    Optional<ActionProposal> lockByIdAndWorkspaceId(@Param("id") UUID id,
                                                    @Param("workspaceId") UUID workspaceId);

    Page<ActionProposal> findByWorkspaceIdAndStatusOrderByCreatedAtDescIdDesc(
            UUID workspaceId, ActionProposalStatus status, Pageable pageable);

    Page<ActionProposal> findByWorkspaceIdOrderByCreatedAtDescIdDesc(UUID workspaceId, Pageable pageable);

    Page<ActionProposal> findByStatusAndExpiresAtLessThanEqualOrderByExpiresAtAsc(
            ActionProposalStatus status, Instant now, Pageable pageable);
}
