package com.signaltrellis.actions;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ActionApprovalRepository extends JpaRepository<ActionApproval, UUID> {
    Optional<ActionApproval> findByProposalIdAndWorkspaceId(UUID proposalId, UUID workspaceId);
}
