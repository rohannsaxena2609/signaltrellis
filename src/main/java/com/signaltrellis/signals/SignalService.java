package com.signaltrellis.signals;

import com.signaltrellis.context.Commitment;
import com.signaltrellis.context.CommitmentRepository;
import com.signaltrellis.context.CommitmentStatus;
import com.signaltrellis.context.LifeEvent;
import com.signaltrellis.context.LifeEventRepository;
import com.signaltrellis.context.LifeEventStatus;
import com.signaltrellis.identity.AuthenticatedUser;
import com.signaltrellis.identity.WorkspaceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SignalService {

    private static final List<SignalStatus> RECONCILABLE_STATUSES = List.of(SignalStatus.ACTIVE, SignalStatus.DISMISSED);

    private final SignalRepository signals;
    private final SignalEvidenceRepository evidence;
    private final SignalHistoryRepository history;
    private final CommitmentRepository commitments;
    private final LifeEventRepository events;
    private final WorkspaceRepository workspaces;
    private final SignalRuleEngine ruleEngine;

    public SignalService(SignalRepository signals, SignalEvidenceRepository evidence,
                         SignalHistoryRepository history, CommitmentRepository commitments,
                         LifeEventRepository events, WorkspaceRepository workspaces,
                         SignalRuleEngine ruleEngine) {
        this.signals = signals;
        this.evidence = evidence;
        this.history = history;
        this.commitments = commitments;
        this.events = events;
        this.workspaces = workspaces;
        this.ruleEngine = ruleEngine;
    }

    @Transactional
    public EvaluationResponse recompute(AuthenticatedUser actor) {
        UUID workspaceId = actor.workspaceId();
        workspaces.lockById(workspaceId).orElseThrow(SignalNotFoundException::new);

        LocalDate today = LocalDate.now();
        List<Commitment> overdue = commitments.findByWorkspaceIdAndStatusAndDueDateBeforeOrderByDueDateAscIdAsc(
                workspaceId, CommitmentStatus.OPEN, today);
        List<Commitment> dueSoon = commitments.findByWorkspaceIdAndStatusAndDueDateBetweenOrderByDueDateAscIdAsc(
                workspaceId, CommitmentStatus.OPEN, today, today.plusDays(7));
        List<Commitment> clusterWindow = commitments.findByWorkspaceIdAndStatusAndDueDateBetweenOrderByDueDateAscIdAsc(
                workspaceId, CommitmentStatus.OPEN, today, today.plusDays(30));
        List<LifeEvent> eventWindow = events.findByWorkspaceIdAndStatusAndEventDateBetweenOrderByEventDateAscIdAsc(
                workspaceId, LifeEventStatus.SCHEDULED, today.minusDays(7), today.plusDays(30));

        List<SignalCandidate> candidates = ruleEngine.evaluate(overdue, dueSoon, clusterWindow, eventWindow, today);
        Map<String, SignalCandidate> desired = new HashMap<>();
        candidates.forEach(candidate -> desired.put(candidate.dedupeKey(), candidate));
        Map<String, Signal> existingByKey = new HashMap<>();
        if (!desired.isEmpty()) {
            signals.findByWorkspaceIdAndDedupeKeyIn(workspaceId, desired.keySet()).forEach(signal ->
                    existingByKey.put(signal.getDedupeKey(), signal));
        }

        Instant now = Instant.now();
        int detected = 0;
        int reopened = 0;
        int resolved = 0;
        for (SignalCandidate candidate : desired.values()) {
            Signal signal = existingByKey.get(candidate.dedupeKey());
            SignalHistoryType historyType = null;
            SignalStatus oldStatus = null;
            if (signal == null) {
                signal = signals.save(new Signal(workspaceId, candidate.dedupeKey(), candidate.type(),
                        candidate.priority(), candidate.title(), candidate.explanation(), now));
                historyType = SignalHistoryType.DETECTED;
                detected++;
            } else if (signal.getStatus() == SignalStatus.RESOLVED) {
                oldStatus = SignalStatus.RESOLVED;
                signal.reopen(candidate.priority(), candidate.title(), candidate.explanation(), now);
                historyType = SignalHistoryType.REOPENED;
                reopened++;
            } else {
                signal.refresh(candidate.priority(), candidate.title(), candidate.explanation(), now);
            }
            signals.saveAndFlush(signal);
            if (historyType != null) {
                history.save(new SignalHistory(workspaceId, signal.getId(), actor.id(), historyType,
                        oldStatus, SignalStatus.ACTIVE,
                        historyType == SignalHistoryType.DETECTED ? "Rule matched" : "Rule matched again", now));
            }
            UUID signalId = signal.getId();
            evidence.deleteBySignalIdAndWorkspaceId(signalId, workspaceId);
            List<SignalEvidence> currentEvidence = candidate.evidence().stream()
                    .map(item -> new SignalEvidence(workspaceId, signalId, item.type(), item.eventId(),
                            item.commitmentId(), item.summary(), item.observedValue()))
                    .toList();
            evidence.saveAll(currentEvidence);
        }

        for (Signal signal : signals.findByWorkspaceIdAndStatusIn(workspaceId, RECONCILABLE_STATUSES)) {
            if (!desired.containsKey(signal.getDedupeKey())) {
                SignalStatus previous = signal.getStatus();
                signal.setStatus(SignalStatus.RESOLVED);
                signal.markEvaluated(now);
                history.save(new SignalHistory(workspaceId, signal.getId(), actor.id(), SignalHistoryType.RESOLVED,
                        previous, SignalStatus.RESOLVED, "Rule no longer matches", now));
                resolved++;
            }
        }
        long active = signals.countByWorkspaceIdAndStatus(workspaceId, SignalStatus.ACTIVE);
        return new EvaluationResponse(detected, reopened, resolved, active, now);
    }

    @Transactional(readOnly = true)
    public Page<SignalResponse> search(UUID workspaceId, SignalStatus status, SignalPriority priority,
                                       SignalType type, int page, int size) {
        validatePage(page, size);
        return signals.search(workspaceId, status, priority, type,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "lastDetectedAt", "id")))
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public SignalDetail get(UUID workspaceId, UUID signalId) {
        Signal signal = findSignal(workspaceId, signalId);
        return new SignalDetail(toResponse(signal), evidenceFor(workspaceId, signalId), historyFor(workspaceId, signalId));
    }

    @Transactional(readOnly = true)
    public List<EvidenceResponse> getEvidence(UUID workspaceId, UUID signalId) {
        findSignal(workspaceId, signalId);
        return evidenceFor(workspaceId, signalId);
    }

    @Transactional
    public SignalResponse dismiss(UUID workspaceId, UUID signalId, UUID actorId) {
        return transition(workspaceId, signalId, actorId, SignalStatus.DISMISSED);
    }

    @Transactional
    public SignalResponse resolve(UUID workspaceId, UUID signalId, UUID actorId) {
        return transition(workspaceId, signalId, actorId, SignalStatus.RESOLVED);
    }

    private SignalResponse transition(UUID workspaceId, UUID signalId, UUID actorId, SignalStatus target) {
        workspaces.lockById(workspaceId).orElseThrow(SignalNotFoundException::new);
        Signal signal = findSignal(workspaceId, signalId);
        SignalStatus previous = signal.getStatus();
        if (previous == SignalStatus.RESOLVED && target == SignalStatus.DISMISSED) {
            throw new InvalidSignalTransitionException("A resolved signal cannot be dismissed");
        }
        if (previous != target) {
            signal.setStatus(target);
            Instant now = Instant.now();
            signal.markEvaluated(now);
            history.save(new SignalHistory(workspaceId, signalId, actorId,
                    target == SignalStatus.DISMISSED ? SignalHistoryType.DISMISSED : SignalHistoryType.RESOLVED,
                    previous, target, "Updated by user", now));
        }
        return toResponse(signal);
    }

    private Signal findSignal(UUID workspaceId, UUID signalId) {
        return signals.findByIdAndWorkspaceId(signalId, workspaceId).orElseThrow(SignalNotFoundException::new);
    }

    private List<EvidenceResponse> evidenceFor(UUID workspaceId, UUID signalId) {
        return evidence.findBySignalIdAndWorkspaceIdOrderByCreatedAtAsc(signalId, workspaceId).stream()
                .map(item -> new EvidenceResponse(item.getId(), item.getEvidenceType(), item.getEventId(),
                        item.getCommitmentId(), item.getSummary(), item.getObservedValue(), item.getCreatedAt()))
                .toList();
    }

    private List<HistoryResponse> historyFor(UUID workspaceId, UUID signalId) {
        return history.findBySignalIdAndWorkspaceIdOrderByOccurredAtAsc(signalId, workspaceId).stream()
                .map(item -> new HistoryResponse(item.getId(), item.getActorUserId(), item.getEventType(),
                        item.getPreviousStatus(), item.getNewStatus(), item.getDetail(), item.getOccurredAt()))
                .toList();
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new InvalidSignalRequestException("Page must be non-negative and page size must be between 1 and 100");
        }
    }

    private SignalResponse toResponse(Signal signal) {
        return new SignalResponse(signal.getId(), signal.getSignalType(), signal.getPriority(), signal.getStatus(),
                signal.getTitle(), signal.getExplanation(), signal.getDetectedAt(),
                signal.getLastDetectedAt(), signal.getLastEvaluatedAt());
    }

    public record EvaluationResponse(int detected, int reopened, int resolved, long activeCount, Instant evaluatedAt) { }

    public record SignalResponse(UUID id, SignalType type, SignalPriority priority, SignalStatus status,
                                 String title, String explanation, Instant detectedAt, Instant lastDetectedAt,
                                 Instant lastEvaluatedAt) { }

    public record EvidenceResponse(UUID id, SignalEvidenceType type, UUID eventId, UUID commitmentId,
                                  String summary, String observedValue, Instant createdAt) { }

    public record HistoryResponse(UUID id, UUID actorUserId, SignalHistoryType eventType,
                                  SignalStatus previousStatus, SignalStatus newStatus,
                                  String detail, Instant occurredAt) { }

    public record SignalDetail(SignalResponse signal, List<EvidenceResponse> evidence,
                               List<HistoryResponse> history) { }
}
