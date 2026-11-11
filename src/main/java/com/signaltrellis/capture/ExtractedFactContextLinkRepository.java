package com.signaltrellis.capture;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ExtractedFactContextLinkRepository extends JpaRepository<ExtractedFactContextLink, UUID> {
    List<ExtractedFactContextLink> findByFactIdAndWorkspaceIdOrderByLinkedAtDesc(UUID factId, UUID workspaceId);
    boolean existsByFactIdAndWorkspaceId(UUID factId, UUID workspaceId);
    boolean existsByWorkspaceIdAndFactIdAndTargetTypeAndTargetId(UUID workspaceId, UUID factId,
                                                                   FactContextTargetType targetType,
                                                                   UUID targetId);
}
