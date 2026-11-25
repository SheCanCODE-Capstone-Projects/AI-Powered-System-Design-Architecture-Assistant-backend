package org.example.aipowereddesigntbackend.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.aipowereddesigntbackend.Enum.Role;

import java.sql.Timestamp;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectMemberResponse {

    private UUID id;
    private UUID projectId;
    private String projectName;
    private UUID accountId;
    private String accountName;
    private String accountEmail;
    private Role role;
    private Timestamp createdAt;
}