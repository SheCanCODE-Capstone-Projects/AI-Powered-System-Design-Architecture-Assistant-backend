package org.example.aipowereddesigntbackend.Service;

import org.example.aipowereddesigntbackend.Dto.ComponentResponse;
import org.example.aipowereddesigntbackend.Dto.CreateProjectRequest;
import org.example.aipowereddesigntbackend.Dto.ProjectDetailResponse;
import org.example.aipowereddesigntbackend.Dto.ProjectResponse;
import org.example.aipowereddesigntbackend.Enum.Role;
import org.example.aipowereddesigntbackend.Model.Account;
import org.example.aipowereddesigntbackend.Model.Component;
import org.example.aipowereddesigntbackend.Model.Project;
import org.example.aipowereddesigntbackend.Model.ProjectMember;
import org.example.aipowereddesigntbackend.Repository.ComponentRepository;
import org.example.aipowereddesigntbackend.Repository.ProjectInvitationRepository;
import org.example.aipowereddesigntbackend.Repository.ProjectMemberRepository;
import org.example.aipowereddesigntbackend.Repository.ProjectRepository;
import org.example.aipowereddesigntbackend.Security.OwnershipValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectInvitationRepository projectInvitationRepository;
    private final ComponentRepository componentRepository;
    private final OwnershipValidator ownershipValidator;

    public ProjectService(
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository,
            ProjectInvitationRepository projectInvitationRepository,
            ComponentRepository componentRepository,
            OwnershipValidator ownershipValidator
    ) {
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.projectInvitationRepository = projectInvitationRepository;
        this.componentRepository = componentRepository;
        this.ownershipValidator = ownershipValidator;
    }

    @Transactional
    public ProjectResponse createProject(String authenticatedUsername, CreateProjectRequest request) {
        Account owner = ownershipValidator.resolveAuthenticatedAccount(authenticatedUsername);

        Project project = new Project();
        project.setName(normalizeName(request.getName()));
        project.setDescription(request.getDescription());
        project.setRequirements(request.getRequirements());
        project.setOwner(owner);

        Project savedProject = projectRepository.saveAndFlush(project);

        ProjectMember ownerMembership = new ProjectMember();
        ownerMembership.setProject(savedProject);
        ownerMembership.setAccount(owner);
        ownerMembership.setRole(Role.OWNER);
        projectMemberRepository.save(ownerMembership);

        return toResponse(savedProject, Role.OWNER);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> listMyProjects(String authenticatedUsername) {
        Account account = ownershipValidator.resolveAuthenticatedAccount(authenticatedUsername);

        return projectMemberRepository.findByAccountId(account.getId())
                .stream()
                .map(membership -> toResponse(membership.getProject(), membership.getRole()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectDetailResponse getProject(String authenticatedUsername, UUID projectId) {
        Account account = ownershipValidator.resolveAuthenticatedAccount(authenticatedUsername);
        Project project = ownershipValidator.requireProjectMembership(projectId, account);

        List<ComponentResponse> components = componentRepository.findByProjectId(project.getId())
                .stream()
                .map(this::toComponentResponse)
                .toList();

        return ProjectDetailResponse.builder()
                .project(toResponse(project, resolveCallersRole(project, account)))
                .components(components)
                .build();
    }

    @Transactional
    public ProjectResponse updateProject(String authenticatedUsername, UUID projectId, CreateProjectRequest request) {
        Account account = ownershipValidator.resolveAuthenticatedAccount(authenticatedUsername);
        Project project = ownershipValidator.requireProjectMembership(projectId, account);

        project.setName(normalizeName(request.getName()));
        project.setDescription(request.getDescription());
        project.setRequirements(request.getRequirements());

        Project saved = projectRepository.saveAndFlush(project);
        return toResponse(saved, resolveCallersRole(saved, account));
    }

    @Transactional
    public void deleteProject(String authenticatedUsername, UUID projectId) {
        Account account = ownershipValidator.resolveAuthenticatedAccount(authenticatedUsername);
        Project project = ownershipValidator.requireProjectOwnership(projectId, account);

        componentRepository.deleteAll(componentRepository.findByProjectId(project.getId()));
        projectInvitationRepository.deleteAll(projectInvitationRepository.findByProjectId(project.getId()));
        projectMemberRepository.deleteAll(projectMemberRepository.findByProjectId(project.getId()));
        projectRepository.delete(project);
    }

    private String normalizeName(String rawName) {
        String trimmed = rawName == null ? "" : rawName.trim();
        if (!StringUtils.hasText(trimmed)) {
            throw new IllegalArgumentException("Project name must not be blank");
        }
        return trimmed;
    }

    private Role resolveCallersRole(Project project, Account account) {
        return project.getOwner().getId().equals(account.getId()) ? Role.OWNER : Role.COLLABORATOR;
    }

    private ProjectResponse toResponse(Project project, Role role) {
        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .requirements(project.getRequirements())
                .ownerId(project.getOwner().getId())
                .role(role)
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }

    private ComponentResponse toComponentResponse(Component component) {
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