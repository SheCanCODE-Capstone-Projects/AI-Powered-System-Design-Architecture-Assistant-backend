package org.example.aipowereddesigntbackend.Service;

import org.example.aipowereddesigntbackend.Dto.ProjectInvitationResponse;
import org.example.aipowereddesigntbackend.Dto.ProjectMemberResponse;
import org.example.aipowereddesigntbackend.Enum.InvitationStatus;
import org.example.aipowereddesigntbackend.Enum.Role;
import org.example.aipowereddesigntbackend.ExceptionHandling.ConflictException;
import org.example.aipowereddesigntbackend.ExceptionHandling.ResourceNotFoundException;
import org.example.aipowereddesigntbackend.Model.Account;
import org.example.aipowereddesigntbackend.Model.Project;
import org.example.aipowereddesigntbackend.Model.ProjectInvitation;
import org.example.aipowereddesigntbackend.Model.ProjectMember;
import org.example.aipowereddesigntbackend.Repository.AccountRepository;
import org.example.aipowereddesigntbackend.Repository.ProjectInvitationRepository;
import org.example.aipowereddesigntbackend.Repository.ProjectMemberRepository;
import org.example.aipowereddesigntbackend.Security.OwnershipValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvitationServiceTest {

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private ProjectInvitationRepository invitationRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private OwnershipValidator ownershipValidator;

    @InjectMocks
    private InvitationService invitationService;

    private Account owner;
    private Account invitee;
    private Project project;

    private static final String OWNER_EMAIL = "owner@example.com";
    private static final String INVITEE_EMAIL = "invitee@example.com";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(invitationService, "acceptBaseUrl", "https://example.com/invitations/accept");

        owner = new Account();
        owner.setId(UUID.randomUUID());
        owner.setUserName(OWNER_EMAIL);
        owner.setFullName("Project Owner");
        owner.setProvider("GOOGLE");
        owner.setRole(Role.OWNER);

        invitee = new Account();
        invitee.setId(UUID.randomUUID());
        invitee.setUserName(INVITEE_EMAIL);
        invitee.setFullName("Invitee User");
        invitee.setProvider("GOOGLE");
        invitee.setRole(Role.OWNER);

        project = new Project();
        project.setId(UUID.randomUUID());
        project.setName("Test Project");
        project.setOwner(owner);
    }

    private ProjectInvitation pendingInvitation(String email) {
        ProjectInvitation invitation = new ProjectInvitation();
        invitation.setId(UUID.randomUUID());
        invitation.setProject(project);
        invitation.setInvitedEmail(email);
        invitation.setInvitedBy(owner);
        invitation.setToken(UUID.randomUUID().toString());
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setExpiresAt(Timestamp.from(Instant.now().plus(7, ChronoUnit.DAYS)));
        return invitation;
    }

    @Test
    void inviteCollaborator_withValidRequest_savesInvitationAndSendsEmail() {
        when(ownershipValidator.resolveAuthenticatedAccount(OWNER_EMAIL)).thenReturn(owner);
        when(ownershipValidator.requireProjectOwnership(project.getId(), owner)).thenReturn(project);
        when(accountRepository.findAccountByUserName(INVITEE_EMAIL)).thenReturn(null);
        when(invitationRepository.existsByProjectIdAndInvitedEmailIgnoreCaseAndStatus(
                project.getId(), INVITEE_EMAIL, InvitationStatus.PENDING)).thenReturn(false);
        when(invitationRepository.saveAndFlush(any(ProjectInvitation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProjectInvitationResponse response =
                invitationService.inviteCollaborator(OWNER_EMAIL, project.getId(), INVITEE_EMAIL);

        assertThat(response.getInvitedEmail()).isEqualTo(INVITEE_EMAIL);
        assertThat(response.getStatus()).isEqualTo(InvitationStatus.PENDING);
        assertThat(response.getProjectId()).isEqualTo(project.getId());

        verify(emailService).sendProjectInvitationEmail(
                eq(INVITEE_EMAIL), eq(project.getName()), eq(owner.getFullName()), anyString());
    }

    @Test
    void inviteCollaborator_byNonOwner_throwsAccessDenied() {
        Account otherUser = new Account();
        otherUser.setId(UUID.randomUUID());
        otherUser.setUserName("other@example.com");

        when(ownershipValidator.resolveAuthenticatedAccount("other@example.com")).thenReturn(otherUser);
        when(ownershipValidator.requireProjectOwnership(project.getId(), otherUser))
                .thenThrow(new AccessDeniedException("Only the project owner can perform this action"));

        assertThatThrownBy(() -> invitationService.inviteCollaborator(
                "other@example.com", project.getId(), INVITEE_EMAIL))
                .isInstanceOf(AccessDeniedException.class);

        verify(invitationRepository, never()).saveAndFlush(any());
    }

    @Test
    void inviteCollaborator_self_throwsIllegalArgumentException() {
        when(ownershipValidator.resolveAuthenticatedAccount(OWNER_EMAIL)).thenReturn(owner);
        when(ownershipValidator.requireProjectOwnership(project.getId(), owner)).thenReturn(project);

        assertThatThrownBy(() -> invitationService.inviteCollaborator(OWNER_EMAIL, project.getId(), OWNER_EMAIL))
                .isInstanceOf(IllegalArgumentException.class);

        verify(invitationRepository, never()).saveAndFlush(any());
    }

    @Test
    void inviteCollaborator_alreadyMember_throwsConflictException() {
        when(ownershipValidator.resolveAuthenticatedAccount(OWNER_EMAIL)).thenReturn(owner);
        when(ownershipValidator.requireProjectOwnership(project.getId(), owner)).thenReturn(project);
        when(accountRepository.findAccountByUserName(INVITEE_EMAIL)).thenReturn(invitee);
        when(projectMemberRepository.existsByProjectIdAndAccountId(project.getId(), invitee.getId()))
                .thenReturn(true);

        assertThatThrownBy(() -> invitationService.inviteCollaborator(OWNER_EMAIL, project.getId(), INVITEE_EMAIL))
                .isInstanceOf(ConflictException.class);

        verify(invitationRepository, never()).saveAndFlush(any());
    }

    @Test
    void inviteCollaborator_withPendingInvitationAlready_throwsConflictException() {
        when(ownershipValidator.resolveAuthenticatedAccount(OWNER_EMAIL)).thenReturn(owner);
        when(ownershipValidator.requireProjectOwnership(project.getId(), owner)).thenReturn(project);
        when(accountRepository.findAccountByUserName(INVITEE_EMAIL)).thenReturn(null);
        when(invitationRepository.existsByProjectIdAndInvitedEmailIgnoreCaseAndStatus(
                project.getId(), INVITEE_EMAIL, InvitationStatus.PENDING)).thenReturn(true);

        assertThatThrownBy(() -> invitationService.inviteCollaborator(OWNER_EMAIL, project.getId(), INVITEE_EMAIL))
                .isInstanceOf(ConflictException.class);

        verify(invitationRepository, never()).saveAndFlush(any());
    }

    @Test
    void inviteCollaborator_projectNotFound_throwsResourceNotFoundException() {
        when(ownershipValidator.resolveAuthenticatedAccount(OWNER_EMAIL)).thenReturn(owner);
        when(ownershipValidator.requireProjectOwnership(project.getId(), owner))
                .thenThrow(new ResourceNotFoundException("Project not found"));

        assertThatThrownBy(() -> invitationService.inviteCollaborator(OWNER_EMAIL, project.getId(), INVITEE_EMAIL))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void acceptInvitation_withValidPendingInvitation_createsMembershipAndMarksAccepted() {
        ProjectInvitation invitation = pendingInvitation(INVITEE_EMAIL);

        when(ownershipValidator.resolveAuthenticatedAccount(INVITEE_EMAIL)).thenReturn(invitee);
        when(invitationRepository.findByToken(invitation.getToken())).thenReturn(Optional.of(invitation));
        when(projectMemberRepository.saveAndFlush(any(ProjectMember.class)))
                .thenAnswer(invocation -> {
                    ProjectMember member = invocation.getArgument(0);
                    member.setId(UUID.randomUUID());
                    member.setCreatedAt(Timestamp.from(Instant.now()));
                    return member;
                });

        ProjectMemberResponse response = invitationService.acceptInvitation(INVITEE_EMAIL, invitation.getToken());

        assertThat(response.getRole()).isEqualTo(Role.COLLABORATOR);
        assertThat(response.getAccountId()).isEqualTo(invitee.getId());
        assertThat(response.getProjectId()).isEqualTo(project.getId());

        ArgumentCaptor<ProjectInvitation> invitationCaptor = ArgumentCaptor.forClass(ProjectInvitation.class);
        verify(invitationRepository).save(invitationCaptor.capture());
        assertThat(invitationCaptor.getValue().getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
    }

    @Test
    void acceptInvitation_withMismatchedEmail_throwsAccessDenied() {
        ProjectInvitation invitation = pendingInvitation(INVITEE_EMAIL);

        when(ownershipValidator.resolveAuthenticatedAccount(OWNER_EMAIL)).thenReturn(owner);
        when(invitationRepository.findByToken(invitation.getToken())).thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> invitationService.acceptInvitation(OWNER_EMAIL, invitation.getToken()))
                .isInstanceOf(AccessDeniedException.class);

        verify(projectMemberRepository, never()).saveAndFlush(any());
    }

    @Test
    void acceptInvitation_whenExpired_throwsConflictException() {
        ProjectInvitation invitation = pendingInvitation(INVITEE_EMAIL);
        invitation.setExpiresAt(Timestamp.from(Instant.now().minus(1, ChronoUnit.DAYS)));

        when(ownershipValidator.resolveAuthenticatedAccount(INVITEE_EMAIL)).thenReturn(invitee);
        when(invitationRepository.findByToken(invitation.getToken())).thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> invitationService.acceptInvitation(INVITEE_EMAIL, invitation.getToken()))
                .isInstanceOf(ConflictException.class);

        verify(projectMemberRepository, never()).saveAndFlush(any());
    }

    @Test
    void acceptInvitation_whenAlreadyAccepted_throwsConflictException() {
        ProjectInvitation invitation = pendingInvitation(INVITEE_EMAIL);
        invitation.setStatus(InvitationStatus.ACCEPTED);

        when(ownershipValidator.resolveAuthenticatedAccount(INVITEE_EMAIL)).thenReturn(invitee);
        when(invitationRepository.findByToken(invitation.getToken())).thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> invitationService.acceptInvitation(INVITEE_EMAIL, invitation.getToken()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void acceptInvitation_withUnknownToken_throwsResourceNotFoundException() {
        when(ownershipValidator.resolveAuthenticatedAccount(INVITEE_EMAIL)).thenReturn(invitee);
        when(invitationRepository.findByToken("unknown-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invitationService.acceptInvitation(INVITEE_EMAIL, "unknown-token"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void declineInvitation_withValidPendingInvitation_marksDeclined() {
        ProjectInvitation invitation = pendingInvitation(INVITEE_EMAIL);

        when(ownershipValidator.resolveAuthenticatedAccount(INVITEE_EMAIL)).thenReturn(invitee);
        when(invitationRepository.findByToken(invitation.getToken())).thenReturn(Optional.of(invitation));

        invitationService.declineInvitation(INVITEE_EMAIL, invitation.getToken());

        ArgumentCaptor<ProjectInvitation> captor = ArgumentCaptor.forClass(ProjectInvitation.class);
        verify(invitationRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(InvitationStatus.DECLINED);
    }

    @Test
    void revokeInvitation_byOwnerOnPendingInvitation_marksRevoked() {
        ProjectInvitation invitation = pendingInvitation(INVITEE_EMAIL);

        when(ownershipValidator.resolveAuthenticatedAccount(OWNER_EMAIL)).thenReturn(owner);
        when(ownershipValidator.requireProjectOwnership(project.getId(), owner)).thenReturn(project);
        when(invitationRepository.findById(invitation.getId())).thenReturn(Optional.of(invitation));

        invitationService.revokeInvitation(OWNER_EMAIL, project.getId(), invitation.getId());

        ArgumentCaptor<ProjectInvitation> captor = ArgumentCaptor.forClass(ProjectInvitation.class);
        verify(invitationRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(InvitationStatus.REVOKED);
    }

    @Test
    void revokeInvitation_byNonOwner_throwsAccessDenied() {
        when(ownershipValidator.resolveAuthenticatedAccount(INVITEE_EMAIL)).thenReturn(invitee);
        when(ownershipValidator.requireProjectOwnership(project.getId(), invitee))
                .thenThrow(new AccessDeniedException("Only the project owner can perform this action"));

        assertThatThrownBy(() -> invitationService.revokeInvitation(
                INVITEE_EMAIL, project.getId(), UUID.randomUUID()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void revokeInvitation_whenAlreadyAccepted_throwsConflictException() {
        ProjectInvitation invitation = pendingInvitation(INVITEE_EMAIL);
        invitation.setStatus(InvitationStatus.ACCEPTED);

        when(ownershipValidator.resolveAuthenticatedAccount(OWNER_EMAIL)).thenReturn(owner);
        when(ownershipValidator.requireProjectOwnership(project.getId(), owner)).thenReturn(project);
        when(invitationRepository.findById(invitation.getId())).thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> invitationService.revokeInvitation(OWNER_EMAIL, project.getId(), invitation.getId()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void removeMember_whenTargetIsOwner_throwsConflictException() {
        ProjectMember ownerMembership = new ProjectMember();
        ownerMembership.setId(UUID.randomUUID());
        ownerMembership.setProject(project);
        ownerMembership.setAccount(owner);
        ownerMembership.setRole(Role.OWNER);

        when(ownershipValidator.resolveAuthenticatedAccount(OWNER_EMAIL)).thenReturn(owner);
        when(ownershipValidator.requireProjectOwnership(project.getId(), owner)).thenReturn(project);
        when(projectMemberRepository.findById(ownerMembership.getId())).thenReturn(Optional.of(ownerMembership));

        assertThatThrownBy(() -> invitationService.removeMember(OWNER_EMAIL, project.getId(), ownerMembership.getId()))
                .isInstanceOf(ConflictException.class);

        verify(projectMemberRepository, never()).delete(any());
    }

    @Test
    void removeMember_whenTargetIsCollaborator_removesMembership() {
        ProjectMember collaboratorMembership = new ProjectMember();
        collaboratorMembership.setId(UUID.randomUUID());
        collaboratorMembership.setProject(project);
        collaboratorMembership.setAccount(invitee);
        collaboratorMembership.setRole(Role.COLLABORATOR);

        when(ownershipValidator.resolveAuthenticatedAccount(OWNER_EMAIL)).thenReturn(owner);
        when(ownershipValidator.requireProjectOwnership(project.getId(), owner)).thenReturn(project);
        when(projectMemberRepository.findById(collaboratorMembership.getId()))
                .thenReturn(Optional.of(collaboratorMembership));

        invitationService.removeMember(OWNER_EMAIL, project.getId(), collaboratorMembership.getId());

        verify(projectMemberRepository).delete(collaboratorMembership);
    }
}