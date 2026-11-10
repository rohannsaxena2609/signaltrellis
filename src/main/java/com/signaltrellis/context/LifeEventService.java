package com.signaltrellis.context;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class LifeEventService {

    private final LifeEventRepository events;
    private final LifeEntityRepository entities;

    public LifeEventService(LifeEventRepository events, LifeEntityRepository entities) {
        this.events = events;
        this.entities = entities;
    }

    @Transactional
    public EventResponse create(UUID workspaceId, LifeEventType type, String title,
                                LocalDate eventDate, String notes, UUID entityId) {
        validateEntity(workspaceId, entityId);
        LifeEvent event = events.save(new LifeEvent(workspaceId, entityId, type, title.trim(), eventDate,
                cleanOptional(notes)));
        return toResponse(event);
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> timeline(UUID workspaceId, LocalDate from, LocalDate to, int page, int size) {
        validatePage(page, size);
        LocalDate today = LocalDate.now();
        LocalDate start = from == null ? today.minusDays(30) : from;
        LocalDate end = to == null ? today.plusDays(365) : to;
        if (start.isAfter(end) || end.isAfter(start.plusYears(5))) {
            throw new InvalidContextRequestException("Timeline must be ordered and span no more than five years");
        }
        return events.findByWorkspaceIdAndEventDateBetweenOrderByEventDateAscIdAsc(workspaceId, start, end,
                        PageRequest.of(page, size))
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> upcoming(UUID workspaceId, int days, int page, int size) {
        if (days < 0 || days > 365) {
            throw new InvalidContextRequestException("Upcoming window must be between 0 and 365 days");
        }
        LocalDate today = LocalDate.now();
        return timeline(workspaceId, today, today.plusDays(days), page, size);
    }

    private void validateEntity(UUID workspaceId, UUID entityId) {
        if (entityId != null && entities.findByIdAndWorkspaceId(entityId, workspaceId).isEmpty()) {
            throw new ContextRecordNotFoundException();
        }
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new InvalidContextRequestException("Page must be non-negative and page size must be between 1 and 100");
        }
    }

    private String cleanOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private EventResponse toResponse(LifeEvent event) {
        return new EventResponse(event.getId(), event.getEntityId(), event.getEventType(), event.getTitle(),
                event.getEventDate(), event.getNotes(), event.getStatus(), event.getCreatedAt());
    }

    public record EventResponse(UUID id, UUID entityId, LifeEventType type, String title,
                                LocalDate date, String notes, LifeEventStatus status,
                                java.time.Instant createdAt) { }
}
