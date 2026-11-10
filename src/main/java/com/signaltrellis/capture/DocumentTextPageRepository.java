package com.signaltrellis.capture;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentTextPageRepository extends JpaRepository<DocumentTextPage, UUID> {
    List<DocumentTextPage> findByExtractionIdOrderByPageNumber(UUID extractionId);
    void deleteByExtractionId(UUID extractionId);
}
