package com.archpilot.dto;

public class DiagramResponse {

    private Long projectId;
    private String mermaidSyntax;

    public DiagramResponse(Long projectId, String mermaidSyntax) {
        this.projectId = projectId;
        this.mermaidSyntax = mermaidSyntax;
    }

    public Long getProjectId() { return projectId; }

    public String getMermaidSyntax() { return mermaidSyntax; }
}