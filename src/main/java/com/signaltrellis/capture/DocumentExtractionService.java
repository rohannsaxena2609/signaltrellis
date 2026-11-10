package com.signaltrellis.capture;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DocumentExtractionService {

    private static final String EXTRACTOR = "PDFBOX_TEXT_3.0";
    private static final int MAX_PAGES = 500;
    private static final int MAX_EXTRACTED_CHARACTERS = 1_000_000;
    private static final Pattern DATE = Pattern.compile("\\b(?:\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}|\\d{4}-\\d{2}-\\d{2})\\b");
    private static final Pattern AMOUNT = Pattern.compile("(?i)(?:₹|\\bINR\\b|\\bRs\\.?\\s*|\\$|\\bUSD\\b)\\s*[0-9][0-9,]*(?:\\.[0-9]{1,2})?");

    private final StoredDocumentRepository documents;
    private final DocumentExtractionRepository extractions;
    private final DocumentTextPageRepository pages;
    private final ExtractedFactRepository facts;
    private final DocumentFileStorage storage;
    private final Clock clock;

    public DocumentExtractionService(StoredDocumentRepository documents,
                                     DocumentExtractionRepository extractions,
                                     DocumentTextPageRepository pages,
                                     ExtractedFactRepository facts,
                                     DocumentFileStorage storage, Clock clock) {
        this.documents = documents;
        this.extractions = extractions;
        this.pages = pages;
        this.facts = facts;
        this.storage = storage;
        this.clock = clock;
    }

    @Transactional
    public ExtractionResponse extract(UUID workspaceId, UUID documentId) {
        StoredDocument storedDocument = documents.findForUpdateByIdAndSourceItem_WorkspaceId(documentId, workspaceId)
                .orElseThrow(DocumentNotFoundException::new);
        if (!"application/pdf".equals(storedDocument.getDetectedContentType())) {
            throw new InvalidDocumentException("Local text extraction currently supports PDF documents only");
        }

        List<PageText> extractedPages = readPages(storedDocument.getStorageKey());
        Instant now = clock.instant();
        DocumentExtraction extraction = extractions.findByDocumentIdAndWorkspaceId(documentId, workspaceId)
                .orElse(null);
        if (extraction == null) {
            extraction = extractions.save(new DocumentExtraction(documentId, workspaceId, EXTRACTOR, now));
        } else {
            facts.deleteByExtractionId(extraction.getId());
            pages.deleteByExtractionId(extraction.getId());
            extraction.reprocess(EXTRACTOR, now);
        }

        int totalCharacters = 0;
        List<DocumentTextPage> savedPages = new ArrayList<>();
        List<ExtractedFact> savedFacts = new ArrayList<>();
        for (PageText pageText : extractedPages) {
            totalCharacters += pageText.text().length();
            if (totalCharacters > MAX_EXTRACTED_CHARACTERS) {
                throw new InvalidDocumentException("Extracted PDF text exceeds the 1 MB review limit");
            }
            DocumentTextPage page = new DocumentTextPage(extraction.getId(), workspaceId,
                    pageText.pageNumber(), pageText.text(), now);
            savedPages.add(page);
            if (savedFacts.size() < 500) {
                savedFacts.addAll(findCandidates(extraction.getId(), workspaceId, pageText, now,
                        500 - savedFacts.size()));
            }
        }
        extraction.complete(totalCharacters, now);
        extractions.save(extraction);
        pages.saveAll(savedPages);
        facts.saveAll(savedFacts);
        return toResponse(extraction, savedPages, savedFacts);
    }

    @Transactional(readOnly = true)
    public ExtractionResponse get(UUID workspaceId, UUID documentId) {
        DocumentExtraction extraction = extractions.findByDocumentIdAndWorkspaceId(documentId, workspaceId)
                .orElseThrow(DocumentNotFoundException::new);
        return toResponse(extraction, pages.findByExtractionIdOrderByPageNumber(extraction.getId()),
                facts.findByExtractionIdAndWorkspaceIdOrderBySourcePageAscIdAsc(extraction.getId(), workspaceId));
    }

    @Transactional
    public FactResponse review(UUID workspaceId, UUID documentId, UUID userId, UUID factId, ReviewRequest request) {
        ExtractedFact fact = facts.findByIdAndWorkspaceId(factId, workspaceId)
                .orElseThrow(DocumentNotFoundException::new);
        DocumentExtraction extraction = extractions.findByDocumentIdAndWorkspaceId(documentId, workspaceId)
                .orElseThrow(DocumentNotFoundException::new);
        if (!extraction.getId().equals(fact.getExtractionId())) {
            throw new DocumentNotFoundException();
        }
        FactReviewStatus status = request.status();
        String correction = request.correctedValue() == null || request.correctedValue().isBlank()
                ? null : request.correctedValue().trim();
        if (status == FactReviewStatus.CORRECTED && correction == null) {
            throw new InvalidDocumentException("A corrected value is required when marking a candidate corrected");
        }
        if (status != FactReviewStatus.CORRECTED && correction != null) {
            throw new InvalidDocumentException("A corrected value is only allowed for corrected candidates");
        }
        if (correction != null && correction.length() > 1000) {
            throw new InvalidDocumentException("Corrected value must be at most 1000 characters");
        }
        fact.review(status, correction, userId, clock.instant());
        return toResponse(facts.save(fact));
    }

    private List<PageText> readPages(UUID storageKey) {
        try (InputStream input = storage.open(storageKey)) {
            byte[] bytes = input.readNBytes((int) DocumentService.MAX_FILE_BYTES + 1);
            try (PDDocument document = Loader.loadPDF(bytes)) {
                if (document.getNumberOfPages() > MAX_PAGES) {
                    throw new InvalidDocumentException("PDF exceeds the 500 page extraction limit");
                }
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setStartPage(1);
                stripper.setEndPage(document.getNumberOfPages());
                String allText = stripper.getText(document);
                String[] pageContents = allText.split("\\f", -1);
                List<PageText> result = new ArrayList<>();
                int characters = 0;
                for (int page = 1; page <= document.getNumberOfPages(); page++) {
                    String text = (page <= pageContents.length ? pageContents[page - 1] : "").trim();
                    characters += text.length();
                    if (characters > MAX_EXTRACTED_CHARACTERS) {
                        throw new InvalidDocumentException("Extracted PDF text exceeds the 1 MB review limit");
                    }
                    result.add(new PageText(page, text));
                }
                return result;
            }
        } catch (InvalidDocumentException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new InvalidDocumentException("PDF text extraction failed; the file may be damaged or encrypted");
        }
    }

    private List<ExtractedFact> findCandidates(UUID extractionId, UUID workspaceId, PageText page, Instant now,
                                               int limit) {
        List<ExtractedFact> result = new ArrayList<>();
        addMatches(result, extractionId, workspaceId, page, DATE, "DATE_CANDIDATE", now, limit);
        addMatches(result, extractionId, workspaceId, page, AMOUNT, "AMOUNT_CANDIDATE", now, limit);
        return result;
    }

    private void addMatches(List<ExtractedFact> output, UUID extractionId, UUID workspaceId,
                            PageText page, Pattern pattern, String type, Instant now, int limit) {
        Matcher matcher = pattern.matcher(page.text());
        while (matcher.find() && output.size() < limit) {
            int start = Math.max(0, matcher.start() - 100);
            int end = Math.min(page.text().length(), matcher.end() + 100);
            String excerpt = page.text().substring(start, end).replaceAll("\\s+", " ").trim();
            output.add(new ExtractedFact(extractionId, workspaceId, type, matcher.group(),
                    page.pageNumber(), excerpt, now));
        }
    }

    private ExtractionResponse toResponse(DocumentExtraction extraction, List<DocumentTextPage> pages,
                                          List<ExtractedFact> facts) {
        return new ExtractionResponse(extraction.getId(), extraction.getDocumentId(), extraction.getStatus(),
                extraction.getExtractor(), extraction.getExtractedCharacterCount(), extraction.getStartedAt(),
                extraction.getCompletedAt(), pages.stream().map(this::toResponse).toList(),
                facts.stream().map(this::toResponse).toList());
    }

    private PageResponse toResponse(DocumentTextPage page) {
        return new PageResponse(page.getPageNumber(), page.getContent(), page.getCharacterCount());
    }

    private FactResponse toResponse(ExtractedFact fact) {
        return new FactResponse(fact.getId(), fact.getFactType(), fact.getExtractedValue(), fact.getSourcePage(),
                fact.getSourceExcerpt(), fact.getConfidence(), fact.getReviewStatus(), fact.getCorrectedValue(),
                fact.getReviewedBy(), fact.getReviewedAt());
    }

    private record PageText(int pageNumber, String text) { }

    public record PageResponse(int pageNumber, String text, int characterCount) { }
    public record FactResponse(UUID id, String type, String extractedValue, int sourcePage, String sourceExcerpt,
                               java.math.BigDecimal confidence, FactReviewStatus reviewStatus,
                               String correctedValue, UUID reviewedBy, Instant reviewedAt) { }
    public record ExtractionResponse(UUID id, UUID documentId, ExtractionStatus status, String extractor,
                                     int extractedCharacterCount, Instant startedAt, Instant completedAt,
                                     List<PageResponse> pages, List<FactResponse> candidates) { }
    public record ReviewRequest(FactReviewStatus status, String correctedValue) { }
}
