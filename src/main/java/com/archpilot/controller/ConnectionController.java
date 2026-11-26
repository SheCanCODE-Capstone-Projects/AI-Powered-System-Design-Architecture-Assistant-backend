package com.archpilot.controller;

import com.archpilot.dto.ConnectionDto;
import com.archpilot.service.ConnectionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ConnectionController {

    private final ConnectionService connectionService;

    public ConnectionController(ConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    @PostMapping("/projects/{projectId}/connections")
    public ResponseEntity<ConnectionDto> create(@PathVariable Long projectId,
                                                @Valid @RequestBody ConnectionDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(connectionService.create(projectId, dto));
    }

    @DeleteMapping("/connections/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        connectionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}