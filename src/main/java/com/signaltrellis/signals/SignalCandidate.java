package com.signaltrellis.signals;

import java.util.List;
import java.util.UUID;

public record SignalCandidate(String dedupeKey, SignalType type, SignalPriority priority,
                              String title, String explanation, List<EvidenceCandidate> evidence) {

    public record EvidenceCandidate(SignalEvidenceType type, UUID eventId, UUID commitmentId,
                                    String summary, String observedValue) { }
}
