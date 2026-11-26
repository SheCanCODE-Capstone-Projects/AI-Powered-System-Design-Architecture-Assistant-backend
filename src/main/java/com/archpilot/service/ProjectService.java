package com.archpilot.service;

import com.archpilot.dto.ProjectDto;
import com.archpilot.exception.ResourceNotFoundException;
import com.archpilot.model.Project;
import com.archpilot.model.User;
import com.archpilot.repository.ProjectRepository;
import com.archpilot.repository.UserRepository;
import com.archpilot.security.OwnershipValidator;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final OwnershipValidator ownershipValidator;

    public ProjectService(ProjectRepository projectRepository,
                          UserRepository userRepository,
                          OwnershipValidator ownershipValidator) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.ownershipValidator = ownershipValidator;
    }

    @Transactional
    public ProjectDto createProject(ProjectDto dto) {
        Project project = new Project();
        project.setOwnerId(currentUser().getId());
        project.setName(dto.getName());
        project.setDescription(dto.getDescription());
        project.setRequirements(dto.getRequirements());
        return toDto(projectRepository.save(project));
    }

    public List<ProjectDto> getMyProjects() {
        return projectRepository.findByOwnerId(currentUser().getId())
                .stream()
                .map(this::toDto)
                .toList();
    }

    public ProjectDto getProject(Long id) {
        return toDto(ownershipValidator.getOwnedProject(id));
    }

    @Transactional
    public ProjectDto updateProject(Long id, ProjectDto dto) {
        Project project = ownershipValidator.getOwnedProject(id);
        project.setName(dto.getName());
        project.setDescription(dto.getDescription());
        project.setRequirements(dto.getRequirements());
        return toDto(projectRepository.save(project));
    }

    @Transactional
    public void deleteProject(Long id) {
        Project project = ownershipValidator.getOwnedProject(id);
        projectRepository.delete(project);
    }

    private User currentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private ProjectDto toDto(Project project) {
        ProjectDto dto = new ProjectDto();
        dto.setId(project.getId());
        dto.setName(project.getName());
        dto.setDescription(project.getDescription());
        dto.setRequirements(project.getRequirements());
        dto.setCreatedAt(project.getCreatedAt());
        dto.setUpdatedAt(project.getUpdatedAt());
        return dto;
    }
}