package com.signaltrellis.capture;

import com.signaltrellis.context.CommitmentRepository;
import com.signaltrellis.context.ContextRecordNotFoundException;
import com.signaltrellis.context.DuplicateContextRecordException;
import com.signaltrellis.context.LifeEntityRepository;
import com.signaltrellis.context.LifeEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class FactContextLinkService {

    private final DocumentExtractionRepository extractions;
    private final ExtractedFactRepository facts;
    private final ExtractedFactContextLinkRepository links;
    private final LifeEntityRepository entities;
    private final LifeEventRepository events;
    private final CommitmentRepository commitments;
    private final Clock clock;

    public FactContextLinkService(DocumentExtractionRepository extractions, ExtractedFactRepository facts,
                                  ExtractedFactContextLinkRepository links, LifeEntityRepository entities,
                                  LifeEventRepository events, CommitmentRepository commitments, Clock clock) {
        this.extractions = extractions;
        this.facts = facts;
        this.links = links;
        this.entities = entities;
        this.events = events;
        this.commitments = commitments;
        this.clock = clock;
    }

    @Transactional
    public FactContextLinkResponse link(UUID workspaceId, UUID userId, UUID documentId,
                                        UUID factId, LinkRequest request) {
        ExtractedFact fact = findFact(workspaceId, documentId, factId);
        if (fact.getReviewStatus() != FactReviewStatus.CONFIRMED
                && fact.getReviewStatus() != FactReviewStatus.CORRECTED) {
            throw new InvalidDocumentException("Only confirmed or corrected facts can be linked to context records");
        }
        verifyTarget(workspaceId, request.targetType(), request.targetId());
        if (links.existsByWorkspaceIdAndFactIdAndTargetTypeAndTargetId(
                workspaceId, factId, request.targetType(), request.targetId())) {
            throw new DuplicateContextRecordException("This fact is already linked to that context record");
        }
        ExtractedFactContextLink link = links.save(new ExtractedFactContextLink(workspaceId, factId,
                request.targetType(), request.targetId(), userId, clock.instant()));
        return toResponse(fact, link);
    }

    @Transactional(readOnly = true)
    public List<FactContextLinkResponse> list(UUID workspaceId, UUID documentId, UUID factId) {
        ExtractedFact fact = findFact(workspaceId, documentId, factId);
        return links.findByFactIdAndWorkspaceIdOrderByLinkedAtDesc(factId, workspaceId).stream()
                .map(link -> toResponse(fact, link)).toList();
    }

    private ExtractedFact findFact(UUID workspaceId, UUID documentId, UUID factId) {
        DocumentExtraction extraction = extractions.findByDocumentIdAndWorkspaceId(documentId, workspaceId)
                .orElseThrow(DocumentNotFoundException::new);
        ExtractedFact fact = facts.findByIdAndWorkspaceId(factId, workspaceId)
                .orElseThrow(DocumentNotFoundException::new);
        if (!fact.getExtractionId().equals(extraction.getId())) throw new DocumentNotFoundException();
        return fact;
    }

    private void verifyTarget(UUID workspaceId, FactContextTargetType type, UUID targetId) {
        boolean exists = switch (type) {
            case ENTITY -> entities.findByIdAndWorkspaceId(targetId, workspaceId).isPresent();
            case EVENT -> events.findByIdAndWorkspaceId(targetId, workspaceId).isPresent();
            case COMMITMENT -> commitments.findByIdAndWorkspaceId(targetId, workspaceId).isPresent();
        };
        if (!exists) throw new ContextRecordNotFoundException();
    }

    private FactContextLinkResponse toResponse(ExtractedFact fact, ExtractedFactContextLink link) {
        String evidenceValue = fact.getReviewStatus() == FactReviewStatus.CORRECTED
                ? fact.getCorrectedValue() : fact.getExtractedValue();
        return new FactContextLinkResponse(link.getId(), fact.getId(), fact.getFactType(), evidenceValue,
                fact.getSourcePage(), fact.getSourceExcerpt(), link.getTargetType(), link.getTargetId(),
                link.getLinkedBy(), link.getLinkedAt());
    }

    public record LinkRequest(FactContextTargetType targetType, UUID targetId) { }
    public record FactContextLinkResponse(UUID id, UUID factId, String factType, String value,
                                          int sourcePage, String sourceExcerpt, FactContextTargetType targetType,
                                          UUID targetId, UUID linkedBy, Instant linkedAt) { }
}
