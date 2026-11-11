package com.signaltrellis.context;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.UUID;
import java.util.List;
import java.util.Optional;

public interface LifeEventRepository extends JpaRepository<LifeEvent, UUID> {
    Optional<LifeEvent> findByIdAndWorkspaceId(UUID id, UUID workspaceId);
    List<LifeEvent> findByWorkspaceIdAndStatusAndEventDateBetweenOrderByEventDateAscIdAsc(
            UUID workspaceId, LifeEventStatus status, LocalDate from, LocalDate to);

    Page<LifeEvent> findByWorkspaceIdAndEventDateBetweenOrderByEventDateAscIdAsc(
            UUID workspaceId, LocalDate from, LocalDate to, Pageable pageable);
}
