package com.archpilot.repository;

import com.archpilot.model.Component;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComponentRepository extends JpaRepository<Component, Long> {

    List<Component> findByProjectId(Long projectId);
}