package com.signaltrellis.assistant;

import com.signaltrellis.identity.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/assistant")
public class AssistantController {

    private final AssistantService assistant;

    public AssistantController(AssistantService assistant) {
        this.assistant = assistant;
    }

    @PostMapping("/chat")
    public AssistantService.AssistantResponse chat(@AuthenticationPrincipal AuthenticatedUser user,
                                                    @Valid @RequestBody ChatRequest request) {
        return assistant.ask(user, request.message());
    }

    public record ChatRequest(@NotBlank @Size(max = 4000) String message) { }
}
