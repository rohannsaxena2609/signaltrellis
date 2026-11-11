package com.signaltrellis.capture;

import com.signaltrellis.retrieval.DocumentIndexService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DocumentProcessingWorker {

    private static final Logger log = LoggerFactory.getLogger(DocumentProcessingWorker.class);

    private final DocumentProcessingJobService jobs;
    private final StoredDocumentRepository documents;
    private final DocumentExtractionService extractions;
    private final DocumentIndexService indexing;
    private final ObjectProvider<MeterRegistry> meterRegistries;

    public DocumentProcessingWorker(DocumentProcessingJobService jobs, StoredDocumentRepository documents,
                                    DocumentExtractionService extractions, DocumentIndexService indexing,
                                    ObjectProvider<MeterRegistry> meterRegistries) {
        this.jobs = jobs;
        this.documents = documents;
        this.extractions = extractions;
        this.indexing = indexing;
        this.meterRegistries = meterRegistries;
    }

    @Scheduled(fixedDelayString = "${signaltrellis.processing.poll-interval:PT2S}")
    public void drain() {
        List<DocumentProcessingJobService.ClaimedJob> claimed = jobs.claimBatch();
        for (DocumentProcessingJobService.ClaimedJob job : claimed) {
            process(job);
        }
    }

    private void process(DocumentProcessingJobService.ClaimedJob job) {
        try {
            if (job.attemptCount() > job.maxAttempts()) {
                record("failed");
                jobs.fail(job, true);
                return;
            }
            if (!jobs.markDocumentProcessing(job)) return;
            StoredDocument document = documents.findById(job.documentId()).orElseThrow(DocumentNotFoundException::new);
            var workspaceId = document.getSourceItem().getWorkspaceId();
            extractions.extractLocally(workspaceId, job.documentId());
            indexing.indexLocally(workspaceId, job.documentId());
            if (jobs.complete(job)) record("completed");
        } catch (InvalidDocumentException exception) {
            DocumentProcessingJobService.RetryResult result = jobs.fail(job, true);
            if (result == DocumentProcessingJobService.RetryResult.FAILED) record("failed");
            log.warn("Document processing job {} permanently failed ({})", job.jobId(), exception.getClass().getSimpleName());
        } catch (RuntimeException exception) {
            DocumentProcessingJobService.RetryResult result = jobs.fail(job, false);
            if (result == DocumentProcessingJobService.RetryResult.FAILED) record("failed");
            if (result == DocumentProcessingJobService.RetryResult.RETRY_SCHEDULED) record("retry");
            log.warn("Document processing job {} failed with {} ({})", job.jobId(),
                    result.name(), exception.getClass().getSimpleName());
        }
    }

    private void record(String outcome) {
        MeterRegistry registry = meterRegistries.getIfAvailable();
        if (registry != null) registry.counter("signaltrellis.document.processing", "outcome", outcome).increment();
    }
}
