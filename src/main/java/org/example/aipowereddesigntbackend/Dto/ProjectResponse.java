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
public class ProjectResponse {

    private UUID id;
    private String name;
    private String description;
    private String requirements;
    private UUID ownerId;
    private Role role;
    private Timestamp createdAt;
    private Timestamp updatedAt;
}