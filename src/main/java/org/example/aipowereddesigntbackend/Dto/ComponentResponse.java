package org.example.aipowereddesigntbackend.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.aipowereddesigntbackend.Enum.ComponentType;

import java.sql.Timestamp;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComponentResponse {

    private UUID id;
    private UUID projectId;
    private String name;
    private ComponentType type;
    private String description;
    private Timestamp createdAt;
    private Timestamp updatedAt;
}