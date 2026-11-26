package com.archpilot.repository;

import com.archpilot.model.Connection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConnectionRepository extends JpaRepository<Connection, Long> {

    List<Connection> findByProjectId(Long projectId);

    List<Connection> findBySourceComponentIdOrTargetComponentId(Long sourceId, Long targetId);
}