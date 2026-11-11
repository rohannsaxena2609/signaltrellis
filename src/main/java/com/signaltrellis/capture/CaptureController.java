package com.signaltrellis.capture;

import com.signaltrellis.identity.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class CaptureController {

    private final DocumentService documents;
    private final DocumentExtractionService extractions;
    private final FactContextLinkService factLinks;

    public CaptureController(DocumentService documents, DocumentExtractionService extractions,
                             FactContextLinkService factLinks) {
        this.documents = documents;
        this.extractions = extractions;
        this.factLinks = factLinks;
    }

    @PostMapping(path = "/capture/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentService.DocumentResponse> upload(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "UNCATEGORIZED") String category) {
        return ResponseEntity.status(201).body(documents.upload(user.workspaceId(), file, category));
    }

    @GetMapping("/documents")
    public Page<DocumentService.DocumentResponse> search(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false, name = "status") String processingStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return documents.search(user.workspaceId(), query, category, processingStatus, page, size);
    }

    @GetMapping("/capture/inbox")
    public Page<DocumentService.DocumentResponse> inbox(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return documents.inbox(user.workspaceId(), page, size);
    }

    @GetMapping("/documents/{id}")
    public DocumentService.DocumentResponse get(@AuthenticationPrincipal AuthenticatedUser user,
                                                @PathVariable UUID id) {
        return documents.get(user.workspaceId(), id);
    }

    @PatchMapping("/documents/{id}/category")
    public DocumentService.DocumentResponse reclassify(@AuthenticationPrincipal AuthenticatedUser user,
                                                        @PathVariable UUID id,
                                                        @Valid @RequestBody CategoryRequest request) {
        return documents.reclassify(user.workspaceId(), id, request.category());
    }

    @GetMapping("/documents/{id}/content")
    public ResponseEntity<Resource> download(@AuthenticationPrincipal AuthenticatedUser user,
                                             @PathVariable UUID id) {
        DocumentService.DocumentDownload download = documents.open(user.workspaceId(), id);
        var metadata = download.metadata();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(metadata.contentType()))
                .contentLength(metadata.sizeBytes())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(metadata.filename(), StandardCharsets.UTF_8).build().toString())
                .body(new InputStreamResource(download.content()));
    }

    @PostMapping("/documents/{id}/extract")
    public DocumentExtractionService.ExtractionResponse extract(@AuthenticationPrincipal AuthenticatedUser user,
                                                                 @PathVariable UUID id) {
        return extractions.extract(user.workspaceId(), id);
    }

    @GetMapping("/documents/{id}/extraction")
    public DocumentExtractionService.ExtractionResponse extraction(@AuthenticationPrincipal AuthenticatedUser user,
                                                                    @PathVariable UUID id) {
        return extractions.get(user.workspaceId(), id);
    }

    @PatchMapping("/documents/{documentId}/extraction/facts/{factId}")
    public DocumentExtractionService.FactResponse reviewFact(@AuthenticationPrincipal AuthenticatedUser user,
                                                              @PathVariable UUID documentId,
                                                              @PathVariable UUID factId,
                                                              @Valid @RequestBody FactReviewRequest request) {
        return extractions.review(user.workspaceId(), documentId, user.id(), factId,
                new DocumentExtractionService.ReviewRequest(request.status(), request.correctedValue()));
    }

    @PostMapping("/documents/{documentId}/extraction/facts/{factId}/context-links")
    public FactContextLinkService.FactContextLinkResponse linkFact(@AuthenticationPrincipal AuthenticatedUser user,
                                                                   @PathVariable UUID documentId,
                                                                   @PathVariable UUID factId,
                                                                   @Valid @RequestBody FactLinkRequest request) {
        return factLinks.link(user.workspaceId(), user.id(), documentId, factId,
                new FactContextLinkService.LinkRequest(request.targetType(), request.targetId()));
    }

    @GetMapping("/documents/{documentId}/extraction/facts/{factId}/context-links")
    public java.util.List<FactContextLinkService.FactContextLinkResponse> factLinks(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID documentId,
            @PathVariable UUID factId) {
        return factLinks.list(user.workspaceId(), documentId, factId);
    }

    public record CategoryRequest(@NotBlank String category) {
    }

    public record FactReviewRequest(@NotNull FactReviewStatus status,
                                    @Size(max = 1000) String correctedValue) { }

    public record FactLinkRequest(@NotNull FactContextTargetType targetType, @NotNull UUID targetId) { }
}
