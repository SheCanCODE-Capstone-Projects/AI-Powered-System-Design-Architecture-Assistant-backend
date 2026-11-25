package org.example.aipowereddesigntbackend.Repository;

import org.example.aipowereddesigntbackend.Model.Component;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ComponentRepository extends JpaRepository<Component, UUID> {
    List<Component> findByProjectId(UUID projectId);
}