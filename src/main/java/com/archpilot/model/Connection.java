package com.archpilot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "connections")
public class Connection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long projectId;

    @Column(nullable = false)
    private Long sourceComponentId;

    @Column(nullable = false)
    private Long targetComponentId;

    private String label;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public Long getSourceComponentId() { return sourceComponentId; }
    public void setSourceComponentId(Long sourceComponentId) { this.sourceComponentId = sourceComponentId; }

    public Long getTargetComponentId() { return targetComponentId; }
    public void setTargetComponentId(Long targetComponentId) { this.targetComponentId = targetComponentId; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
}