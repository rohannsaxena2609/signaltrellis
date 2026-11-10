package com.signaltrellis.capture;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExtractedFactRepository extends JpaRepository<ExtractedFact, UUID> {
    List<ExtractedFact> findByExtractionIdAndWorkspaceIdOrderBySourcePageAscIdAsc(UUID extractionId,
                                                                                    UUID workspaceId);
    Optional<ExtractedFact> findByIdAndWorkspaceId(UUID id, UUID workspaceId);
    void deleteByExtractionId(UUID extractionId);
}
