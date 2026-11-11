package com.signaltrellis.capture;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentProcessingJobRepository extends JpaRepository<DocumentProcessingJob, UUID> {

    Optional<DocumentProcessingJob> findByDocumentId(UUID documentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DocumentProcessingJob> findForUpdateById(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select job from DocumentProcessingJob job
            where (job.status = :queued and job.nextAttemptAt <= :now)
               or (job.status = :processing and job.leaseExpiresAt <= :now)
            order by job.nextAttemptAt asc, job.createdAt asc
            """)
    List<DocumentProcessingJob> findClaimable(@Param("now") Instant now,
                                              @Param("queued") DocumentProcessingStatus queued,
                                              @Param("processing") DocumentProcessingStatus processing,
                                              Pageable pageable);
}
