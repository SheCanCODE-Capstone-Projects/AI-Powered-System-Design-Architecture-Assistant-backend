package com.archpilot.controller;

import com.archpilot.dto.DiagramResponse;
import com.archpilot.service.MermaidGeneratorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
public class DiagramController {

    private final MermaidGeneratorService mermaidGeneratorService;

    public DiagramController(MermaidGeneratorService mermaidGeneratorService) {
        this.mermaidGeneratorService = mermaidGeneratorService;
    }

    @GetMapping("/{projectId}/diagram")
    public DiagramResponse getDiagram(@PathVariable Long projectId) {
        return mermaidGeneratorService.generate(projectId);
    }
}