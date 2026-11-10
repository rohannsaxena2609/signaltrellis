package com.signaltrellis.context;

import com.signaltrellis.identity.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/commitments")
public class CommitmentController {

    private final CommitmentService commitments;

    public CommitmentController(CommitmentService commitments) {
        this.commitments = commitments;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommitmentService.CommitmentResponse create(@AuthenticationPrincipal AuthenticatedUser user,
                                                        @Valid @RequestBody CreateCommitmentRequest request) {
        return commitments.create(user.workspaceId(), request.title(), request.amount(), request.currency(),
                request.dueDate(), request.notes(), request.entityId());
    }

    @GetMapping
    public Page<CommitmentService.CommitmentResponse> list(@AuthenticationPrincipal AuthenticatedUser user,
                                                            @RequestParam(required = false) CommitmentStatus status,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "20") int size) {
        return commitments.list(user.workspaceId(), status, page, size);
    }

    @GetMapping("/summary")
    public CommitmentService.CommitmentSummary summary(@AuthenticationPrincipal AuthenticatedUser user) {
        return commitments.summary(user.workspaceId());
    }

    public record CreateCommitmentRequest(@NotBlank @Size(max = 160) String title,
                                          @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal amount,
                                          @Pattern(regexp = "[A-Za-z]{3}") String currency,
                                          @NotNull LocalDate dueDate,
                                          @Size(max = 2000) String notes,
                                          UUID entityId) { }
}
