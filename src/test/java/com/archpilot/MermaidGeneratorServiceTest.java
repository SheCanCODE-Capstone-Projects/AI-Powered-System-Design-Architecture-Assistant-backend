package com.archpilot;

import com.archpilot.dto.DiagramResponse;
import com.archpilot.model.Component;
import com.archpilot.model.Connection;
import com.archpilot.repository.ComponentRepository;
import com.archpilot.repository.ConnectionRepository;
import com.archpilot.security.OwnershipValidator;
import com.archpilot.service.MermaidGeneratorService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MermaidGeneratorServiceTest {

    @Test
    void generatesDiagramWithDatabaseShapeAndLabel() {
        ComponentRepository componentRepository = mock(ComponentRepository.class);
        ConnectionRepository connectionRepository = mock(ConnectionRepository.class);
        OwnershipValidator ownershipValidator = mock(OwnershipValidator.class);

        Component frontend = new Component();
        frontend.setId(1L);
        frontend.setName("Web Frontend");
        frontend.setType("FRONTEND");

        Component database = new Component();
        database.setId(2L);
        database.setName("PostgreSQL DB");
        database.setType("DATABASE");

        Connection connection = new Connection();
        connection.setSourceComponentId(1L);
        connection.setTargetComponentId(2L);
        connection.setLabel("HTTP");

        when(componentRepository.findByProjectId(1L)).thenReturn(List.of(frontend, database));
        when(connectionRepository.findByProjectId(1L)).thenReturn(List.of(connection));

        MermaidGeneratorService service =
                new MermaidGeneratorService(componentRepository, connectionRepository, ownershipValidator);

        DiagramResponse response = service.generate(1L);

        String expected = "graph TD\n"
                + "  C1[\"Web Frontend\"]\n"
                + "  C2[(\"PostgreSQL DB\")]\n"
                + "  C1 -->|HTTP| C2";

        assertEquals(1L, response.getProjectId());
        assertEquals(expected, response.getMermaidSyntax());
    }

    @Test
    void generatesOnlyHeaderWhenProjectIsEmpty() {
        ComponentRepository componentRepository = mock(ComponentRepository.class);
        ConnectionRepository connectionRepository = mock(ConnectionRepository.class);
        OwnershipValidator ownershipValidator = mock(OwnershipValidator.class);

        when(componentRepository.findByProjectId(5L)).thenReturn(List.of());
        when(connectionRepository.findByProjectId(5L)).thenReturn(List.of());

        MermaidGeneratorService service =
                new MermaidGeneratorService(componentRepository, connectionRepository, ownershipValidator);

        assertEquals("graph TD", service.generate(5L).getMermaidSyntax());
    }
}