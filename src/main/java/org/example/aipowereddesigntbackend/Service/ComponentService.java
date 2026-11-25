package org.example.aipowereddesigntbackend.Service;

import org.example.aipowereddesigntbackend.Dto.ComponentRequest;
import org.example.aipowereddesigntbackend.Dto.ComponentResponse;
import org.example.aipowereddesigntbackend.ExceptionHandling.ResourceNotFoundException;
import org.example.aipowereddesigntbackend.Model.Account;
import org.example.aipowereddesigntbackend.Model.Component;
import org.example.aipowereddesigntbackend.Model.Project;
import org.example.aipowereddesigntbackend.Repository.ComponentRepository;
import org.example.aipowereddesigntbackend.Security.OwnershipValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

/**
 * Any project member (OWNER or accepted COLLABORATOR) may manage components -
 * component work is exactly what collaborators are invited to help with.
 */
@Service
public class ComponentService {

    private final ComponentRepository componentRepository;
    private final OwnershipValidator ownershipValidator;

    public ComponentService(ComponentRepository componentRepository, OwnershipValidator ownershipValidator) {
        this.componentRepository = componentRepository;
        this.ownershipValidator = ownershipValidator;
    }

    @Transactional
    public ComponentResponse addComponent(String username, UUID projectId, ComponentRequest request) {
        Account account = ownershipValidator.resolveAuthenticatedAccount(username);
        Project project = ownershipValidator.requireProjectMembership(projectId, account);

        Component component = new Component();
        component.setProject(project);
        component.setName(normalizeName(request.getName()));
        component.setType(request.getType());
        component.setDescription(request.getDescription());

        Component saved = componentRepository.saveAndFlush(component);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ComponentResponse> listComponents(String username, UUID projectId) {
        Account account = ownershipValidator.resolveAuthenticatedAccount(username);
        Project project = ownershipValidator.requireProjectMembership(projectId, account);

        return componentRepository.findByProjectId(project.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ComponentResponse updateComponent(String username, UUID componentId, ComponentRequest request) {
        Account account = ownershipValidator.resolveAuthenticatedAccount(username);
        Component component = findComponentAccessibleBy(componentId, account);

        component.setName(normalizeName(request.getName()));
        component.setType(request.getType());
        component.setDescription(request.getDescription());

        Component saved = componentRepository.saveAndFlush(component);
        return toResponse(saved);
    }

    @Transactional
    public void deleteComponent(String username, UUID componentId) {
        Account account = ownershipValidator.resolveAuthenticatedAccount(username);
        Component component = findComponentAccessibleBy(componentId, account);

        componentRepository.delete(component);
    }

    private String normalizeName(String rawName) {
        String trimmed = rawName == null ? "" : rawName.trim();
        if (!StringUtils.hasText(trimmed)) {
            throw new IllegalArgumentException("Component name must not be blank");
        }
        return trimmed;
    }

    private Component findComponentAccessibleBy(UUID componentId, Account account) {
        Component component = componentRepository.findById(componentId)
                .orElseThrow(() -> new ResourceNotFoundException("Component not found"));

        ownershipValidator.requireMembership(component.getProject(), account);
        return component;
    }

    private ComponentResponse toResponse(Component component) {
        return ComponentResponse.builder()
                .id(component.getId())
                .projectId(component.getProject().getId())
                .name(component.getName())
                .type(component.getType())
                .description(component.getDescription())
                .createdAt(component.getCreatedAt())
                .updatedAt(component.getUpdatedAt())
                .build();
    }
}