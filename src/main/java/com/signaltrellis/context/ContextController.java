package com.signaltrellis.context;

import com.signaltrellis.identity.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/context")
public class ContextController {

    private final ContextService context;

    public ContextController(ContextService context) {
        this.context = context;
    }

    @PostMapping("/entities")
    @ResponseStatus(HttpStatus.CREATED)
    public ContextService.EntityResponse createEntity(@AuthenticationPrincipal AuthenticatedUser user,
                                                       @Valid @RequestBody CreateEntityRequest request) {
        return context.createEntity(user.workspaceId(), request.type(), request.name(), request.description());
    }

    @GetMapping("/entities")
    public Page<ContextService.EntityResponse> searchEntities(@AuthenticationPrincipal AuthenticatedUser user,
                                                                @RequestParam(required = false) LifeEntityType type,
                                                                @RequestParam(required = false) String query,
                                                                @RequestParam(defaultValue = "0") int page,
                                                                @RequestParam(defaultValue = "20") int size) {
        return context.searchEntities(user.workspaceId(), type, query, page, size);
    }

    @GetMapping("/entities/{id}")
    public ContextService.EntityResponse getEntity(@AuthenticationPrincipal AuthenticatedUser user,
                                                    @PathVariable UUID id) {
        return context.getEntity(user.workspaceId(), id);
    }

    @GetMapping("/entities/{id}/connections")
    public ContextService.ContextGraphResponse connections(@AuthenticationPrincipal AuthenticatedUser user,
                                                            @PathVariable UUID id,
                                                            @RequestParam(defaultValue = "1") int depth,
                                                            @RequestParam(defaultValue = "50") int limit) {
        return context.connections(user.workspaceId(), id, depth, limit);
    }

    @PostMapping("/relationships")
    @ResponseStatus(HttpStatus.CREATED)
    public ContextService.RelationshipResponse createRelationship(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CreateRelationshipRequest request) {
        return context.createRelationship(user.workspaceId(), request.sourceEntityId(),
                request.targetEntityId(), request.type());
    }

    @DeleteMapping("/relationships/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRelationship(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        context.deleteRelationship(user.workspaceId(), id);
    }

    public record CreateEntityRequest(@NotNull LifeEntityType type,
                                      @NotBlank @Size(max = 160) String name,
                                      @Size(max = 2000) String description) { }

    public record CreateRelationshipRequest(@NotNull UUID sourceEntityId,
                                            @NotNull UUID targetEntityId,
                                            @NotNull RelationshipType type) { }
}
