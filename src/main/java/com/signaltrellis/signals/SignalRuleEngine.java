package com.signaltrellis.signals;

import com.signaltrellis.context.Commitment;
import com.signaltrellis.context.LifeEvent;
import com.signaltrellis.context.LifeEventStatus;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Component
public class SignalRuleEngine {

    public List<SignalCandidate> evaluate(List<Commitment> overdueCommitments,
                                          List<Commitment> dueSoonCommitments,
                                          List<Commitment> clusterCommitments,
                                          List<LifeEvent> events, LocalDate today) {
        List<SignalCandidate> candidates = new ArrayList<>();
        for (Commitment commitment : overdueCommitments) {
            long days = ChronoUnit.DAYS.between(commitment.getDueDate(), today);
            candidates.add(commitmentSignal(commitment, "commitment-overdue:",
                    SignalType.COMMITMENT_OVERDUE, SignalPriority.HIGH,
                    "Overdue: " + commitment.getTitle(),
                    "This open commitment was due " + commitment.getDueDate() + " (" + days + " days ago)."));
        }
        for (Commitment commitment : dueSoonCommitments) {
            long days = ChronoUnit.DAYS.between(today, commitment.getDueDate());
            SignalPriority priority = days <= 1 ? SignalPriority.HIGH : SignalPriority.MEDIUM;
            String timing = days == 0 ? "today" : days == 1 ? "tomorrow" : "in " + days + " days";
            candidates.add(commitmentSignal(commitment, "commitment-due-soon:",
                    SignalType.COMMITMENT_DUE_SOON, priority,
                    "Due soon: " + commitment.getTitle(),
                    "This open commitment is due " + timing + " (" + commitment.getDueDate() + ")."));
        }
        candidates.addAll(clusterSignals(clusterCommitments, today));
        for (LifeEvent event : events) {
            if (event.getStatus() != LifeEventStatus.SCHEDULED) continue;
            long days = ChronoUnit.DAYS.between(today, event.getEventDate());
            if (days < 0) {
                candidates.add(eventSignal(event, SignalType.EVENT_MISSED, SignalPriority.MEDIUM,
                        "Scheduled event passed: " + event.getTitle(),
                        "This event was scheduled for " + event.getEventDate() + " and remains scheduled."));
            } else {
                SignalPriority priority = days <= 1 ? SignalPriority.HIGH
                        : days <= 7 ? SignalPriority.MEDIUM : SignalPriority.LOW;
                String timing = days == 0 ? "today" : days == 1 ? "tomorrow" : "in " + days + " days";
                candidates.add(eventSignal(event, SignalType.EVENT_APPROACHING, priority,
                        "Coming up: " + event.getTitle(),
                        "This scheduled event is " + timing + " (" + event.getEventDate() + ")."));
            }
        }
        return candidates;
    }

    private List<SignalCandidate> clusterSignals(List<Commitment> commitments, LocalDate today) {
        List<Commitment> sorted = commitments.stream()
                .sorted(Comparator.comparing(Commitment::getDueDate).thenComparing(Commitment::getId))
                .toList();
        List<SignalCandidate> result = new ArrayList<>();
        int start = 0;
        while (start < sorted.size()) {
            int end = start + 1;
            while (end < sorted.size()
                    && ChronoUnit.DAYS.between(sorted.get(end - 1).getDueDate(), sorted.get(end).getDueDate()) <= 3) {
                end++;
            }
            if (end - start >= 2) {
                List<Commitment> group = sorted.subList(start, end);
                LocalDate first = group.getFirst().getDueDate();
                LocalDate last = group.getLast().getDueDate();
                long daysToFirst = ChronoUnit.DAYS.between(today, first);
                SignalPriority priority = group.size() >= 3 && daysToFirst <= 7
                        ? SignalPriority.HIGH : daysToFirst <= 7 ? SignalPriority.MEDIUM : SignalPriority.LOW;
                String title = group.size() + " commitments due close together";
                String explanation = "These open commitments are due between " + first + " and " + last
                        + ", with no more than three days between consecutive due dates.";
                List<SignalCandidate.EvidenceCandidate> evidence = group.stream()
                        .map(this::commitmentEvidence).toList();
                result.add(new SignalCandidate("payment-cluster:" + clusterHash(group), SignalType.PAYMENT_CLUSTER,
                        priority, title, explanation, evidence));
            }
            start = end;
        }
        return result;
    }

    private SignalCandidate commitmentSignal(Commitment commitment, String keyPrefix, SignalType type,
                                             SignalPriority priority, String title, String explanation) {
        return new SignalCandidate(keyPrefix + commitment.getId(), type, priority,
                limit(title, 160), explanation,
                List.of(commitmentEvidence(commitment)));
    }

    private SignalCandidate eventSignal(LifeEvent event, SignalType type, SignalPriority priority,
                                        String title, String explanation) {
        var evidence = new SignalCandidate.EvidenceCandidate(SignalEvidenceType.EVENT, event.getId(), null,
                "Scheduled life event", event.getTitle() + " — " + event.getEventDate());
        return new SignalCandidate(type.name().toLowerCase() + ":" + event.getId(), type, priority,
                limit(title, 160), explanation, List.of(evidence));
    }

    private SignalCandidate.EvidenceCandidate commitmentEvidence(Commitment commitment) {
        String amount = commitment.getAmount() == null ? "Amount not recorded"
                : commitment.getAmount().toPlainString() + " " + commitment.getCurrency();
        return new SignalCandidate.EvidenceCandidate(SignalEvidenceType.COMMITMENT, null, commitment.getId(),
                "Open commitment due " + commitment.getDueDate(), commitment.getTitle() + " — " + amount);
    }

    private String clusterHash(List<Commitment> commitments) {
        String ids = commitments.stream().map(Commitment::getId).map(UUID::toString).sorted()
                .reduce((left, right) -> left + ":" + right).orElseThrow();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(ids.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 16);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String limit(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
