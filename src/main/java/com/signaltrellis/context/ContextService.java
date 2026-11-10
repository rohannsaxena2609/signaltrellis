package com.signaltrellis.context;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ContextService {

    private final LifeEntityRepository entities;
    private final EntityRelationshipRepository relationships;

    public ContextService(LifeEntityRepository entities, EntityRelationshipRepository relationships) {
        this.entities = entities;
        this.relationships = relationships;
    }

    @Transactional
    public EntityResponse createEntity(UUID workspaceId, LifeEntityType type, String name, String description) {
        String normalizedName = name.trim();
        if (entities.existsByWorkspaceIdAndEntityTypeAndNameIgnoreCase(workspaceId, type, normalizedName)) {
            throw new DuplicateContextRecordException("An entity with this type and name already exists");
        }
        try {
            return toResponse(entities.saveAndFlush(
                    new LifeEntity(workspaceId, type, normalizedName, cleanOptional(description))));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateContextRecordException("An entity with this type and name already exists");
        }
    }

    @Transactional(readOnly = true)
    public Page<EntityResponse> searchEntities(UUID workspaceId, LifeEntityType type, String query,
                                                int page, int size) {
        validatePage(page, size);
        String normalizedQuery = cleanOptional(query);
        if (normalizedQuery != null && normalizedQuery.length() > 200) {
            throw new InvalidContextRequestException("Search text must be at most 200 characters");
        }
        return entities.search(workspaceId, type, normalizedQuery,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name", "id")))
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public EntityResponse getEntity(UUID workspaceId, UUID entityId) {
        return toResponse(findEntity(workspaceId, entityId));
    }

    @Transactional
    public RelationshipResponse createRelationship(UUID workspaceId, UUID sourceId, UUID targetId,
                                                   RelationshipType type) {
        if (sourceId.equals(targetId)) {
            throw new InvalidContextRequestException("An entity cannot be related to itself");
        }
        LifeEntity source = findEntity(workspaceId, sourceId);
        LifeEntity target = findEntity(workspaceId, targetId);
        if (relationships.existsByWorkspaceIdAndSourceEntity_IdAndTargetEntity_IdAndRelationshipType(
                workspaceId, sourceId, targetId, type)) {
            throw new DuplicateContextRecordException("This relationship already exists");
        }
        try {
            return toResponse(relationships.saveAndFlush(new EntityRelationship(workspaceId, source, target, type)));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateContextRecordException("This relationship conflicts with an existing relationship");
        }
    }

    @Transactional
    public void deleteRelationship(UUID workspaceId, UUID relationshipId) {
        EntityRelationship relationship = relationships.findByIdAndWorkspaceId(relationshipId, workspaceId)
                .orElseThrow(ContextRecordNotFoundException::new);
        relationships.delete(relationship);
    }

    @Transactional(readOnly = true)
    public ContextGraphResponse connections(UUID workspaceId, UUID entityId, int depth, int nodeLimit) {
        if (depth < 1 || depth > 3) {
            throw new InvalidContextRequestException("Graph depth must be between 1 and 3");
        }
        if (nodeLimit < 2 || nodeLimit > 200) {
            throw new InvalidContextRequestException("Graph node limit must be between 2 and 200");
        }

        LifeEntity seed = findEntity(workspaceId, entityId);
        Map<UUID, EntityResponse> nodes = new LinkedHashMap<>();
        Map<UUID, RelationshipResponse> edges = new LinkedHashMap<>();
        Set<UUID> visited = new LinkedHashSet<>();
        Set<UUID> frontier = new LinkedHashSet<>();
        nodes.put(seed.getId(), toResponse(seed));
        visited.add(seed.getId());
        frontier.add(seed.getId());
        boolean truncated = false;

        for (int level = 0; level < depth && !frontier.isEmpty(); level++) {
            int edgeLimit = Math.min(500, nodeLimit * 4);
            Page<EntityRelationship> result = relationships.findTouching(workspaceId, frontier,
                    PageRequest.of(0, edgeLimit, Sort.by(Sort.Direction.ASC, "createdAt")));
            truncated |= result.hasNext();
            Set<UUID> next = new LinkedHashSet<>();

            for (EntityRelationship edge : result.getContent()) {
                LifeEntity source = edge.getSourceEntity();
                LifeEntity target = edge.getTargetEntity();
                int newNodes = (nodes.containsKey(source.getId()) ? 0 : 1)
                        + (nodes.containsKey(target.getId()) ? 0 : 1);
                if (nodes.size() + newNodes > nodeLimit) {
                    truncated = true;
                    continue;
                }
                nodes.putIfAbsent(source.getId(), toResponse(source));
                nodes.putIfAbsent(target.getId(), toResponse(target));
                edges.putIfAbsent(edge.getId(), toResponse(edge));
                if (visited.add(source.getId())) next.add(source.getId());
                if (visited.add(target.getId())) next.add(target.getId());
            }
            frontier = next;
            if (nodes.size() >= nodeLimit && !frontier.isEmpty()) {
                truncated = true;
                break;
            }
        }
        return new ContextGraphResponse(entityId, depth, truncated,
                new ArrayList<>(nodes.values()), new ArrayList<>(edges.values()));
    }

    private LifeEntity findEntity(UUID workspaceId, UUID entityId) {
        return entities.findByIdAndWorkspaceId(entityId, workspaceId)
                .orElseThrow(ContextRecordNotFoundException::new);
    }

    private String cleanOptional(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new InvalidContextRequestException("Page must be non-negative and page size must be between 1 and 100");
        }
    }

    private EntityResponse toResponse(LifeEntity entity) {
        return new EntityResponse(entity.getId(), entity.getEntityType(), entity.getName(),
                entity.getDescription(), entity.getCreatedAt());
    }

    private RelationshipResponse toResponse(EntityRelationship relationship) {
        return new RelationshipResponse(relationship.getId(), relationship.getSourceEntity().getId(),
                relationship.getSourceEntity().getName(), relationship.getRelationshipType(),
                relationship.getTargetEntity().getId(), relationship.getTargetEntity().getName(),
                relationship.getCreatedAt());
    }

    public record EntityResponse(UUID id, LifeEntityType type, String name, String description, Instant createdAt) { }

    public record RelationshipResponse(UUID id, UUID sourceEntityId, String sourceName,
                                       RelationshipType type, UUID targetEntityId, String targetName,
                                       Instant createdAt) { }

    public record ContextGraphResponse(UUID rootEntityId, int depth, boolean truncated,
                                       Collection<EntityResponse> nodes,
                                       Collection<RelationshipResponse> relationships) { }
}
