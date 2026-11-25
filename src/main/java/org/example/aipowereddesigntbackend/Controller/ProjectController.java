package org.example.aipowereddesigntbackend.Controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.example.aipowereddesigntbackend.Dto.ComponentRequest;
import org.example.aipowereddesigntbackend.Dto.ComponentResponse;
import org.example.aipowereddesigntbackend.Dto.CreateProjectRequest;
import org.example.aipowereddesigntbackend.Dto.InviteCollaboratorRequest;
import org.example.aipowereddesigntbackend.Dto.ProjectDetailResponse;
import org.example.aipowereddesigntbackend.Dto.ProjectInvitationResponse;
import org.example.aipowereddesigntbackend.Dto.ProjectMemberResponse;
import org.example.aipowereddesigntbackend.Dto.ProjectResponse;
import org.example.aipowereddesigntbackend.Service.ComponentService;
import org.example.aipowereddesigntbackend.Service.InvitationService;
import org.example.aipowereddesigntbackend.Service.ProjectService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Every endpoint rooted at {@code /api/projects}: project creation and listing,
 * collaborator invitations, project membership, and project-scoped components.
 * All operations require authentication (Google or GitHub login) and, except
 * for project creation/listing, require the caller to be a member (owner or
 * accepted collaborator) of the target project. Invitation and membership
 * management additionally require the caller to be the project OWNER.
 */
@RestController
@RequestMapping("/api/projects")
@Tag(
        name = "Projects",
        description = "Create and list system architecture projects, invite and manage collaborators, "
                + "view/remove project members, and add/list the project's architectural components. "
                + "Everything here is scoped to a single project resource; component update/delete and "
                + "invitation accept/decline live under /api/components and /api/invitations respectively "
                + "since those actions are addressed by their own resource ID or invitation token, not a "
                + "project ID."
)
public class ProjectController {

    private final ProjectService projectService;
    private final InvitationService invitationService;
    private final ComponentService componentService;

    public ProjectController(
            ProjectService projectService,
            InvitationService invitationService,
            ComponentService componentService
    ) {
        this.projectService = projectService;
        this.invitationService = invitationService;
        this.componentService = componentService;
    }

    // ---------------------------------------------------------------------
    // Projects
    // ---------------------------------------------------------------------

