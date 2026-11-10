package com.signaltrellis.actions;

import com.signaltrellis.identity.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/actions")
public class ActionController {

    private final ActionService actions;

    public ActionController(ActionService actions) {
        this.actions = actions;
    }

    @PostMapping("/proposals")
    public ActionService.ActionDetail propose(@AuthenticationPrincipal AuthenticatedUser user,
                                               @RequestHeader("Idempotency-Key") String idempotencyKey,
                                               @Valid @RequestBody CreateProposalRequest request) {
        return actions.propose(user, idempotencyKey, new ActionService.ProposalRequest(request.signalId(),
                request.title(), request.details(), request.scheduledFor(), request.expiresAt()));
    }

    @GetMapping("/pending")
    public Page<ActionService.ActionProposalResponse> pending(@AuthenticationPrincipal AuthenticatedUser user,
                                                               @RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "20") int size) {
        return actions.pending(user.workspaceId(), page, size);
    }

    @GetMapping
    public Page<ActionService.ActionProposalResponse> list(@AuthenticationPrincipal AuthenticatedUser user,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "20") int size) {
        return actions.list(user.workspaceId(), page, size);
    }

    @GetMapping("/history")
    public Page<ActionService.ActionHistoryResponse> history(@AuthenticationPrincipal AuthenticatedUser user,
                                                              @RequestParam(defaultValue = "0") int page,
                                                              @RequestParam(defaultValue = "20") int size) {
        return actions.history(user.workspaceId(), page, size);
    }

    @GetMapping("/{id}")
    public ActionService.ActionDetail get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return actions.get(user.workspaceId(), id);
    }

    @PostMapping("/{id}/approve")
    public ActionService.ActionDetail approve(@AuthenticationPrincipal AuthenticatedUser user,
                                               @PathVariable UUID id,
                                               @RequestBody(required = false) DecisionRequest request) {
        return actions.approve(user, id, request == null ? null : request.reason());
    }

    @PostMapping("/{id}/reject")
    public ActionService.ActionDetail reject(@AuthenticationPrincipal AuthenticatedUser user,
                                              @PathVariable UUID id,
                                              @RequestBody(required = false) DecisionRequest request) {
        return actions.reject(user, id, request == null ? null : request.reason());
    }

    public record CreateProposalRequest(UUID signalId, @NotBlank @Size(max = 160) String title,
                                        @Size(max = 1000) String details, @NotNull Instant scheduledFor,
                                        @NotNull Instant expiresAt) { }

    public record DecisionRequest(@Size(max = 500) String reason) { }
}
