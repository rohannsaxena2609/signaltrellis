package com.signaltrellis.signals;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SignalHistoryRepository extends JpaRepository<SignalHistory, UUID> {
    List<SignalHistory> findBySignalIdAndWorkspaceIdOrderByOccurredAtAsc(UUID signalId, UUID workspaceId);
}