    @PostMapping
    @Operation(
            summary = "Create a project",
            description = "Creates a new system architecture project. The caller becomes the project's "
                    + "OWNER automatically - there is no way to set the owner or role from the request body. "
                    + "Call this first; every other endpoint in this controller operates on the project ID "
                    + "returned here."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Project successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid project information"),
            @ApiResponse(responseCode = "401", description = "User is not authenticated")
    })
    public ResponseEntity<ProjectResponse> createProject(
            @AuthenticationPrincipal OAuth2User principal,
            @Valid @RequestBody CreateProjectRequest request
    ) {
        String username = principal == null ? null : principal.getAttribute("email");
        ProjectResponse response = projectService.createProject(username, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(
            summary = "List my projects",
            description = "Returns every project the authenticated user owns or collaborates on, "
                    + "each annotated with the caller's role (OWNER or COLLABORATOR) on that project."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Projects successfully retrieved"),
            @ApiResponse(responseCode = "401", description = "User is not authenticated")
    })
    public ResponseEntity<List<ProjectResponse>> listMyProjects(@AuthenticationPrincipal OAuth2User principal) {
        String username = principal == null ? null : principal.getAttribute("email");
        return ResponseEntity.ok(projectService.listMyProjects(username));
    }

    @GetMapping("/{projectId}")
    @Operation(
            summary = "Get a project",
            description = "Any project member (owner or collaborator) may call this. Returns the project's "
                    + "details together with its components. Connections will be added here once that "
                    + "feature exists."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Project successfully retrieved"),
            @ApiResponse(responseCode = "403", description = "User is not a member of the project"),
            @ApiResponse(responseCode = "404", description = "Project not found")
    })
    public ResponseEntity<ProjectDetailResponse> getProject(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable UUID projectId
    ) {
        String username = principal == null ? null : principal.getAttribute("email");
        return ResponseEntity.ok(projectService.getProject(username, projectId));
    }

    @PutMapping("/{projectId}")
    @Operation(
            summary = "Update a project",
            description = "Any project member (owner or collaborator) may call this. Replaces the project's "
                    + "name, description, and requirements with the given values. Uses the same request body "
                    + "shape as creation; ownership itself can never be changed through this endpoint."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Project successfully updated"),
            @ApiResponse(responseCode = "400", description = "Invalid project information"),
            @ApiResponse(responseCode = "403", description = "User is not a member of the project"),
            @ApiResponse(responseCode = "404", description = "Project not found")
    })
    public ResponseEntity<ProjectResponse> updateProject(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateProjectRequest request
    ) {
        String username = principal == null ? null : principal.getAttribute("email");
        return ResponseEntity.ok(projectService.updateProject(username, projectId, request));
    }

    @DeleteMapping("/{projectId}")
    @Operation(
            summary = "Delete a project",
            description = "Owner-only. Permanently deletes the project along with its architecture data "
                    + "(components) and collaboration data (memberships and invitations). This cannot be "
                    + "undone - there is no soft-delete or recovery."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Project and all associated data deleted"),
            @ApiResponse(responseCode = "403", description = "Only the project owner can delete the project"),
            @ApiResponse(responseCode = "404", description = "Project not found")
    })
    public ResponseEntity<Void> deleteProject(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable UUID projectId
    ) {
        String username = principal == null ? null : principal.getAttribute("email");
        projectService.deleteProject(username, projectId);
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------------
    // Collaborator invitations - owner only
    // ---------------------------------------------------------------------

    @PostMapping("/{projectId}/invitations")
    @Operation(
            summary = "Invite a collaborator",
            description = "Owner-only. Emails a time-limited invitation link to the given address. The "
                    + "invitee does not need an existing account - they log in with Google or GitHub using "
                    + "that same email, then call POST /api/invitations/{token}/accept with the token from "
                    + "the email to become a COLLABORATOR."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Invitation created and emailed"),
            @ApiResponse(responseCode = "400", description = "Invalid invitation information"),
            @ApiResponse(responseCode = "403", description = "Only the project owner can invite collaborators"),
            @ApiResponse(responseCode = "404", description = "Project not found"),
            @ApiResponse(responseCode = "409", description = "Invitation already pending or user already a member")
    })
    public ResponseEntity<ProjectInvitationResponse> inviteCollaborator(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable UUID projectId,
            @Valid @RequestBody InviteCollaboratorRequest request
    ) {
        String username = principal == null ? null : principal.getAttribute("email");
        ProjectInvitationResponse response =
                invitationService.inviteCollaborator(username, projectId, request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{projectId}/invitations")
    @Operation(
            summary = "List project invitations",
            description = "Owner-only. Returns every invitation sent for this project, in any status "
                    + "(PENDING, ACCEPTED, DECLINED, REVOKED), so the owner can see who has responded."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Invitations successfully retrieved"),
            @ApiResponse(responseCode = "403", description = "Only the project owner can view invitations"),
            @ApiResponse(responseCode = "404", description = "Project not found")
    })
    public ResponseEntity<List<ProjectInvitationResponse>> listInvitations(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable UUID projectId
    ) {
        String username = principal == null ? null : principal.getAttribute("email");
        return ResponseEntity.ok(invitationService.listInvitationsForProject(username, projectId));
    }

    @DeleteMapping("/{projectId}/invitations/{invitationId}")
    @Operation(
            summary = "Revoke a pending invitation",
            description = "Owner-only. Cancels a PENDING invitation so its link can no longer be accepted. "
                    + "Has no effect on invitations that were already accepted, declined, or revoked - those "
                    + "requests fail instead so you can tell the revoke didn't apply."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Invitation revoked"),
            @ApiResponse(responseCode = "403", description = "Only the project owner can revoke invitations"),
            @ApiResponse(responseCode = "404", description = "Project or invitation not found"),
            @ApiResponse(responseCode = "409", description = "Invitation is not pending")
    })
    public ResponseEntity<Void> revokeInvitation(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable UUID projectId,
            @PathVariable UUID invitationId
    ) {
        String username = principal == null ? null : principal.getAttribute("email");
        invitationService.revokeInvitation(username, projectId, invitationId);
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------------
    // Members - owner only
    // ---------------------------------------------------------------------

    @GetMapping("/{projectId}/members")
    @Operation(
            summary = "List project members",
            description = "Owner-only. Returns the owner and every accepted collaborator, with each "
                    + "member's role and membership ID - use the membership ID (not the account ID) to "
                    + "remove someone with the endpoint below."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Members successfully retrieved"),
            @ApiResponse(responseCode = "403", description = "Only the project owner can view members"),
            @ApiResponse(responseCode = "404", description = "Project not found")
    })
    public ResponseEntity<List<ProjectMemberResponse>> listMembers(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable UUID projectId
    ) {
        String username = principal == null ? null : principal.getAttribute("email");
        return ResponseEntity.ok(invitationService.listMembers(username, projectId));
    }

    @DeleteMapping("/{projectId}/members/{memberId}")
    @Operation(
            summary = "Remove a collaborator",
            description = "Owner-only. Revokes a collaborator's access immediately. The project owner's own "
                    + "membership can never be removed through this endpoint - project deletion/ownership "
                    + "transfer (if ever added) would be a separate operation."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Collaborator removed"),
            @ApiResponse(responseCode = "403", description = "Only the project owner can remove collaborators"),
            @ApiResponse(responseCode = "404", description = "Project or member not found"),
            @ApiResponse(responseCode = "409", description = "The project owner cannot be removed")
    })
    public ResponseEntity<Void> removeMember(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable UUID projectId,
            @PathVariable UUID memberId
    ) {
        String username = principal == null ? null : principal.getAttribute("email");
        invitationService.removeMember(username, projectId, memberId);
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------------
    // Components - any project member
    // ---------------------------------------------------------------------

    @PostMapping("/{projectId}/components")
    @Operation(
            summary = "Add a component",
            description = "Any project member (owner or collaborator) may call this. Adds an architectural "
                    + "component (FRONTEND, BACKEND, DATABASE, CACHE, EXTERNAL_API, or OTHER) to the project. "
                    + "To change or remove a component afterwards, use PUT/DELETE /api/components/{id} with "
                    + "the ID returned here."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Component successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid component information"),
            @ApiResponse(responseCode = "403", description = "User is not a member of the project"),
            @ApiResponse(responseCode = "404", description = "Project not found")
    })
    public ResponseEntity<ComponentResponse> addComponent(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable UUID projectId,
            @Valid @RequestBody ComponentRequest request
    ) {
        String username = principal == null ? null : principal.getAttribute("email");
        ComponentResponse response = componentService.addComponent(username, projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{projectId}/components")
    @Operation(
            summary = "List project components",
            description = "Any project member (owner or collaborator) may call this. Returns every "
                    + "architectural component currently defined on the project."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Components successfully retrieved"),
            @ApiResponse(responseCode = "403", description = "User is not a member of the project"),
            @ApiResponse(responseCode = "404", description = "Project not found")
    })
    public ResponseEntity<List<ComponentResponse>> listComponents(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable UUID projectId
    ) {
        String username = principal == null ? null : principal.getAttribute("email");
        return ResponseEntity.ok(componentService.listComponents(username, projectId));
    }
}
