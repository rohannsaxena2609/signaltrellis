package com.signaltrellis.capture;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SourceItemRepository extends JpaRepository<SourceItem, UUID> {
}
