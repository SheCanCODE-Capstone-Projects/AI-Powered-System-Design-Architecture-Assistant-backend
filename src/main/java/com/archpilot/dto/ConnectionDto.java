package com.archpilot.dto;

import jakarta.validation.constraints.NotNull;

public class ConnectionDto {

    private Long id;

    @NotNull
    private Long sourceComponentId;

    @NotNull
    private Long targetComponentId;

    private String label;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSourceComponentId() { return sourceComponentId; }
    public void setSourceComponentId(Long sourceComponentId) { this.sourceComponentId = sourceComponentId; }

    public Long getTargetComponentId() { return targetComponentId; }
    public void setTargetComponentId(Long targetComponentId) { this.targetComponentId = targetComponentId; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
}