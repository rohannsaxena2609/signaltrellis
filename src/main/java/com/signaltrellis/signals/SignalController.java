package com.signaltrellis.signals;

import com.signaltrellis.identity.AuthenticatedUser;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/signals")
public class SignalController {

    private final SignalService signals;

    public SignalController(SignalService signals) {
        this.signals = signals;
    }

    @PostMapping("/recompute")
    public SignalService.EvaluationResponse recompute(@AuthenticationPrincipal AuthenticatedUser user) {
        return signals.recompute(user);
    }

    @GetMapping
    public Page<SignalService.SignalResponse> search(@AuthenticationPrincipal AuthenticatedUser user,
                                                      @RequestParam(required = false) SignalStatus status,
                                                      @RequestParam(required = false) SignalPriority priority,
                                                      @RequestParam(required = false) SignalType type,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        return signals.search(user.workspaceId(), status, priority, type, page, size);
    }

    @GetMapping("/{id}")
    public SignalService.SignalDetail get(@AuthenticationPrincipal AuthenticatedUser user,
                                           @PathVariable UUID id) {
        return signals.get(user.workspaceId(), id);
    }

    @GetMapping("/{id}/evidence")
    public List<SignalService.EvidenceResponse> evidence(@AuthenticationPrincipal AuthenticatedUser user,
                                                          @PathVariable UUID id) {
        return signals.getEvidence(user.workspaceId(), id);
    }
}
