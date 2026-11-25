package org.example.aipowereddesigntbackend.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.aipowereddesigntbackend.Enum.ComponentType;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComponentRequest {

    @NotBlank(message = "Component name is required")
    @Size(max = 150, message = "Component name must not exceed 150 characters")
    private String name;

    @NotNull(message = "Component type is required")
    private ComponentType type;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;
}