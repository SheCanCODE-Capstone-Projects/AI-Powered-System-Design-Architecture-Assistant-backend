package com.archpilot.service;

import com.archpilot.dto.ComponentDto;
import com.archpilot.exception.ResourceNotFoundException;
import com.archpilot.model.Component;
import com.archpilot.repository.ComponentRepository;
import com.archpilot.security.OwnershipValidator;
import org.springframework.stereotype.Service;

@Service
public class ComponentService {

    private final ComponentRepository componentRepository;
    private final OwnershipValidator ownershipValidator;

    public ComponentService(ComponentRepository componentRepository,
                            OwnershipValidator ownershipValidator) {
        this.componentRepository = componentRepository;
        this.ownershipValidator = ownershipValidator;
    }

    public ComponentDto create(Long projectId, ComponentDto dto) {
        ownershipValidator.getOwnedProject(projectId);
        Component component = new Component();
        component.setProjectId(projectId);
        component.setName(dto.getName());
        component.setType(dto.getType());
        component.setDescription(dto.getDescription());
        return toDto(componentRepository.save(component));
    }

    public ComponentDto update(Long id, ComponentDto dto) {
        Component component = findOwnedComponent(id);
        component.setName(dto.getName());
        component.setType(dto.getType());
        component.setDescription(dto.getDescription());
        return toDto(componentRepository.save(component));
    }

    public void delete(Long id) {
        Component component = findOwnedComponent(id);
        componentRepository.delete(component);
    }

    private Component findOwnedComponent(Long id) {
        Component component = componentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Component not found"));
        ownershipValidator.getOwnedProject(component.getProjectId());
        return component;
    }

    private ComponentDto toDto(Component component) {
        ComponentDto dto = new ComponentDto();
        dto.setId(component.getId());
        dto.setName(component.getName());
        dto.setType(component.getType());
        dto.setDescription(component.getDescription());
        return dto;
    }
}