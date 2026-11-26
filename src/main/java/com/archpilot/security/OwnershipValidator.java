package com.archpilot.security;

import com.archpilot.exception.ForbiddenException;
import com.archpilot.exception.ResourceNotFoundException;
import com.archpilot.model.Project;
import com.archpilot.model.User;
import com.archpilot.repository.ProjectRepository;
import com.archpilot.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class OwnershipValidator {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public OwnershipValidator(ProjectRepository projectRepository, UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    public Project getOwnedProject(Long projectId) {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        if (!project.getOwnerId().equals(user.getId())) {
            throw new ForbiddenException("You do not own this project");
        }

        return project;
    }
}