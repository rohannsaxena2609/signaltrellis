package com.signaltrellis.context;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class CommitmentService {

    private final CommitmentRepository commitments;
    private final LifeEntityRepository entities;

    public CommitmentService(CommitmentRepository commitments, LifeEntityRepository entities) {
        this.commitments = commitments;
        this.entities = entities;
    }

    @Transactional
    public CommitmentResponse create(UUID workspaceId, String title, BigDecimal amount, String currency,
                                     LocalDate dueDate, String notes, UUID entityId) {
        if ((amount == null) != (currency == null)) {
            throw new InvalidContextRequestException("Amount and currency must be supplied together");
        }
        if (entityId != null && entities.findByIdAndWorkspaceId(entityId, workspaceId).isEmpty()) {
            throw new ContextRecordNotFoundException();
        }
        String normalizedCurrency = currency == null ? null : currency.toUpperCase(java.util.Locale.ROOT);
        Commitment commitment = commitments.save(new Commitment(workspaceId, entityId, title.trim(),
                amount, normalizedCurrency, dueDate, cleanOptional(notes)));
        return toResponse(commitment);
    }

    @Transactional(readOnly = true)
    public Page<CommitmentResponse> list(UUID workspaceId, CommitmentStatus status, int page, int size) {
        validatePage(page, size);
        PageRequest request = PageRequest.of(page, size);
        Page<Commitment> results = status == null
                ? commitments.findByWorkspaceIdOrderByDueDateAscIdAsc(workspaceId, request)
                : commitments.findByWorkspaceIdAndStatusOrderByDueDateAscIdAsc(workspaceId, status, request);
        return results.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public CommitmentSummary summary(UUID workspaceId) {
        long open = commitments.countByWorkspaceIdAndStatus(workspaceId, CommitmentStatus.OPEN);
        long overdue = commitments.countByWorkspaceIdAndStatusAndDueDateBefore(
                workspaceId, CommitmentStatus.OPEN, LocalDate.now());
        List<AmountTotal> totals = commitments.sumAmountsByCurrency(workspaceId, CommitmentStatus.OPEN)
                .stream().map(row -> new AmountTotal(row.getCurrency(), row.getTotalAmount())).toList();
        return new CommitmentSummary(open, overdue, totals);
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new InvalidContextRequestException("Page must be non-negative and page size must be between 1 and 100");
        }
    }

    private String cleanOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private CommitmentResponse toResponse(Commitment commitment) {
        return new CommitmentResponse(commitment.getId(), commitment.getEntityId(), commitment.getTitle(),
                commitment.getAmount(), commitment.getCurrency(), commitment.getDueDate(),
                commitment.getStatus(), commitment.getNotes(), commitment.getCreatedAt());
    }

    public record CommitmentResponse(UUID id, UUID entityId, String title, BigDecimal amount,
                                     String currency, LocalDate dueDate, CommitmentStatus status,
                                     String notes, java.time.Instant createdAt) { }

    public record AmountTotal(String currency, BigDecimal totalAmount) { }

    public record CommitmentSummary(long openCount, long overdueCount, List<AmountTotal> openAmountByCurrency) { }
}
