package com.signaltrellis.retrieval;

import com.signaltrellis.identity.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/retrieval")
public class DocumentRetrievalController {

    private final DocumentRetrievalService retrieval;

    public DocumentRetrievalController(DocumentRetrievalService retrieval) {
        this.retrieval = retrieval;
    }

    @PostMapping("/search")
    public DocumentRetrievalService.SearchResponse search(@AuthenticationPrincipal AuthenticatedUser user,
                                                           @Valid @RequestBody SearchRequest request) {
        return retrieval.search(user.workspaceId(), request.query(), request.limit());
    }

    public record SearchRequest(@NotBlank @Size(max = 1000) String query,
                                @Min(1) @Max(20) int limit) { }
}
