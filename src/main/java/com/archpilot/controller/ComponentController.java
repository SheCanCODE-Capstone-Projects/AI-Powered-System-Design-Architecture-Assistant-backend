
package com.archpilot.controller;

import com.archpilot.dto.ComponentDto;
import com.archpilot.service.ComponentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ComponentController {

    private final ComponentService componentService;

    public ComponentController(ComponentService componentService) {
        this.componentService = componentService;
    }

    @PostMapping("/projects/{projectId}/components")
    public ResponseEntity<ComponentDto> create(@PathVariable Long projectId,
                                               @Valid @RequestBody ComponentDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(componentService.create(projectId, dto));
    }

    @PutMapping("/components/{id}")
    public ComponentDto update(@PathVariable Long id,
                               @Valid @RequestBody ComponentDto dto) {
        return componentService.update(id, dto);
    }

    @DeleteMapping("/components/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        componentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}