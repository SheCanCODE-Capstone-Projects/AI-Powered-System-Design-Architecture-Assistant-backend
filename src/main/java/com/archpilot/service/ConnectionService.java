package com.archpilot.service;

import com.archpilot.dto.ConnectionDto;
import com.archpilot.exception.ResourceNotFoundException;
import com.archpilot.model.Component;
import com.archpilot.model.Connection;
import com.archpilot.repository.ComponentRepository;
import com.archpilot.repository.ConnectionRepository;
import com.archpilot.security.OwnershipValidator;
import org.springframework.stereotype.Service;

@Service
public class ConnectionService {

    private final ConnectionRepository connectionRepository;
    private final ComponentRepository componentRepository;
    private final OwnershipValidator ownershipValidator;

    public ConnectionService(ConnectionRepository connectionRepository,
                             ComponentRepository componentRepository,
                             OwnershipValidator ownershipValidator) {
        this.connectionRepository = connectionRepository;
        this.componentRepository = componentRepository;
        this.ownershipValidator = ownershipValidator;
    }

    public ConnectionDto create(Long projectId, ConnectionDto dto) {
        ownershipValidator.getOwnedProject(projectId);

        if (dto.getSourceComponentId().equals(dto.getTargetComponentId())) {
            throw new IllegalArgumentException("A component cannot connect to itself");
        }

        Component source = findComponent(dto.getSourceComponentId());
        Component target = findComponent(dto.getTargetComponentId());

        if (!source.getProjectId().equals(projectId) || !target.getProjectId().equals(projectId)) {
            throw new IllegalArgumentException("Both components must belong to this project");
        }

        Connection connection = new Connection();
        connection.setProjectId(projectId);
        connection.setSourceComponentId(source.getId());
        connection.setTargetComponentId(target.getId());
        connection.setLabel(dto.getLabel());
        return toDto(connectionRepository.save(connection));
    }

    public void delete(Long id) {
        Connection connection = connectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Connection not found"));
        ownershipValidator.getOwnedProject(connection.getProjectId());
        connectionRepository.delete(connection);
    }

    private Component findComponent(Long id) {
        return componentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Component not found: " + id));
    }

    private ConnectionDto toDto(Connection connection) {
        ConnectionDto dto = new ConnectionDto();
        dto.setId(connection.getId());
        dto.setSourceComponentId(connection.getSourceComponentId());
        dto.setTargetComponentId(connection.getTargetComponentId());
        dto.setLabel(connection.getLabel());
        return dto;
    }
}