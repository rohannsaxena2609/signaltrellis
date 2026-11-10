package com.signaltrellis.actions;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReminderRepository extends JpaRepository<Reminder, UUID> {
    Optional<Reminder> findByProposalIdAndWorkspaceId(UUID proposalId, UUID workspaceId);
}
