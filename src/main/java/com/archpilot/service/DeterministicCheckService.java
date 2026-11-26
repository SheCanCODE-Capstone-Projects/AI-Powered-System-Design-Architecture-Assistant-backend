package com.archpilot.service;

import com.archpilot.model.Component;
import com.archpilot.model.Connection;
import com.archpilot.model.ReviewFinding;
import com.archpilot.repository.ComponentRepository;
import com.archpilot.repository.ConnectionRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DeterministicCheckService {

    private final ComponentRepository componentRepository;
    private final ConnectionRepository connectionRepository;

    public DeterministicCheckService(ComponentRepository componentRepository,
                                     ConnectionRepository connectionRepository) {
        this.componentRepository = componentRepository;
        this.connectionRepository = connectionRepository;
    }

    public List<ReviewFinding> check(Long projectId) {
        List<Component> components = componentRepository.findByProjectId(projectId);
        List<Connection> connections = connectionRepository.findByProjectId(projectId);
        List<ReviewFinding> findings = new ArrayList<>();

        if (components.isEmpty()) {
            return findings;
        }

        boolean hasBackend = false;
        boolean hasDatabase = false;

        for (Component component : components) {
            if ("BACKEND".equalsIgnoreCase(component.getType())) {
                hasBackend = true;
            }
            if ("DATABASE".equalsIgnoreCase(component.getType())) {
                hasDatabase = true;
            }
            if (!isConnected(component, connections)) {
                findings.add(createFinding(projectId, component.getName(), "MEDIUM",
                        "Isolated component",
                        "This component has no connections, so it does not take part in the system.",
                        "Connect it to the components that use it, or remove it."));
            }
        }

        if (hasBackend && !hasDatabase) {
            findings.add(createFinding(projectId, "System", "MEDIUM",
                    "No database found",
                    "The system has a backend but no place to store data permanently.",
                    "Add a database component and connect the backend to it."));
        }

        for (Connection connection : connections) {
            Component source = findById(components, connection.getSourceComponentId());
            Component target = findById(components, connection.getTargetComponentId());
            if (source != null && target != null
                    && "FRONTEND".equalsIgnoreCase(source.getType())
                    && "DATABASE".equalsIgnoreCase(target.getType())) {
                findings.add(createFinding(projectId, target.getName(), "HIGH",
                        "Frontend connects directly to the database",
                        "The frontend runs on the user's device, so a direct database connection exposes the data.",
                        "Place a backend API between the frontend and the database."));
            }
        }

        return findings;
    }

    private boolean isConnected(Component component, List<Connection> connections) {
        for (Connection connection : connections) {
            if (connection.getSourceComponentId().equals(component.getId())
                    || connection.getTargetComponentId().equals(component.getId())) {
                return true;
            }
        }
        return false;
    }

    private Component findById(List<Component> components, Long id) {
        for (Component component : components) {
            if (component.getId().equals(id)) {
                return component;
            }
        }
        return null;
    }

    private ReviewFinding createFinding(Long projectId, String componentName, String severity,
                                        String issue, String explanation, String recommendation) {
        ReviewFinding finding = new ReviewFinding();
        finding.setProjectId(projectId);
        finding.setComponentName(componentName);
        finding.setSeverity(severity);
        finding.setIssue(issue);
        finding.setExplanation(explanation);
        finding.setRecommendation(recommendation);
        return finding;
    }
}