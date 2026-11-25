package org.example.aipowereddesigntbackend.Service;

import org.example.aipowereddesigntbackend.Dto.ComponentResponse;
import org.example.aipowereddesigntbackend.Dto.CreateProjectRequest;
import org.example.aipowereddesigntbackend.Dto.ProjectDetailResponse;
import org.example.aipowereddesigntbackend.Dto.ProjectResponse;
import org.example.aipowereddesigntbackend.Enum.ComponentType;
import org.example.aipowereddesigntbackend.Enum.Role;
import org.example.aipowereddesigntbackend.ExceptionHandling.ResourceNotFoundException;
import org.example.aipowereddesigntbackend.Model.Account;
import org.example.aipowereddesigntbackend.Model.Component;
import org.example.aipowereddesigntbackend.Model.Project;
import org.example.aipowereddesigntbackend.Model.ProjectMember;
import org.example.aipowereddesigntbackend.Repository.ComponentRepository;
import org.example.aipowereddesigntbackend.Repository.ProjectInvitationRepository;
import org.example.aipowereddesigntbackend.Repository.ProjectMemberRepository;
import org.example.aipowereddesigntbackend.Repository.ProjectRepository;
import org.example.aipowereddesigntbackend.Security.OwnershipValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private ProjectInvitationRepository projectInvitationRepository;

    @Mock
    private ComponentRepository componentRepository;

    @Mock
    private OwnershipValidator ownershipValidator;

    @InjectMocks
    private ProjectService projectService;

    private Account owner;
    private Account collaborator;
    private static final String OWNER_EMAIL = "owner@example.com";
    private static final String COLLABORATOR_EMAIL = "collaborator@example.com";

    @BeforeEach
    void setUp() {
        owner = new Account();
        owner.setId(UUID.randomUUID());
        owner.setUserName(OWNER_EMAIL);
        owner.setFullName("Project Owner");
        owner.setProvider("GOOGLE");
        owner.setRole(Role.OWNER);

        collaborator = new Account();
        collaborator.setId(UUID.randomUUID());
        collaborator.setUserName(COLLABORATOR_EMAIL);
        collaborator.setFullName("Collaborator");
        collaborator.setProvider("GOOGLE");
        collaborator.setRole(Role.OWNER);
    }

    private Project buildProject(String name, Account owner) {
        Project project = new Project();
        project.setId(UUID.randomUUID());
        project.setName(name);
        project.setOwner(owner);
        project.setCreatedAt(Timestamp.from(java.time.Instant.now()));
        project.setUpdatedAt(Timestamp.from(java.time.Instant.now()));
        return project;
    }

    @Test
    void createProject_withValidRequestAndAuthenticatedUser_savesProjectAndOwnerMembership() {
        CreateProjectRequest request = new CreateProjectRequest(
                "  E-commerce Platform  ",
                "System architecture for an online shopping platform",
                "Authentication, payment processing and product management"
        );

        when(ownershipValidator.resolveAuthenticatedAccount(OWNER_EMAIL)).thenReturn(owner);
        when(projectRepository.saveAndFlush(any(Project.class))).thenAnswer(invocation -> {
            Project project = invocation.getArgument(0);
            project.setId(UUID.randomUUID());
            project.setCreatedAt(Timestamp.from(java.time.Instant.now()));
            project.setUpdatedAt(Timestamp.from(java.time.Instant.now()));
            return project;
        });
        when(projectMemberRepository.save(any(ProjectMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProjectResponse response = projectService.createProject(OWNER_EMAIL, request);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getName()).isEqualTo("E-commerce Platform");
        assertThat(response.getDescription()).isEqualTo(request.getDescription());
        assertThat(response.getRequirements()).isEqualTo(request.getRequirements());
        assertThat(response.getOwnerId()).isEqualTo(owner.getId());
        assertThat(response.getRole()).isEqualTo(Role.OWNER);
        assertThat(response.getCreatedAt()).isNotNull();
        assertThat(response.getUpdatedAt()).isNotNull();
    }

    @Test
    void createProject_assignsAuthenticatedUserAsOwnerOfProjectAndMembership() {
        CreateProjectRequest request = new CreateProjectRequest("Inventory System", null, null);

        when(ownershipValidator.resolveAuthenticatedAccount(OWNER_EMAIL)).thenReturn(owner);
        when(projectRepository.saveAndFlush(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(projectMemberRepository.save(any(ProjectMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        projectService.createProject(OWNER_EMAIL, request);

        ArgumentCaptor<Project> projectCaptor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).saveAndFlush(projectCaptor.capture());
        assertThat(projectCaptor.getValue().getOwner()).isEqualTo(owner);

        ArgumentCaptor<ProjectMember> memberCaptor = ArgumentCaptor.forClass(ProjectMember.class);
        verify(projectMemberRepository).save(memberCaptor.capture());
        assertThat(memberCaptor.getValue().getAccount()).isEqualTo(owner);
        assertThat(memberCaptor.getValue().getRole()).isEqualTo(Role.OWNER);
    }

    @Test
    void createProject_withBlankName_throwsIllegalArgumentException() {
        CreateProjectRequest request = new CreateProjectRequest("   ", "desc", null);

        when(ownershipValidator.resolveAuthenticatedAccount(OWNER_EMAIL)).thenReturn(owner);

        assertThatThrownBy(() -> projectService.createProject(OWNER_EMAIL, request))
                .isInstanceOf(IllegalArgumentException.class);

        verify(projectRepository, never()).saveAndFlush(any());
        verify(projectMemberRepository, never()).save(any());
    }

    @Test
    void createProject_withMissingAuthenticatedUser_throwsInsufficientAuthenticationException() {
        CreateProjectRequest request = new CreateProjectRequest("Valid Name", null, null);

        when(ownershipValidator.resolveAuthenticatedAccount(null))
                .thenThrow(new InsufficientAuthenticationException("Authenticated user could not be resolved"));

        assertThatThrownBy(() -> projectService.createProject(null, request))
                .isInstanceOf(InsufficientAuthenticationException.class);

        verify(projectRepository, never()).saveAndFlush(any());
    }

    @Test
    void createProject_whenDatabasePersistenceFails_propagatesExceptionAndSkipsMembershipSave() {
        CreateProjectRequest request = new CreateProjectRequest("Valid Name", null, null);

        when(ownershipValidator.resolveAuthenticatedAccount(OWNER_EMAIL)).thenReturn(owner);
        when(projectRepository.saveAndFlush(any(Project.class)))
                .thenThrow(new DataAccessResourceFailureException("DB unavailable"));

        assertThatThrownBy(() -> projectService.createProject(OWNER_EMAIL, request))
                .isInstanceOf(DataAccessResourceFailureException.class);

        verify(projectMemberRepository, never()).save(any());
    }

    @Test
    void listMyProjects_returnsProjectsWithCallersRoleFromMembership() {
        ProjectMember ownerMembership = new ProjectMember();
        ownerMembership.setProject(buildProject("Owned Project", owner));
        ownerMembership.setRole(Role.OWNER);

        ProjectMember collaboratorMembership = new ProjectMember();
        collaboratorMembership.setProject(buildProject("Collaborating Project", owner));
        collaboratorMembership.setRole(Role.COLLABORATOR);

        when(ownershipValidator.resolveAuthenticatedAccount(OWNER_EMAIL)).thenReturn(owner);
        when(projectMemberRepository.findByAccountId(owner.getId()))
                .thenReturn(List.of(ownerMembership, collaboratorMembership));

        List<ProjectResponse> responses = projectService.listMyProjects(OWNER_EMAIL);

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(ProjectResponse::getRole)
                .containsExactlyInAnyOrder(Role.OWNER, Role.COLLABORATOR);
    }

    @Test
    void getProject_asMember_returnsProjectWithComponentsAndCorrectRole() {
        Project project = buildProject("Detailed Project", owner);
        Component component = new Component();
        component.setId(UUID.randomUUID());
        component.setProject(project);
        component.setName("Database");
        component.setType(ComponentType.DATABASE);

        when(ownershipValidator.resolveAuthenticatedAccount(COLLABORATOR_EMAIL)).thenReturn(collaborator);
        when(ownershipValidator.requireProjectMembership(project.getId(), collaborator)).thenReturn(project);
        when(componentRepository.findByProjectId(project.getId())).thenReturn(List.of(component));

        ProjectDetailResponse response = projectService.getProject(COLLABORATOR_EMAIL, project.getId());

        assertThat(response.getProject().getId()).isEqualTo(project.getId());
        assertThat(response.getProject().getRole()).isEqualTo(Role.COLLABORATOR);
        assertThat(response.getComponents()).extracting(ComponentResponse::getName).containsExactly("Database");
    }

    @Test
    void getProject_byNonMember_throwsAccessDenied() {
        Project project = buildProject("Private Project", owner);

        when(ownershipValidator.resolveAuthenticatedAccount(COLLABORATOR_EMAIL)).thenReturn(collaborator);
        when(ownershipValidator.requireProjectMembership(project.getId(), collaborator))
                .thenThrow(new AccessDeniedException("You do not have access to this project"));

        assertThatThrownBy(() -> projectService.getProject(COLLABORATOR_EMAIL, project.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getProject_withUnknownId_throwsResourceNotFoundException() {
        UUID unknownId = UUID.randomUUID();
        when(ownershipValidator.resolveAuthenticatedAccount(OWNER_EMAIL)).thenReturn(owner);
        when(ownershipValidator.requireProjectMembership(unknownId, owner))
                .thenThrow(new ResourceNotFoundException("Project not found"));

        assertThatThrownBy(() -> projectService.getProject(OWNER_EMAIL, unknownId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateProject_asMember_updatesFields() {
        Project project = buildProject("Old Name", owner);
        CreateProjectRequest request = new CreateProjectRequest("New Name", "New description", "New requirements");

        when(ownershipValidator.resolveAuthenticatedAccount(OWNER_EMAIL)).thenReturn(owner);
        when(ownershipValidator.requireProjectMembership(project.getId(), owner)).thenReturn(project);
        when(projectRepository.saveAndFlush(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectResponse response = projectService.updateProject(OWNER_EMAIL, project.getId(), request);

        assertThat(response.getName()).isEqualTo("New Name");
        assertThat(response.getDescription()).isEqualTo("New description");
        assertThat(response.getRequirements()).isEqualTo("New requirements");
        assertThat(response.getRole()).isEqualTo(Role.OWNER);
    }

    @Test
    void updateProject_byNonMember_throwsAccessDenied() {
        Project project = buildProject("Private Project", owner);
        CreateProjectRequest request = new CreateProjectRequest("Hijacked Name", null, null);

        when(ownershipValidator.resolveAuthenticatedAccount(COLLABORATOR_EMAIL)).thenReturn(collaborator);
        when(ownershipValidator.requireProjectMembership(project.getId(), collaborator))
                .thenThrow(new AccessDeniedException("You do not have access to this project"));

        assertThatThrownBy(() -> projectService.updateProject(COLLABORATOR_EMAIL, project.getId(), request))
                .isInstanceOf(AccessDeniedException.class);

        verify(projectRepository, never()).saveAndFlush(any());
    }

    @Test
    void deleteProject_byOwner_cascadesDeleteAndRemovesProject() {
        Project project = buildProject("Doomed Project", owner);

        when(ownershipValidator.resolveAuthenticatedAccount(OWNER_EMAIL)).thenReturn(owner);
        when(ownershipValidator.requireProjectOwnership(project.getId(), owner)).thenReturn(project);
        when(componentRepository.findByProjectId(project.getId())).thenReturn(List.of());
        when(projectInvitationRepository.findByProjectId(project.getId())).thenReturn(List.of());
        when(projectMemberRepository.findByProjectId(project.getId())).thenReturn(List.of());

        projectService.deleteProject(OWNER_EMAIL, project.getId());

        verify(projectRepository).delete(project);
    }

    @Test
    void deleteProject_byCollaborator_throwsAccessDeniedAndDoesNotDelete() {
        Project project = buildProject("Protected Project", owner);

        when(ownershipValidator.resolveAuthenticatedAccount(COLLABORATOR_EMAIL)).thenReturn(collaborator);
        when(ownershipValidator.requireProjectOwnership(project.getId(), collaborator))
                .thenThrow(new AccessDeniedException("Only the project owner can perform this action"));

        assertThatThrownBy(() -> projectService.deleteProject(COLLABORATOR_EMAIL, project.getId()))
                .isInstanceOf(AccessDeniedException.class);

        verify(projectRepository, never()).delete(any());
    }
}