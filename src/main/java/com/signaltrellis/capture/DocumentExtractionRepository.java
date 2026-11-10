package com.signaltrellis.capture;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DocumentExtractionRepository extends JpaRepository<DocumentExtraction, UUID> {
    Optional<DocumentExtraction> findByDocumentIdAndWorkspaceId(UUID documentId, UUID workspaceId);
}
