package com.signaltrellis.capture;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DocumentExtractionService {

    private static final int MAX_PAGES = 500;
    private static final int MAX_OCR_PAGES = 25;
    private static final int MAX_EXTRACTED_CHARACTERS = 1_000_000;
    private static final long MAX_IMAGE_PIXELS = 40_000_000L;
    private static final Pattern DATE = Pattern.compile("\\b(?:\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}|\\d{4}-\\d{2}-\\d{2})\\b");
    private static final Pattern AMOUNT = Pattern.compile("(?i)(?:₹|\\bINR\\b|\\bRs\\.?\\s*|\\$|\\bUSD\\b)\\s*[0-9][0-9,]*(?:\\.[0-9]{1,2})?");

    private final StoredDocumentRepository documents;
    private final DocumentExtractionRepository extractions;
    private final DocumentTextPageRepository pages;
    private final ExtractedFactRepository facts;
    private final DocumentFileStorage storage;
    private final Clock clock;
    private final OcrEngine ocr;

    public DocumentExtractionService(StoredDocumentRepository documents,
                                     DocumentExtractionRepository extractions,
                                     DocumentTextPageRepository pages,
                                     ExtractedFactRepository facts,
                                     DocumentFileStorage storage, Clock clock, OcrEngine ocr) {
        this.documents = documents;
        this.extractions = extractions;
        this.pages = pages;
        this.facts = facts;
        this.storage = storage;
        this.clock = clock;
        this.ocr = ocr;
    }

    @Transactional
    public ExtractionResponse extract(UUID workspaceId, UUID documentId) {
        StoredDocument storedDocument = documents.findForUpdateByIdAndSourceItem_WorkspaceId(documentId, workspaceId)
                .orElseThrow(DocumentNotFoundException::new);
        List<PageText> extractedPages = readPages(storedDocument.getStorageKey(),
                storedDocument.getDetectedContentType());
        Instant now = clock.instant();
        String extractor = extractedPages.stream().anyMatch(page -> page.method().equals("TESSERACT"))
                ? "PDFBOX_TESSERACT_LOCAL" : "PDFBOX_TEXT_3.0";
        DocumentExtraction extraction = extractions.findByDocumentIdAndWorkspaceId(documentId, workspaceId)
                .orElse(null);
        if (extraction == null) {
            extraction = extractions.save(new DocumentExtraction(documentId, workspaceId, extractor, now));
        } else {
            facts.deleteByExtractionId(extraction.getId());
            pages.deleteByExtractionId(extraction.getId());
            extraction.reprocess(extractor, now);
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
                    pageText.pageNumber(), pageText.text(), pageText.method(), now);
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

    private List<PageText> readPages(UUID storageKey, String contentType) {
        try (InputStream input = storage.open(storageKey)) {
            byte[] bytes = input.readNBytes((int) DocumentService.MAX_FILE_BYTES + 1);
            if ("image/png".equals(contentType) || "image/jpeg".equals(contentType)) {
                validateImageSize(bytes);
                return List.of(new PageText(1, runOcr(bytes), "TESSERACT"));
            }
            if (!"application/pdf".equals(contentType)) {
                throw new InvalidDocumentException("This file type does not support local text extraction");
            }
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
                int ocrPages = 0;
                PDFRenderer renderer = new PDFRenderer(document);
                for (int page = 1; page <= document.getNumberOfPages(); page++) {
                    String text = (page <= pageContents.length ? pageContents[page - 1] : "").trim();
                    String method = "PDFBOX_TEXT";
                    if (text.length() < 30) {
                        if (++ocrPages > MAX_OCR_PAGES) {
                            throw new InvalidDocumentException("PDF exceeds the 25 page OCR limit");
                        }
                        var mediaBox = document.getPage(page - 1).getMediaBox();
                        double pixelCount = mediaBox.getWidth() * 150d / 72d
                                * mediaBox.getHeight() * 150d / 72d;
                        if (pixelCount > MAX_IMAGE_PIXELS) {
                            throw new InvalidDocumentException("A PDF page exceeds the local OCR image size limit");
                        }
                        var image = renderer.renderImageWithDPI(page - 1, 150);
                        ByteArrayOutputStream imageBytes = new ByteArrayOutputStream();
                        ImageIO.write(image, "png", imageBytes);
                        text = runOcr(imageBytes.toByteArray()).trim();
                        method = "TESSERACT";
                    }
                    characters += text.length();
                    if (characters > MAX_EXTRACTED_CHARACTERS) {
                        throw new InvalidDocumentException("Extracted PDF text exceeds the 1 MB review limit");
                    }
                    result.add(new PageText(page, text, method));
                }
                return result;
            }
        } catch (InvalidDocumentException exception) {
            throw exception;
        } catch (OcrUnavailableException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new InvalidDocumentException("Document text extraction failed; the file may be damaged or encrypted");
        }
    }

    private String runOcr(byte[] imageBytes) {
        try {
            return ocr.extractText(imageBytes);
        } catch (OcrUnavailableException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new InvalidDocumentException("OCR could not read the document image");
        }
    }

    private void validateImageSize(byte[] imageBytes) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new java.io.ByteArrayInputStream(imageBytes))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new InvalidDocumentException("Unable to read uploaded image");
            ImageReader reader = readers.next();
            try {
                reader.setInput(input);
                long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
                if (pixels <= 0 || pixels > MAX_IMAGE_PIXELS) {
                    throw new InvalidDocumentException("Image exceeds the 40 megapixel OCR limit");
                }
            } finally {
                reader.dispose();
            }
        } catch (IOException exception) {
            throw new InvalidDocumentException("Unable to inspect uploaded image");
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
        return new PageResponse(page.getPageNumber(), page.getContent(), page.getCharacterCount(),
                page.getExtractionMethod());
    }

    private FactResponse toResponse(ExtractedFact fact) {
        return new FactResponse(fact.getId(), fact.getFactType(), fact.getExtractedValue(), fact.getSourcePage(),
                fact.getSourceExcerpt(), fact.getConfidence(), fact.getReviewStatus(), fact.getCorrectedValue(),
                fact.getReviewedBy(), fact.getReviewedAt());
    }

    private record PageText(int pageNumber, String text, String method) { }

    public record PageResponse(int pageNumber, String text, int characterCount, String extractionMethod) { }
    public record FactResponse(UUID id, String type, String extractedValue, int sourcePage, String sourceExcerpt,
                               java.math.BigDecimal confidence, FactReviewStatus reviewStatus,
                               String correctedValue, UUID reviewedBy, Instant reviewedAt) { }
    public record ExtractionResponse(UUID id, UUID documentId, ExtractionStatus status, String extractor,
                                     int extractedCharacterCount, Instant startedAt, Instant completedAt,
                                     List<PageResponse> pages, List<FactResponse> candidates) { }
    public record ReviewRequest(FactReviewStatus status, String correctedValue) { }
}
