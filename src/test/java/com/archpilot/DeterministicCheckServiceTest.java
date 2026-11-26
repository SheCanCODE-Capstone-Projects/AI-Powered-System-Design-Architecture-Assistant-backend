package com.archpilot;

import com.archpilot.model.Component;
import com.archpilot.model.Connection;
import com.archpilot.model.ReviewFinding;
import com.archpilot.repository.ComponentRepository;
import com.archpilot.repository.ConnectionRepository;
import com.archpilot.service.DeterministicCheckService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DeterministicCheckServiceTest {

    private Component makeComponent(Long id, String name, String type) {
        Component component = new Component();
        component.setId(id);
        component.setName(name);
        component.setType(type);
        return component;
    }

    @Test
    void flagsFrontendConnectedDirectlyToDatabase() {
        ComponentRepository componentRepository = mock(ComponentRepository.class);
        ConnectionRepository connectionRepository = mock(ConnectionRepository.class);

        Connection connection = new Connection();
        connection.setSourceComponentId(1L);
        connection.setTargetComponentId(2L);

        when(componentRepository.findByProjectId(1L)).thenReturn(List.of(
                makeComponent(1L, "Web", "FRONTEND"),
                makeComponent(2L, "DB", "DATABASE")));
        when(connectionRepository.findByProjectId(1L)).thenReturn(List.of(connection));

        DeterministicCheckService service =
                new DeterministicCheckService(componentRepository, connectionRepository);

        List<ReviewFinding> findings = service.check(1L);

        assertEquals(1, findings.size());
        assertEquals("HIGH", findings.get(0).getSeverity());
    }

    @Test
    void flagsIsolatedComponent() {
        ComponentRepository componentRepository = mock(ComponentRepository.class);
        ConnectionRepository connectionRepository = mock(ConnectionRepository.class);

        when(componentRepository.findByProjectId(1L)).thenReturn(List.of(
                makeComponent(1L, "Cache", "CACHE")));
        when(connectionRepository.findByProjectId(1L)).thenReturn(List.of());

        DeterministicCheckService service =
                new DeterministicCheckService(componentRepository, connectionRepository);

        List<ReviewFinding> findings = service.check(1L);

        assertEquals(1, findings.size());
        assertEquals("Isolated component", findings.get(0).getIssue());
    }

    @Test
    void flagsBackendWithoutDatabase() {
        ComponentRepository componentRepository = mock(ComponentRepository.class);
        ConnectionRepository connectionRepository = mock(ConnectionRepository.class);

        Connection connection = new Connection();
        connection.setSourceComponentId(1L);
        connection.setTargetComponentId(2L);

        when(componentRepository.findByProjectId(1L)).thenReturn(List.of(
                makeComponent(1L, "Web", "FRONTEND"),
                makeComponent(2L, "API", "BACKEND")));
        when(connectionRepository.findByProjectId(1L)).thenReturn(List.of(connection));

        DeterministicCheckService service =
                new DeterministicCheckService(componentRepository, connectionRepository);

        List<ReviewFinding> findings = service.check(1L);

        assertTrue(findings.stream().anyMatch(f -> f.getIssue().equals("No database found")));
    }
}