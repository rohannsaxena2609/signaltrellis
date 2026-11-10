package com.signaltrellis.actions;

import com.signaltrellis.identity.AuthenticatedUser;
import com.signaltrellis.identity.WorkspaceRepository;
import com.signaltrellis.signals.Signal;
import com.signaltrellis.signals.SignalRepository;
import com.signaltrellis.signals.SignalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class ActionService {

    private final ActionProposalRepository proposals;
    private final ActionApprovalRepository approvals;
    private final ReminderRepository reminders;
    private final ActionHistoryRepository history;
    private final SignalRepository signals;
    private final WorkspaceRepository workspaces;
    private final Clock clock;

    public ActionService(ActionProposalRepository proposals, ActionApprovalRepository approvals,
                         ReminderRepository reminders, ActionHistoryRepository history,
                         SignalRepository signals, WorkspaceRepository workspaces, Clock clock) {
        this.proposals = proposals;
        this.approvals = approvals;
        this.reminders = reminders;
        this.history = history;
        this.signals = signals;
        this.workspaces = workspaces;
        this.clock = clock;
    }

    @Transactional
    public ActionDetail propose(AuthenticatedUser actor, String idempotencyKey, ProposalRequest request) {
        Instant now = clock.instant();
        String key = normalizeIdempotencyKey(idempotencyKey);
        String title = request.title().trim();
        String details = cleanOptional(request.details());
        if (!request.scheduledFor().isAfter(now)) {
            throw new InvalidActionRequestException("Reminder time must be in the future");
        }
        if (!request.expiresAt().isAfter(now) || !request.expiresAt().isBefore(request.scheduledFor())) {
            throw new InvalidActionRequestException("Approval expiry must be in the future and before the reminder time");
        }

        UUID workspaceId = actor.workspaceId();
        workspaces.lockById(workspaceId).orElseThrow(ActionRecordNotFoundException::new);
        String hash = requestHash(request.signalId(), title, details, request.scheduledFor(), request.expiresAt());
        ActionProposal existing = proposals.findByWorkspaceIdAndIdempotencyKey(workspaceId, key).orElse(null);
        if (existing != null) {
            if (!MessageDigest.isEqual(existing.getRequestHash().getBytes(StandardCharsets.US_ASCII),
                    hash.getBytes(StandardCharsets.US_ASCII))) {
                throw new ActionConflictException("Idempotency key was already used for a different request");
            }
            return toDetail(existing);
        }

        if (request.signalId() != null) {
            Signal signal = signals.findByIdAndWorkspaceId(request.signalId(), workspaceId)
                    .orElseThrow(ActionRecordNotFoundException::new);
            if (signal.getStatus() != SignalStatus.ACTIVE) {
                throw new InvalidActionRequestException("An action can only be proposed for an active signal");
            }
        }

        ActionProposal proposal = proposals.saveAndFlush(new ActionProposal(workspaceId, request.signalId(), actor.id(),
                ActionType.SCHEDULE_REMINDER, title, details, request.scheduledFor(), request.expiresAt(),
                key, hash, now));
        recordHistory(proposal, actor.id(), ActionHistoryType.PROPOSED, null,
                ActionProposalStatus.PENDING_APPROVAL, "Awaiting user approval", now);
        return toDetail(proposal);
    }

    @Transactional(readOnly = true)
    public Page<ActionProposalResponse> pending(UUID workspaceId, int page, int size) {
        validatePage(page, size);
        return proposals.findByWorkspaceIdAndStatusOrderByCreatedAtDescIdDesc(workspaceId,
                        ActionProposalStatus.PENDING_APPROVAL, PageRequest.of(page, size))
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ActionProposalResponse> list(UUID workspaceId, int page, int size) {
        validatePage(page, size);
        return proposals.findByWorkspaceIdOrderByCreatedAtDescIdDesc(workspaceId, PageRequest.of(page, size))
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ActionDetail get(UUID workspaceId, UUID proposalId) {
        return toDetail(findProposal(workspaceId, proposalId));
    }

    @Transactional(readOnly = true)
    public Page<ActionHistoryResponse> history(UUID workspaceId, int page, int size) {
        validatePage(page, size);
        return history.findByWorkspaceIdOrderByOccurredAtDescIdDesc(workspaceId,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "occurredAt", "id")))
                .map(this::toResponse);
    }

    @Transactional
    public ActionDetail approve(AuthenticatedUser actor, UUID proposalId, String reason) {
        return decide(actor, proposalId, ApprovalDecision.APPROVE, reason);
    }

    @Transactional
    public ActionDetail reject(AuthenticatedUser actor, UUID proposalId, String reason) {
        return decide(actor, proposalId, ApprovalDecision.REJECT, reason);
    }

    @Scheduled(fixedDelayString = "${signaltrellis.actions.expiration-scan:PT1M}")
    @Transactional
    public void expirePendingProposals() {
        Instant now = clock.instant();
        Page<ActionProposal> expiredPage = proposals.findByStatusAndExpiresAtLessThanEqualOrderByExpiresAtAsc(
                ActionProposalStatus.PENDING_APPROVAL, now, PageRequest.of(0, 100));
        for (ActionProposal stale : expiredPage.getContent()) {
            if (workspaces.lockById(stale.getWorkspaceId()).isEmpty()) {
                continue;
            }
            ActionProposal proposal = proposals.lockByIdAndWorkspaceId(stale.getId(), stale.getWorkspaceId()).orElse(null);
            if (proposal != null && proposal.getStatus() == ActionProposalStatus.PENDING_APPROVAL
                    && !proposal.getExpiresAt().isAfter(now)) {
                proposal.expire(now);
                proposals.save(proposal);
                recordHistory(proposal, null, ActionHistoryType.EXPIRED, ActionProposalStatus.PENDING_APPROVAL,
                        ActionProposalStatus.EXPIRED, "Approval window expired", now);
            }
        }
    }

    private ActionDetail decide(AuthenticatedUser actor, UUID proposalId, ApprovalDecision decision, String reason) {
        UUID workspaceId = actor.workspaceId();
        workspaces.lockById(workspaceId).orElseThrow(ActionRecordNotFoundException::new);
        ActionProposal proposal = proposals.lockByIdAndWorkspaceId(proposalId, workspaceId)
                .orElseThrow(ActionRecordNotFoundException::new);
        Instant now = clock.instant();
        ActionProposalStatus expectedStatus = decision == ApprovalDecision.APPROVE
                ? ActionProposalStatus.APPROVED : ActionProposalStatus.REJECTED;

        if (proposal.getStatus() == expectedStatus) {
            return toDetail(proposal);
        }
        if (proposal.getStatus() != ActionProposalStatus.PENDING_APPROVAL) {
            throw new ActionConflictException("This proposal has already been decided or expired");
        }
        if (!proposal.getExpiresAt().isAfter(now)) {
            proposal.expire(now);
            proposals.save(proposal);
            recordHistory(proposal, null, ActionHistoryType.EXPIRED, ActionProposalStatus.PENDING_APPROVAL,
                    ActionProposalStatus.EXPIRED, "Approval window expired", now);
            return toDetail(proposal);
        }

        String normalizedReason = cleanOptional(reason);
        approvals.save(new ActionApproval(workspaceId, proposalId, actor.id(), decision,
                normalizedReason, now));
        if (decision == ApprovalDecision.APPROVE) {
            proposal.approve(now);
        } else {
            proposal.reject(now);
        }
        proposals.save(proposal);
        if (decision == ApprovalDecision.APPROVE) {
            Reminder reminder = reminders.save(new Reminder(workspaceId, proposalId, proposal.getTitle(),
                    proposal.getScheduledFor(), now));
            recordHistory(proposal, actor.id(), ActionHistoryType.APPROVED,
                    ActionProposalStatus.PENDING_APPROVAL, ActionProposalStatus.APPROVED,
                    normalizedReason == null ? "Approved by user" : normalizedReason, now);
            recordHistory(proposal, actor.id(), ActionHistoryType.REMINDER_SCHEDULED,
                    ActionProposalStatus.APPROVED, ActionProposalStatus.APPROVED,
                    "Reminder " + reminder.getId() + " scheduled", now);
        } else {
            recordHistory(proposal, actor.id(), ActionHistoryType.REJECTED,
                    ActionProposalStatus.PENDING_APPROVAL, ActionProposalStatus.REJECTED,
                    normalizedReason == null ? "Rejected by user" : normalizedReason, now);
        }
        return toDetail(proposal);
    }

    private void recordHistory(ActionProposal proposal, UUID actorId, ActionHistoryType eventType,
                               ActionProposalStatus oldStatus, ActionProposalStatus newStatus,
                               String detail, Instant occurredAt) {
        history.save(new ActionHistory(proposal.getWorkspaceId(), proposal.getId(), actorId,
                eventType, oldStatus, newStatus, detail, occurredAt));
    }

    private ActionProposal findProposal(UUID workspaceId, UUID proposalId) {
        return proposals.findByIdAndWorkspaceId(proposalId, workspaceId)
                .orElseThrow(ActionRecordNotFoundException::new);
    }

    private ActionDetail toDetail(ActionProposal proposal) {
        ActionApproval approval = approvals.findByProposalIdAndWorkspaceId(proposal.getId(), proposal.getWorkspaceId())
                .orElse(null);
        Reminder reminder = reminders.findByProposalIdAndWorkspaceId(proposal.getId(), proposal.getWorkspaceId())
                .orElse(null);
        List<ActionHistoryResponse> entries = history.findByProposalIdAndWorkspaceIdOrderByOccurredAtAsc(
                        proposal.getId(), proposal.getWorkspaceId()).stream()
                .map(this::toResponse)
                .toList();
        return new ActionDetail(toResponse(proposal), approval == null ? null : toResponse(approval),
                reminder == null ? null : toResponse(reminder), entries);
    }

    private ActionProposalResponse toResponse(ActionProposal proposal) {
        return new ActionProposalResponse(proposal.getId(), proposal.getSignalId(), proposal.getActionType(),
                proposal.getTitle(), proposal.getDetails(), proposal.getScheduledFor(), proposal.getExpiresAt(),
                proposal.getStatus(), proposal.getRequestedBy(), proposal.getCreatedAt(), proposal.getUpdatedAt());
    }

    private ActionApprovalResponse toResponse(ActionApproval approval) {
        return new ActionApprovalResponse(approval.getDecision(), approval.getActorUserId(),
                approval.getReason(), approval.getDecidedAt());
    }

    private ReminderResponse toResponse(Reminder reminder) {
        return new ReminderResponse(reminder.getId(), reminder.getTitle(), reminder.getScheduledFor(),
                reminder.getStatus(), reminder.getCreatedAt());
    }

    private ActionHistoryResponse toResponse(ActionHistory entry) {
        return new ActionHistoryResponse(entry.getId(), entry.getProposalId(), entry.getActorUserId(),
                entry.getEventType(), entry.getPreviousStatus(), entry.getNewStatus(),
                entry.getDetail(), entry.getOccurredAt());
    }

    private String normalizeIdempotencyKey(String key) {
        if (key == null || key.isBlank() || key.trim().length() > 100) {
            throw new InvalidActionRequestException("Idempotency-Key must contain 1 to 100 characters");
        }
        return key.trim();
    }

    private String requestHash(UUID signalId, String title, String details, Instant scheduledFor, Instant expiresAt) {
        String canonical = String.join("\n", ActionType.SCHEDULE_REMINDER.name(),
                signalId == null ? "" : signalId.toString(), title, details == null ? "" : details,
                scheduledFor.toString(), expiresAt.toString());
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String cleanOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new InvalidActionRequestException("Page must be non-negative and page size must be between 1 and 100");
        }
    }

    public record ProposalRequest(UUID signalId, String title, String details,
                                  Instant scheduledFor, Instant expiresAt) { }

    public record ActionProposalResponse(UUID id, UUID signalId, ActionType type, String title, String details,
                                         Instant scheduledFor, Instant expiresAt, ActionProposalStatus status,
                                         UUID requestedBy, Instant createdAt, Instant updatedAt) { }

    public record ActionApprovalResponse(ApprovalDecision decision, UUID actorUserId,
                                         String reason, Instant decidedAt) { }

    public record ReminderResponse(UUID id, String title, Instant scheduledFor,
                                   ReminderStatus status, Instant createdAt) { }

    public record ActionHistoryResponse(UUID id, UUID proposalId, UUID actorUserId,
                                        ActionHistoryType eventType, ActionProposalStatus previousStatus,
                                        ActionProposalStatus newStatus, String detail, Instant occurredAt) { }

    public record ActionDetail(ActionProposalResponse proposal, ActionApprovalResponse approval,
                               ReminderResponse reminder, List<ActionHistoryResponse> history) { }
}
