package org.example.aipowereddesigntbackend.Repository;

import org.example.aipowereddesigntbackend.Enum.InvitationStatus;
import org.example.aipowereddesigntbackend.Model.ProjectInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectInvitationRepository extends JpaRepository<ProjectInvitation, UUID> {
    Optional<ProjectInvitation> findByToken(String token);

    List<ProjectInvitation> findByProjectId(UUID projectId);

    List<ProjectInvitation> findByInvitedEmailIgnoreCase(String invitedEmail);

    boolean existsByProjectIdAndInvitedEmailIgnoreCaseAndStatus(
            UUID projectId, String invitedEmail, InvitationStatus status
    );
}