package com.signaltrellis.actions;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.List;

public interface ActionHistoryRepository extends JpaRepository<ActionHistory, UUID> {
    Page<ActionHistory> findByWorkspaceIdOrderByOccurredAtDescIdDesc(UUID workspaceId, Pageable pageable);
    List<ActionHistory> findByProposalIdAndWorkspaceIdOrderByOccurredAtAsc(UUID proposalId, UUID workspaceId);
}
