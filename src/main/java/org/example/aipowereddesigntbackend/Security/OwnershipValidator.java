package org.example.aipowereddesigntbackend.Security;

import org.example.aipowereddesigntbackend.ExceptionHandling.ResourceNotFoundException;
import org.example.aipowereddesigntbackend.Model.Account;
import org.example.aipowereddesigntbackend.Model.Project;
import org.example.aipowereddesigntbackend.Repository.AccountRepository;
import org.example.aipowereddesigntbackend.Repository.ProjectMemberRepository;
import org.example.aipowereddesigntbackend.Repository.ProjectRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * Single place that resolves the authenticated Account from the OAuth2
 * principal's email and enforces project-level access. Membership checks
 * (owner or accepted collaborator) gate collaborative actions; ownership
 * checks gate administrative/destructive ones. Used by every service that
 * needs to answer "is this caller allowed to touch this project" instead of
 * each one re-implementing the same lookup.
 */
@Component
public class OwnershipValidator {

    private final AccountRepository accountRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;

    public OwnershipValidator(
            AccountRepository accountRepository,
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository
    ) {
        this.accountRepository = accountRepository;
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
    }

    public Account resolveAuthenticatedAccount(String username) {
        if (!StringUtils.hasText(username)) {
            throw new InsufficientAuthenticationException("Authenticated user could not be resolved");
        }

        Account account = accountRepository.findAccountByUserName(username);
        if (account == null) {
            throw new InsufficientAuthenticationException("Authenticated user could not be resolved");
        }

        return account;
    }

    /**
     * Loads the project and ensures the account is a member (owner or
     * accepted collaborator) of it.
     */
    public Project requireProjectMembership(UUID projectId, Account account) {
        Project project = findProject(projectId);
        requireMembership(project, account);
        return project;
    }

    /**
     * Loads the project and ensures the account is its owner.
     */
    public Project requireProjectOwnership(UUID projectId, Account account) {
        Project project = findProject(projectId);
        requireOwnership(project, account);
        return project;
    }

    public void requireMembership(Project project, Account account) {
        boolean isMember = projectMemberRepository.existsByProjectIdAndAccountId(project.getId(), account.getId());
        if (!isMember) {
            throw new AccessDeniedException("You do not have access to this project");
        }
    }

    public void requireOwnership(Project project, Account account) {
        if (!project.getOwner().getId().equals(account.getId())) {
            throw new AccessDeniedException("Only the project owner can perform this action");
        }
    }

    private Project findProject(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
    }
}