package org.example.aipowereddesigntbackend.Service;

import org.example.aipowereddesigntbackend.Dto.ComponentRequest;
import org.example.aipowereddesigntbackend.Dto.ComponentResponse;
import org.example.aipowereddesigntbackend.Enum.ComponentType;
import org.example.aipowereddesigntbackend.Enum.Role;
import org.example.aipowereddesigntbackend.ExceptionHandling.ResourceNotFoundException;
import org.example.aipowereddesigntbackend.Model.Account;
import org.example.aipowereddesigntbackend.Model.Component;
import org.example.aipowereddesigntbackend.Model.Project;
import org.example.aipowereddesigntbackend.Repository.ComponentRepository;
import org.example.aipowereddesigntbackend.Security.OwnershipValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComponentServiceTest {

    @Mock
    private ComponentRepository componentRepository;

    @Mock
    private OwnershipValidator ownershipValidator;

    @InjectMocks
    private ComponentService componentService;

    private Account owner;
    private Account collaborator;
    private Account stranger;
    private Project project;

    private static final String OWNER_EMAIL = "owner@example.com";
    private static final String COLLABORATOR_EMAIL = "collaborator@example.com";
    private static final String STRANGER_EMAIL = "stranger@example.com";

    @BeforeEach
    void setUp() {
        owner = new Account();
        owner.setId(UUID.randomUUID());
        owner.setUserName(OWNER_EMAIL);
        owner.setFullName("Owner");
        owner.setRole(Role.OWNER);

        collaborator = new Account();
        collaborator.setId(UUID.randomUUID());
        collaborator.setUserName(COLLABORATOR_EMAIL);
        collaborator.setFullName("Collaborator");
        collaborator.setRole(Role.OWNER);

        stranger = new Account();
        stranger.setId(UUID.randomUUID());
        stranger.setUserName(STRANGER_EMAIL);
        stranger.setFullName("Stranger");
        stranger.setRole(Role.OWNER);

        project = new Project();
        project.setId(UUID.randomUUID());
        project.setName("Test Project");
        project.setOwner(owner);
    }

    private ComponentRequest validRequest() {
        return new ComponentRequest("Web Frontend", ComponentType.FRONTEND, "React SPA");
    }

    @Test
    void addComponent_asOwner_savesComponent() {
        when(ownershipValidator.resolveAuthenticatedAccount(OWNER_EMAIL)).thenReturn(owner);
        when(ownershipValidator.requireProjectMembership(project.getId(), owner)).thenReturn(project);
        when(componentRepository.saveAndFlush(any(Component.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ComponentResponse response = componentService.addComponent(OWNER_EMAIL, project.getId(), validRequest());

        assertThat(response.getName()).isEqualTo("Web Frontend");
        assertThat(response.getType()).isEqualTo(ComponentType.FRONTEND);
        assertThat(response.getProjectId()).isEqualTo(project.getId());
    }

    @Test
    void addComponent_asAcceptedCollaborator_savesComponent() {
        when(ownershipValidator.resolveAuthenticatedAccount(COLLABORATOR_EMAIL)).thenReturn(collaborator);
        when(ownershipValidator.requireProjectMembership(project.getId(), collaborator)).thenReturn(project);
        when(componentRepository.saveAndFlush(any(Component.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ComponentResponse response =
                componentService.addComponent(COLLABORATOR_EMAIL, project.getId(), validRequest());

        assertThat(response.getName()).isEqualTo("Web Frontend");
    }

    @Test
    void addComponent_byNonMember_throwsAccessDenied() {
        when(ownershipValidator.resolveAuthenticatedAccount(STRANGER_EMAIL)).thenReturn(stranger);
        when(ownershipValidator.requireProjectMembership(project.getId(), stranger))
                .thenThrow(new AccessDeniedException("You do not have access to this project"));

        assertThatThrownBy(() -> componentService.addComponent(STRANGER_EMAIL, project.getId(), validRequest()))
                .isInstanceOf(AccessDeniedException.class);

        verify(componentRepository, never()).saveAndFlush(any());
    }

    @Test
    void addComponent_withBlankName_throwsIllegalArgumentException() {
        when(ownershipValidator.resolveAuthenticatedAccount(OWNER_EMAIL)).thenReturn(owner);
        when(ownershipValidator.requireProjectMembership(project.getId(), owner)).thenReturn(project);

        ComponentRequest request = new ComponentRequest("   ", ComponentType.BACKEND, null);

        assertThatThrownBy(() -> componentService.addComponent(OWNER_EMAIL, project.getId(), request))
                .isInstanceOf(IllegalArgumentException.class);

        verify(componentRepository, never()).saveAndFlush(any());
    }

    @Test
    void addComponent_withUnknownProject_throwsResourceNotFoundException() {
        when(ownershipValidator.resolveAuthenticatedAccount(OWNER_EMAIL)).thenReturn(owner);
        when(ownershipValidator.requireProjectMembership(project.getId(), owner))
                .thenThrow(new ResourceNotFoundException("Project not found"));

        assertThatThrownBy(() -> componentService.addComponent(OWNER_EMAIL, project.getId(), validRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void addComponent_withMissingAuthenticatedUser_throwsInsufficientAuthenticationException() {
        when(ownershipValidator.resolveAuthenticatedAccount(null))
                .thenThrow(new InsufficientAuthenticationException("Authenticated user could not be resolved"));

        assertThatThrownBy(() -> componentService.addComponent(null, project.getId(), validRequest()))
                .isInstanceOf(InsufficientAuthenticationException.class);
    }

    @Test
    void listComponents_returnsComponentsForMember() {
        Component component = new Component();
        component.setId(UUID.randomUUID());
        component.setProject(project);
        component.setName("Database");
        component.setType(ComponentType.DATABASE);

        when(ownershipValidator.resolveAuthenticatedAccount(OWNER_EMAIL)).thenReturn(owner);
        when(ownershipValidator.requireProjectMembership(project.getId(), owner)).thenReturn(project);
        when(componentRepository.findByProjectId(project.getId())).thenReturn(List.of(component));

        List<ComponentResponse> responses = componentService.listComponents(OWNER_EMAIL, project.getId());

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getName()).isEqualTo("Database");
    }

    @Test
    void updateComponent_byMember_updatesFields() {
        Component existing = new Component();
        existing.setId(UUID.randomUUID());
        existing.setProject(project);
        existing.setName("Old Name");
        existing.setType(ComponentType.CACHE);

        ComponentRequest request = new ComponentRequest("New Name", ComponentType.DATABASE, "Updated description");

        when(ownershipValidator.resolveAuthenticatedAccount(OWNER_EMAIL)).thenReturn(owner);
        when(componentRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(componentRepository.saveAndFlush(any(Component.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ComponentResponse response = componentService.updateComponent(OWNER_EMAIL, existing.getId(), request);

        assertThat(response.getName()).isEqualTo("New Name");
        assertThat(response.getType()).isEqualTo(ComponentType.DATABASE);
        assertThat(response.getDescription()).isEqualTo("Updated description");
    }

    @Test
    void updateComponent_byNonMember_throwsAccessDenied() {
        Component existing = new Component();
        existing.setId(UUID.randomUUID());
        existing.setProject(project);
        existing.setName("Old Name");
        existing.setType(ComponentType.CACHE);

        when(ownershipValidator.resolveAuthenticatedAccount(STRANGER_EMAIL)).thenReturn(stranger);
        when(componentRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        doThrow(new AccessDeniedException("You do not have access to this project"))
                .when(ownershipValidator).requireMembership(project, stranger);

        assertThatThrownBy(() -> componentService.updateComponent(STRANGER_EMAIL, existing.getId(), validRequest()))
                .isInstanceOf(AccessDeniedException.class);

        verify(componentRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateComponent_withUnknownId_throwsResourceNotFoundException() {
        UUID unknownId = UUID.randomUUID();
        when(ownershipValidator.resolveAuthenticatedAccount(OWNER_EMAIL)).thenReturn(owner);
        when(componentRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> componentService.updateComponent(OWNER_EMAIL, unknownId, validRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteComponent_byMember_deletesComponent() {
        Component existing = new Component();
        existing.setId(UUID.randomUUID());
        existing.setProject(project);
        existing.setName("To Delete");
        existing.setType(ComponentType.EXTERNAL_API);

        when(ownershipValidator.resolveAuthenticatedAccount(OWNER_EMAIL)).thenReturn(owner);
        when(componentRepository.findById(existing.getId())).thenReturn(Optional.of(existing));

        componentService.deleteComponent(OWNER_EMAIL, existing.getId());

        verify(componentRepository).delete(existing);
    }

    @Test
    void deleteComponent_byNonMember_throwsAccessDeniedAndDoesNotDelete() {
        Component existing = new Component();
        existing.setId(UUID.randomUUID());
        existing.setProject(project);
        existing.setName("To Delete");
        existing.setType(ComponentType.EXTERNAL_API);

        when(ownershipValidator.resolveAuthenticatedAccount(STRANGER_EMAIL)).thenReturn(stranger);
        when(componentRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        doThrow(new AccessDeniedException("You do not have access to this project"))
                .when(ownershipValidator).requireMembership(project, stranger);

        assertThatThrownBy(() -> componentService.deleteComponent(STRANGER_EMAIL, existing.getId()))
                .isInstanceOf(AccessDeniedException.class);

        verify(componentRepository, never()).delete(any());
    }
}