package org.example.aipowereddesigntbackend.Controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.example.aipowereddesigntbackend.Dto.ComponentRequest;
import org.example.aipowereddesigntbackend.Dto.ComponentResponse;
import org.example.aipowereddesigntbackend.Service.ComponentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Endpoints addressed directly by a component ID rather than a project ID.
 * Creating and listing components is done under /api/projects/{projectId}/components
 * (see {@link ProjectController}), since those operations need a project to
 * attach to; updating and deleting a component only need the component's own ID.
 */
@RestController
@RequestMapping("/api/components")
@Tag(
        name = "Components",
        description = "Update or delete an existing architectural component by its own ID. To create a "
                + "component or list a project's components, see POST/GET /api/projects/{projectId}/components."
)
public class ComponentController {

    private final ComponentService componentService;

    public ComponentController(ComponentService componentService) {
        this.componentService = componentService;
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update a component",
            description = "Any project member (owner or collaborator) may call this. Replaces the "
                    + "component's name, type, and description with the given values."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Component successfully updated"),
            @ApiResponse(responseCode = "400", description = "Invalid component information"),
            @ApiResponse(responseCode = "403", description = "User is not a member of the component's project"),
            @ApiResponse(responseCode = "404", description = "Component not found")
    })
    public ResponseEntity<ComponentResponse> updateComponent(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable UUID id,
            @Valid @RequestBody ComponentRequest request
    ) {
        String username = principal == null ? null : principal.getAttribute("email");
        return ResponseEntity.ok(componentService.updateComponent(username, id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete a component",
            description = "Any project member (owner or collaborator) may call this. Permanently removes "
                    + "the component from its project."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Component successfully deleted"),
            @ApiResponse(responseCode = "403", description = "User is not a member of the component's project"),
            @ApiResponse(responseCode = "404", description = "Component not found")
    })
    public ResponseEntity<Void> deleteComponent(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable UUID id
    ) {
        String username = principal == null ? null : principal.getAttribute("email");
        componentService.deleteComponent(username, id);
        return ResponseEntity.noContent().build();
    }
}
