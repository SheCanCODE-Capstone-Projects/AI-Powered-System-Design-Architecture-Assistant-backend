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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class InvitationService {

    private static final int EXPIRY_DAYS = 7;

    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectInvitationRepository invitationRepository;
    private final AccountRepository accountRepository;
    private final EmailService emailService;
    private final OwnershipValidator ownershipValidator;

    @Value("${app.invitation.accept-url}")
    private String acceptBaseUrl;

    public InvitationService(
            ProjectMemberRepository projectMemberRepository,
            ProjectInvitationRepository invitationRepository,
            AccountRepository accountRepository,
            EmailService emailService,
            OwnershipValidator ownershipValidator
    ) {
        this.projectMemberRepository = projectMemberRepository;
        this.invitationRepository = invitationRepository;
        this.accountRepository = accountRepository;
        this.emailService = emailService;
        this.ownershipValidator = ownershipValidator;
    }

    @Transactional
    public ProjectInvitationResponse inviteCollaborator(String ownerEmail, UUID projectId, String invitedEmailRaw) {
        Account owner = ownershipValidator.resolveAuthenticatedAccount(ownerEmail);
        Project project = ownershipValidator.requireProjectOwnership(projectId, owner);

        String invitedEmail = invitedEmailRaw == null ? "" : invitedEmailRaw.trim().toLowerCase();
        if (!StringUtils.hasText(invitedEmail)) {
            throw new IllegalArgumentException("Email is required");
        }
        if (invitedEmail.equalsIgnoreCase(owner.getUserName())) {
            throw new IllegalArgumentException("You cannot invite yourself to your own project");
        }

        Account existingAccount = accountRepository.findAccountByUserName(invitedEmail);
        if (existingAccount != null
                && projectMemberRepository.existsByProjectIdAndAccountId(project.getId(), existingAccount.getId())) {
            throw new ConflictException("This user is already a member of the project");
        }

        if (invitationRepository.existsByProjectIdAndInvitedEmailIgnoreCaseAndStatus(
                project.getId(), invitedEmail, InvitationStatus.PENDING)) {
            throw new ConflictException("An invitation is already pending for this email");
        }

        ProjectInvitation invitation = new ProjectInvitation();
        invitation.setProject(project);
        invitation.setInvitedEmail(invitedEmail);
        invitation.setInvitedBy(owner);
        invitation.setToken(UUID.randomUUID().toString());
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setExpiresAt(Timestamp.from(Instant.now().plus(EXPIRY_DAYS, ChronoUnit.DAYS)));

        ProjectInvitation saved = invitationRepository.saveAndFlush(invitation);

        emailService.sendProjectInvitationEmail(
                invitedEmail,
                project.getName(),
                owner.getFullName(),
                acceptBaseUrl + "?token=" + saved.getToken()
        );

        return toInvitationResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ProjectInvitationResponse> listInvitationsForProject(String ownerEmail, UUID projectId) {
        Account owner = ownershipValidator.resolveAuthenticatedAccount(ownerEmail);
        Project project = ownershipValidator.requireProjectOwnership(projectId, owner);

        return invitationRepository.findByProjectId(project.getId())
                .stream()
                .map(this::toInvitationResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProjectInvitationResponse> listMyInvitations(String accountEmail) {
        if (!StringUtils.hasText(accountEmail)) {
            throw new InsufficientAuthenticationException("Authenticated user could not be resolved");
        }

        return invitationRepository.findByInvitedEmailIgnoreCase(accountEmail.trim())
                .stream()
                .map(this::toInvitationResponse)
                .toList();
    }

    @Transactional
    public ProjectMemberResponse acceptInvitation(String accountEmail, String token) {
        Account account = ownershipValidator.resolveAuthenticatedAccount(accountEmail);
        ProjectInvitation invitation = findActionablePendingInvitation(token, account);

        ProjectMember membership = new ProjectMember();
        membership.setProject(invitation.getProject());
        membership.setAccount(account);
        membership.setRole(Role.COLLABORATOR);
        membership.setInvitedBy(invitation.getInvitedBy());
        ProjectMember savedMembership = projectMemberRepository.saveAndFlush(membership);

        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitationRepository.save(invitation);

        return toMemberResponse(savedMembership);
    }

    @Transactional
    public void declineInvitation(String accountEmail, String token) {
        Account account = ownershipValidator.resolveAuthenticatedAccount(accountEmail);
        ProjectInvitation invitation = findActionablePendingInvitation(token, account);

        invitation.setStatus(InvitationStatus.DECLINED);
        invitationRepository.save(invitation);
    }

    @Transactional
    public void revokeInvitation(String ownerEmail, UUID projectId, UUID invitationId) {
        Account owner = ownershipValidator.resolveAuthenticatedAccount(ownerEmail);
        Project project = ownershipValidator.requireProjectOwnership(projectId, owner);

        ProjectInvitation invitation = invitationRepository.findById(invitationId)
                .filter(inv -> inv.getProject().getId().equals(project.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new ConflictException("Only pending invitations can be revoked");
        }

        invitation.setStatus(InvitationStatus.REVOKED);
        invitationRepository.save(invitation);
    }

    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> listMembers(String ownerEmail, UUID projectId) {
        Account owner = ownershipValidator.resolveAuthenticatedAccount(ownerEmail);
        Project project = ownershipValidator.requireProjectOwnership(projectId, owner);

        return projectMemberRepository.findByProjectId(project.getId())
                .stream()
                .map(this::toMemberResponse)
                .toList();
    }

    @Transactional
    public void removeMember(String ownerEmail, UUID projectId, UUID memberId) {
        Account owner = ownershipValidator.resolveAuthenticatedAccount(ownerEmail);
        Project project = ownershipValidator.requireProjectOwnership(projectId, owner);

        ProjectMember member = projectMemberRepository.findById(memberId)
                .filter(m -> m.getProject().getId().equals(project.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        if (member.getRole() == Role.OWNER) {
            throw new ConflictException("The project owner cannot be removed");
        }

        projectMemberRepository.delete(member);
    }

    private ProjectInvitation findActionablePendingInvitation(String token, Account account) {
        ProjectInvitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));

        if (!invitation.getInvitedEmail().equalsIgnoreCase(account.getUserName())) {
            throw new AccessDeniedException("This invitation was not sent to your account");
        }

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new ConflictException("This invitation is no longer pending");
        }

        if (invitation.getExpiresAt().toInstant().isBefore(Instant.now())) {
            throw new ConflictException("This invitation has expired");
        }

        return invitation;
    }

    private ProjectInvitationResponse toInvitationResponse(ProjectInvitation invitation) {
        return ProjectInvitationResponse.builder()
                .id(invitation.getId())
                .projectId(invitation.getProject().getId())
                .projectName(invitation.getProject().getName())
                .invitedEmail(invitation.getInvitedEmail())
                .status(invitation.getStatus())
                .invitedById(invitation.getInvitedBy().getId())
                .invitedByName(invitation.getInvitedBy().getFullName())
                .createdAt(invitation.getCreatedAt())
                .expiresAt(invitation.getExpiresAt())
                .build();
    }

    private ProjectMemberResponse toMemberResponse(ProjectMember member) {
        return ProjectMemberResponse.builder()
                .id(member.getId())
                .projectId(member.getProject().getId())
                .projectName(member.getProject().getName())
                .accountId(member.getAccount().getId())
                .accountName(member.getAccount().getFullName())
                .accountEmail(member.getAccount().getUserName())
                .role(member.getRole())
                .createdAt(member.getCreatedAt())
                .build();
    }
}