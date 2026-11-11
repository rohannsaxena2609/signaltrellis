package com.signaltrellis.capture;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentProcessingJobService {

    private final DocumentProcessingJobRepository jobs;
    private final StoredDocumentRepository documents;
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final int maxAttempts;
    private final int batchSize;
    private final Duration leaseDuration;

    public DocumentProcessingJobService(DocumentProcessingJobRepository jobs, StoredDocumentRepository documents,
                                        JdbcTemplate jdbc,
                                        Clock clock,
                                        @Value("${signaltrellis.processing.max-attempts:5}") int maxAttempts,
                                        @Value("${signaltrellis.processing.batch-size:1}") int batchSize,
                                        @Value("${signaltrellis.processing.lease:PT45M}") Duration leaseDuration) {
        this.jobs = jobs;
        this.documents = documents;
        this.jdbc = jdbc;
        this.clock = clock;
        this.maxAttempts = maxAttempts;
        this.batchSize = batchSize;
        this.leaseDuration = leaseDuration;
        if (maxAttempts < 1 || maxAttempts > 10) {
            throw new IllegalArgumentException("Processing attempts must be between 1 and 10");
        }
        if (batchSize < 1 || batchSize > 20) {
            throw new IllegalArgumentException("Processing batch size must be between 1 and 20");
        }
        if (leaseDuration.isZero() || leaseDuration.isNegative()) {
            throw new IllegalArgumentException("Processing lease duration must be positive");
        }
    }

    @Transactional
    public JobResponse enqueue(UUID workspaceId, UUID documentId) {
        documents.findByIdAndSourceItem_WorkspaceId(documentId, workspaceId)
                .orElseThrow(DocumentNotFoundException::new);
        Instant now = clock.instant();
        jdbc.update("""
                INSERT INTO app.document_processing_jobs AS current_job
                    (id, document_id, status, attempt_count, next_attempt_at, created_at, updated_at)
                VALUES (?, ?, 'QUEUED', 0, ?, ?, ?)
                ON CONFLICT (document_id) DO UPDATE SET
                    status = 'QUEUED', attempt_count = 0, next_attempt_at = EXCLUDED.next_attempt_at,
                    lease_expires_at = NULL, last_error = NULL, updated_at = EXCLUDED.updated_at
                WHERE current_job.status = 'FAILED'
                """, UUID.randomUUID(), documentId, now, now, now);
        jdbc.update("""
                UPDATE app.documents d SET processing_status = CASE job.status
                    WHEN 'QUEUED' THEN 'QUEUED'
                    WHEN 'PROCESSING' THEN 'PROCESSING'
                    WHEN 'PROCESSED' THEN 'PROCESSED'
                    ELSE 'FAILED' END
                FROM app.document_processing_jobs job
                WHERE d.id = job.document_id AND d.id = ?
                """, documentId);
        DocumentProcessingJob job = jobs.findByDocumentId(documentId).orElseThrow(DocumentNotFoundException::new);
        return toResponse(job);
    }

    @Transactional(readOnly = true)
    public JobResponse status(UUID workspaceId, UUID documentId) {
        documents.findByIdAndSourceItem_WorkspaceId(documentId, workspaceId)
                .orElseThrow(DocumentNotFoundException::new);
        return jobs.findByDocumentId(documentId).map(this::toResponse)
                .orElseThrow(DocumentNotFoundException::new);
    }

    @Transactional
    public List<ClaimedJob> claimBatch() {
        Instant now = clock.instant();
        List<DocumentProcessingJob> claimed = jobs.findClaimable(now, DocumentProcessingStatus.QUEUED,
                DocumentProcessingStatus.PROCESSING, PageRequest.of(0, batchSize));
        List<ClaimedJob> ready = new ArrayList<>();
        for (DocumentProcessingJob job : claimed) {
            if (job.getStatus() == DocumentProcessingStatus.PROCESSING && job.getAttemptCount() >= maxAttempts) {
                job.markFailed(now, "Processing failed after retry limit");
                lockDocument(job.getDocumentId()).markProcessingFailed();
                continue;
            }
            job.markProcessing(now, now.plus(leaseDuration));
            ready.add(new ClaimedJob(job.getId(), job.getDocumentId(), job.getAttemptCount(), maxAttempts));
        }
        return List.copyOf(ready);
    }

    @Transactional
    public boolean markDocumentProcessing(ClaimedJob claimedJob) {
        DocumentProcessingJob job = jobs.findForUpdateById(claimedJob.jobId()).orElse(null);
        if (!isCurrentAttempt(job, claimedJob)) return false;
        lockDocument(claimedJob.documentId()).markProcessing();
        return true;
    }

    @Transactional
    public boolean complete(ClaimedJob claimedJob) {
        DocumentProcessingJob job = jobs.findForUpdateById(claimedJob.jobId()).orElse(null);
        if (!isCurrentAttempt(job, claimedJob)) return false;
        StoredDocument document = lockDocument(claimedJob.documentId());
        job.markCompleted(clock.instant());
        document.markProcessed();
        return true;
    }

    @Transactional
    public RetryResult fail(ClaimedJob claimedJob, boolean permanentFailure) {
        DocumentProcessingJob job = jobs.findForUpdateById(claimedJob.jobId()).orElse(null);
        if (!isCurrentAttempt(job, claimedJob)) return RetryResult.STALE;
        StoredDocument document = lockDocument(claimedJob.documentId());
        Instant now = clock.instant();
        if (permanentFailure || job.getAttemptCount() >= maxAttempts) {
            String message = permanentFailure ? "Document could not be processed" : "Processing failed after retry limit";
            job.markFailed(now, message);
            document.markProcessingFailed();
            return RetryResult.FAILED;
        }
        long delaySeconds = Math.min(3600, 5L * (1L << Math.min(job.getAttemptCount(), 9)));
        job.markQueued(now, now.plusSeconds(delaySeconds), "Processing attempt failed; retry scheduled");
        document.queueProcessing();
        return RetryResult.RETRY_SCHEDULED;
    }

    private boolean isCurrentAttempt(DocumentProcessingJob job, ClaimedJob claim) {
        return job != null && job.getStatus() == DocumentProcessingStatus.PROCESSING
                && job.getAttemptCount() == claim.attemptCount();
    }

    private StoredDocument lockDocument(UUID documentId) {
        StoredDocument document = documents.findById(documentId).orElse(null);
        if (document == null) throw new DocumentNotFoundException();
        return documents.findForUpdateByIdAndSourceItem_WorkspaceId(documentId,
                document.getSourceItem().getWorkspaceId()).orElseThrow(DocumentNotFoundException::new);
    }

    private JobResponse toResponse(DocumentProcessingJob job) {
        return new JobResponse(job.getId(), job.getDocumentId(), job.getStatus(), job.getAttemptCount(),
                maxAttempts, job.getNextAttemptAt(), job.getLastError(), job.getCreatedAt(), job.getUpdatedAt());
    }

    public record ClaimedJob(UUID jobId, UUID documentId, int attemptCount, int maxAttempts) { }
    public record JobResponse(UUID jobId, UUID documentId, DocumentProcessingStatus status, int attemptCount,
                              int maxAttempts, Instant nextAttemptAt, String lastError,
                              Instant createdAt, Instant updatedAt) { }
    public enum RetryResult { STALE, RETRY_SCHEDULED, FAILED }
}
