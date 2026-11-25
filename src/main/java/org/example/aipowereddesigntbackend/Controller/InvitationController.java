package org.example.aipowereddesigntbackend.Controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.aipowereddesigntbackend.Dto.ProjectInvitationResponse;
import org.example.aipowereddesigntbackend.Dto.ProjectMemberResponse;
import org.example.aipowereddesigntbackend.Service.InvitationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/invitations")
@Tag(name = "Invitations", description = "Endpoints for invited users to view and respond to project invitations")
public class InvitationController {

    private final InvitationService invitationService;

    public InvitationController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @GetMapping("/me")
    @Operation(
            summary = "List my invitations",
            description = "Returns every invitation sent to the authenticated user's email address."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Invitations successfully retrieved"),
            @ApiResponse(responseCode = "401", description = "User is not authenticated")
    })
    public ResponseEntity<List<ProjectInvitationResponse>> myInvitations(
            @AuthenticationPrincipal OAuth2User principal
    ) {
        String username = principal == null ? null : principal.getAttribute("email");
        return ResponseEntity.ok(invitationService.listMyInvitations(username));
    }

    @PostMapping("/{token}/accept")
    @Operation(
            summary = "Accept an invitation",
            description = "Accepts a pending invitation addressed to the authenticated user's email, "
                    + "granting COLLABORATOR access to the project."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Invitation accepted and membership created"),
            @ApiResponse(responseCode = "401", description = "User is not authenticated"),
            @ApiResponse(responseCode = "403", description = "Invitation was not sent to this account"),
            @ApiResponse(responseCode = "404", description = "Invitation not found"),
            @ApiResponse(responseCode = "409", description = "Invitation is no longer pending or has expired")
    })
    public ResponseEntity<ProjectMemberResponse> accept(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable String token
    ) {
        String username = principal == null ? null : principal.getAttribute("email");
        return ResponseEntity.ok(invitationService.acceptInvitation(username, token));
    }

    @PostMapping("/{token}/decline")
    @Operation(
            summary = "Decline an invitation",
            description = "Declines a pending invitation addressed to the authenticated user's email."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Invitation declined"),
            @ApiResponse(responseCode = "401", description = "User is not authenticated"),
            @ApiResponse(responseCode = "403", description = "Invitation was not sent to this account"),
            @ApiResponse(responseCode = "404", description = "Invitation not found"),
            @ApiResponse(responseCode = "409", description = "Invitation is no longer pending or has expired")
    })
    public ResponseEntity<Void> decline(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable String token
    ) {
        String username = principal == null ? null : principal.getAttribute("email");
        invitationService.declineInvitation(username, token);
        return ResponseEntity.noContent().build();
    }
}