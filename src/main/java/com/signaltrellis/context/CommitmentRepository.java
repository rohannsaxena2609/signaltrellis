package com.signaltrellis.context;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface CommitmentRepository extends JpaRepository<Commitment, UUID> {

    Page<Commitment> findByWorkspaceIdOrderByDueDateAscIdAsc(UUID workspaceId, Pageable pageable);

    Page<Commitment> findByWorkspaceIdAndStatusOrderByDueDateAscIdAsc(UUID workspaceId,
                                                                 CommitmentStatus status, Pageable pageable);

    long countByWorkspaceIdAndStatus(UUID workspaceId, CommitmentStatus status);

    long countByWorkspaceIdAndStatusAndDueDateBefore(UUID workspaceId, CommitmentStatus status, LocalDate date);

    @Query("""
            select c.currency as currency, sum(c.amount) as totalAmount
            from Commitment c
            where c.workspaceId = :workspaceId and c.status = :status and c.amount is not null
            group by c.currency
            order by c.currency
            """)
    List<CurrencyTotal> sumAmountsByCurrency(@Param("workspaceId") UUID workspaceId,
                                             @Param("status") CommitmentStatus status);

    interface CurrencyTotal {
        String getCurrency();
        BigDecimal getTotalAmount();
    }
}
