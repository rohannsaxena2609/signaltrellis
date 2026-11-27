package com.signaltrellis.signals;

import com.signaltrellis.context.Commitment;
import com.signaltrellis.context.LifeEvent;
import com.signaltrellis.context.LifeEventType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignalRuleEngineTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 14);
    private static final UUID WORKSPACE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final SignalRuleEngine engine = new SignalRuleEngine();

    @Test
    void identifiesOverdueAndUpcomingCommitmentsAtTheirDateBoundaries() {
        Commitment overdue = commitment("Overdue", TODAY.minusDays(2));
        Commitment dueToday = commitment("Due today", TODAY);
        Commitment dueTomorrow = commitment("Due tomorrow", TODAY.plusDays(1));
        Commitment dueInSevenDays = commitment("Due in seven days", TODAY.plusDays(7));

        List<SignalCandidate> candidates = engine.evaluate(
                List.of(overdue),
                List.of(dueToday, dueTomorrow, dueInSevenDays),
                List.of(),
                List.of(),
                TODAY
        );

        assertEquals(4, candidates.size());
        assertEquals(SignalPriority.HIGH, candidateFor(candidates, "commitment-overdue:").priority());
        assertTrue(candidateFor(candidates, "commitment-overdue:").explanation().contains("2 days ago"));
        assertEquals(SignalPriority.HIGH, candidateFor(candidates, "commitment-due-soon:" + dueToday.getId()).priority());
        assertEquals(SignalPriority.HIGH, candidateFor(candidates, "commitment-due-soon:" + dueTomorrow.getId()).priority());
        assertEquals(SignalPriority.MEDIUM, candidateFor(candidates, "commitment-due-soon:" + dueInSevenDays.getId()).priority());
    }

    @Test
    void groupsCommitmentsWhenConsecutiveDueDatesAreAtMostThreeDaysApart() {
        Commitment first = commitment("First", TODAY.plusDays(1));
        Commitment second = commitment("Second", TODAY.plusDays(4));
        Commitment third = commitment("Third", TODAY.plusDays(7));
        Commitment fourth = commitment("Fourth", TODAY.plusDays(15));
        Commitment fifth = commitment("Fifth", TODAY.plusDays(18));

        List<Commitment> commitments = List.of(first, second, third, fourth, fifth);
        List<SignalCandidate> candidates = engine.evaluate(List.of(), List.of(), commitments, List.of(), TODAY);
        List<SignalCandidate> clusters = candidates.stream()
                .filter(candidate -> candidate.type() == SignalType.PAYMENT_CLUSTER)
                .sorted(Comparator.comparing(SignalCandidate::title).reversed())
                .toList();

        assertEquals(2, clusters.size());
        assertEquals("3 commitments due close together", clusters.get(0).title());
        assertEquals(SignalPriority.HIGH, clusters.get(0).priority());
        assertEquals("2 commitments due close together", clusters.get(1).title());
        assertEquals(SignalPriority.LOW, clusters.get(1).priority());

        List<String> originalKeys = clusters.stream().map(SignalCandidate::dedupeKey).sorted().toList();
        List<String> reversedKeys = engine.evaluate(List.of(), List.of(),
                        List.of(fifth, fourth, third, second, first), List.of(), TODAY)
                .stream()
                .filter(candidate -> candidate.type() == SignalType.PAYMENT_CLUSTER)
                .map(SignalCandidate::dedupeKey)
                .sorted()
                .toList();
        assertEquals(originalKeys, reversedKeys);
    }

    @Test
    void assignsEventPrioritiesAndMarksPastEventsAsMissed() {
        LifeEvent yesterday = event("Yesterday", TODAY.minusDays(1));
        LifeEvent today = event("Today", TODAY);
        LifeEvent tomorrow = event("Tomorrow", TODAY.plusDays(1));
        LifeEvent inSevenDays = event("In seven days", TODAY.plusDays(7));
        LifeEvent inEightDays = event("In eight days", TODAY.plusDays(8));

        List<SignalCandidate> candidates = engine.evaluate(
                List.of(), List.of(), List.of(),
                List.of(yesterday, today, tomorrow, inSevenDays, inEightDays),
                TODAY
        );

        assertEquals(5, candidates.size());
        assertEquals(SignalPriority.MEDIUM, candidateFor(candidates, "event_missed:").priority());
        assertEquals(SignalPriority.HIGH, candidateFor(candidates, "event_approaching:" + today.getId()).priority());
        assertEquals(SignalPriority.HIGH, candidateFor(candidates, "event_approaching:" + tomorrow.getId()).priority());
        assertEquals(SignalPriority.MEDIUM, candidateFor(candidates, "event_approaching:" + inSevenDays.getId()).priority());
        assertEquals(SignalPriority.LOW, candidateFor(candidates, "event_approaching:" + inEightDays.getId()).priority());
    }

    @Test
    void limitsGeneratedCommitmentTitlesToTheSupportedLength() {
        Commitment longTitle = commitment("x".repeat(160), TODAY.plusDays(2));

        SignalCandidate candidate = engine.evaluate(
                List.of(), List.of(longTitle), List.of(), List.of(), TODAY
        ).get(0);

        assertEquals(160, candidate.title().length());
    }

    @Test
    void truncatesGeneratedCommitmentTitlesThatExceedTheSupportedLength() {
        Commitment longTitle = commitment("x".repeat(161), TODAY.plusDays(2));

        SignalCandidate candidate = engine.evaluate(
                List.of(), List.of(longTitle), List.of(), List.of(), TODAY
        ).get(0);

        assertEquals(160, candidate.title().length());
        assertEquals("x".repeat(160), candidate.title());
    }

    private Commitment commitment(String title, LocalDate dueDate) {
        return new Commitment(WORKSPACE_ID, null, title, null, null, dueDate, null);
    }

    private LifeEvent event(String title, LocalDate eventDate) {
        return new LifeEvent(WORKSPACE_ID, null, LifeEventType.OTHER, title, eventDate, null);
    }

    private SignalCandidate candidateFor(List<SignalCandidate> candidates, String dedupeKey) {
        return candidates.stream()
                .filter(candidate -> candidate.dedupeKey().startsWith(dedupeKey))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing signal candidate: " + dedupeKey));
    }
}
