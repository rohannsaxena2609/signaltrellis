package com.signaltrellis.context;

import com.signaltrellis.identity.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events")
public class LifeEventController {

    private final LifeEventService events;

    public LifeEventController(LifeEventService events) {
        this.events = events;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LifeEventService.EventResponse create(@AuthenticationPrincipal AuthenticatedUser user,
                                                  @Valid @RequestBody CreateEventRequest request) {
        return events.create(user.workspaceId(), request.type(), request.title(), request.date(),
                request.notes(), request.entityId());
    }

    @GetMapping("/timeline")
    public Page<LifeEventService.EventResponse> timeline(@AuthenticationPrincipal AuthenticatedUser user,
                                                          @RequestParam(required = false) LocalDate from,
                                                          @RequestParam(required = false) LocalDate to,
                                                          @RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "20") int size) {
        return events.timeline(user.workspaceId(), from, to, page, size);
    }

    @GetMapping("/upcoming")
    public Page<LifeEventService.EventResponse> upcoming(@AuthenticationPrincipal AuthenticatedUser user,
                                                          @RequestParam(defaultValue = "30") int days,
                                                          @RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "20") int size) {
        return events.upcoming(user.workspaceId(), days, page, size);
    }

    public record CreateEventRequest(@NotNull LifeEventType type,
                                     @NotBlank @Size(max = 160) String title,
                                     @NotNull LocalDate date,
                                     @Size(max = 2000) String notes,
                                     UUID entityId) { }
}
