package org.example.aipowereddesigntbackend.Repository;

import org.example.aipowereddesigntbackend.Model.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, UUID> {
    List<ProjectMember> findByProjectId(UUID projectId);

    List<ProjectMember> findByAccountId(UUID accountId);

    boolean existsByProjectIdAndAccountId(UUID projectId, UUID accountId);
}