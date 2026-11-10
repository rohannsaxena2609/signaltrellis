package com.signaltrellis.context;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.UUID;

public interface LifeEventRepository extends JpaRepository<LifeEvent, UUID> {
    Page<LifeEvent> findByWorkspaceIdAndEventDateBetweenOrderByEventDateAscIdAsc(
            UUID workspaceId, LocalDate from, LocalDate to, Pageable pageable);
}
