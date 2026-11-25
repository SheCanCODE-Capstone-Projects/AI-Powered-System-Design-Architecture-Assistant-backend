package org.example.aipowereddesigntbackend.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.aipowereddesigntbackend.Enum.InvitationStatus;

import java.sql.Timestamp;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectInvitationResponse {

    private UUID id;
    private UUID projectId;
    private String projectName;
    private String invitedEmail;
    private InvitationStatus status;
    private UUID invitedById;
    private String invitedByName;
    private Timestamp createdAt;
    private Timestamp expiresAt;
}