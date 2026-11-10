package com.signaltrellis.signals;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SignalEvidenceRepository extends JpaRepository<SignalEvidence, UUID> {
    List<SignalEvidence> findBySignalIdAndWorkspaceIdOrderByCreatedAtAsc(UUID signalId, UUID workspaceId);
    void deleteBySignalIdAndWorkspaceId(UUID signalId, UUID workspaceId);
}
