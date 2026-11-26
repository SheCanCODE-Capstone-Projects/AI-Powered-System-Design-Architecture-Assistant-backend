package com.archpilot.service;

import com.archpilot.dto.DiagramResponse;
import com.archpilot.model.Component;
import com.archpilot.model.Connection;
import com.archpilot.repository.ComponentRepository;
import com.archpilot.repository.ConnectionRepository;
import com.archpilot.security.OwnershipValidator;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MermaidGeneratorService {

    private final ComponentRepository componentRepository;
    private final ConnectionRepository connectionRepository;
    private final OwnershipValidator ownershipValidator;

    public MermaidGeneratorService(ComponentRepository componentRepository,
                                   ConnectionRepository connectionRepository,
                                   OwnershipValidator ownershipValidator) {
        this.componentRepository = componentRepository;
        this.connectionRepository = connectionRepository;
        this.ownershipValidator = ownershipValidator;
    }

    public DiagramResponse generate(Long projectId) {
        ownershipValidator.getOwnedProject(projectId);

        List<Component> components = componentRepository.findByProjectId(projectId);
        List<Connection> connections = connectionRepository.findByProjectId(projectId);

        StringBuilder text = new StringBuilder("graph TD");

        for (Component component : components) {
            text.append("\n  ").append(nodeText(component));
        }

        for (Connection connection : connections) {
            text.append("\n  C").append(connection.getSourceComponentId());
            if (connection.getLabel() != null && !connection.getLabel().isBlank()) {
                text.append(" -->|").append(clean(connection.getLabel())).append("| ");
            } else {
                text.append(" --> ");
            }
            text.append("C").append(connection.getTargetComponentId());
        }

        return new DiagramResponse(projectId, text.toString());
    }

    private String nodeText(Component component) {
        String id = "C" + component.getId();
        String name = "\"" + clean(component.getName()) + "\"";

        if ("DATABASE".equalsIgnoreCase(component.getType())) {
            return id + "[(" + name + ")]";
        }
        return id + "[" + name + "]";
    }

    private String clean(String text) {
        return text.replace("\"", "'").replace("|", "/");
    }
}