package com.signaltrellis.assistant;

import com.signaltrellis.actions.ActionService;
import com.signaltrellis.context.CommitmentService;
import com.signaltrellis.context.CommitmentStatus;
import com.signaltrellis.identity.AuthenticatedUser;
import com.signaltrellis.retrieval.DocumentRetrievalService;
import com.signaltrellis.signals.SignalService;
import com.signaltrellis.signals.SignalStatus;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Service
public class AssistantCapabilities {

    private final DocumentRetrievalService retrieval;
    private final CommitmentService commitments;
    private final SignalService signals;
    private final ActionService actions;
    private final Clock clock;

    public AssistantCapabilities(DocumentRetrievalService retrieval, CommitmentService commitments,
                                 SignalService signals, ActionService actions, Clock clock) {
        this.retrieval = retrieval;
        this.commitments = commitments;
        this.signals = signals;
        this.actions = actions;
        this.clock = clock;
    }

    public DocumentRetrievalService.SearchResponse searchDocuments(AuthenticatedUser actor,
                                                                    String query, int limit) {
        return retrieval.search(actor.workspaceId(), query, Math.min(Math.max(limit, 1), 10));
    }

    public List<UpcomingCommitment> upcomingCommitments(AuthenticatedUser actor, int daysAhead) {
        if (daysAhead < 0 || daysAhead > 365) {
            throw new IllegalArgumentException("daysAhead must be between 0 and 365");
        }
        LocalDate today = LocalDate.now(clock);
        return commitments.list(actor.workspaceId(), CommitmentStatus.OPEN, 0, 100).getContent().stream()
                .filter(item -> !item.dueDate().isBefore(today.minusDays(30)))
                .filter(item -> !item.dueDate().isAfter(today.plusDays(daysAhead)))
                .limit(30)
                .map(item -> new UpcomingCommitment(item.id(), item.title(), item.dueDate(), item.amount(),
                        item.currency(), item.status().name()))
                .toList();
    }

    public List<SignalService.SignalResponse> activeSignals(AuthenticatedUser actor, int limit) {
        Page<SignalService.SignalResponse> page = signals.search(actor.workspaceId(), SignalStatus.ACTIVE,
                null, null, 0, Math.min(Math.max(limit, 1), 20));
        return page.getContent();
    }

    public PreparedReminderProposal prepareReminderProposal(AuthenticatedUser actor, String title, String details,
                                                             Instant scheduledFor, Instant expiresAt,
                                                             UUID signalId) {
        if (title == null || title.isBlank() || title.trim().length() > 160) {
            throw new InvalidAssistantRequestException("Reminder title must be between 1 and 160 characters");
        }
        if (details != null && details.length() > 1000) {
            throw new InvalidAssistantRequestException("Reminder details must be at most 1000 characters");
        }
        String normalizedTitle = title.trim();
        String normalizedDetails = details == null || details.isBlank() ? null : details.trim();
        String proposalKeySource = actor.id() + "|" + actor.workspaceId() + "|" + signalId + "|" + normalizedTitle
                + "|" + normalizedDetails + "|" + scheduledFor + "|" + expiresAt;
        String idempotencyKey = "assistant-" + UUID.nameUUIDFromBytes(
                proposalKeySource.getBytes(StandardCharsets.UTF_8));
        ActionService.ActionDetail detail = actions.propose(actor, idempotencyKey,
                new ActionService.ProposalRequest(signalId, normalizedTitle, normalizedDetails, scheduledFor, expiresAt));
        return new PreparedReminderProposal(detail.proposal().id(), detail.proposal().title(),
                detail.proposal().status().name(), detail.proposal().scheduledFor(), detail.proposal().expiresAt(), true);
    }

    public record UpcomingCommitment(UUID id, String title, LocalDate dueDate,
                                     java.math.BigDecimal amount, String currency, String status) { }
    public record PreparedReminderProposal(UUID proposalId, String title, String status,
                                           Instant scheduledFor, Instant approvalExpiresAt,
                                           boolean userApprovalRequired) { }
}
