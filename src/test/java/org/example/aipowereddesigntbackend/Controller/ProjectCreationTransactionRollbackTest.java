package org.example.aipowereddesigntbackend.Controller;

import org.example.aipowereddesigntbackend.Dto.CreateProjectRequest;
import org.example.aipowereddesigntbackend.Enum.Role;
import org.example.aipowereddesigntbackend.Model.Account;
import org.example.aipowereddesigntbackend.Repository.AccountRepository;
import org.example.aipowereddesigntbackend.Repository.ProjectMemberRepository;
import org.example.aipowereddesigntbackend.Repository.ProjectRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies that a persistence failure while saving the owner membership rolls
 * back the whole project-creation transaction, leaving no orphaned Project row.
 * Deliberately not run inside a test-managed transaction: the rollback under
 * test is performed by ProjectService's own transaction, which must actually
 * commit/rollback against the database to be observable here.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProjectCreationTransactionRollbackTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @MockitoBean
    private ProjectMemberRepository projectMemberRepository;

    private Account owner;

    @BeforeEach
    void setUp() {
        owner = new Account();
        owner.setUserName("rollback-user-" + UUID.randomUUID() + "@example.com");
        owner.setFullName("Rollback User");
        owner.setProvider("GOOGLE");
        owner.setRole(Role.OWNER);
        owner = accountRepository.save(owner);

        when(projectMemberRepository.save(any()))
                .thenThrow(new org.springframework.dao.DataAccessResourceFailureException("simulated membership persistence failure"));
    }

    @AfterEach
    void tearDown() {
        projectRepository.findAll().stream()
                .filter(project -> project.getOwner().getId().equals(owner.getId()))
                .forEach(projectRepository::delete);
        accountRepository.delete(owner);
    }

    @Test
    void createProject_whenMembershipPersistenceFails_rollsBackProjectCreation() throws Exception {
        CreateProjectRequest request = new CreateProjectRequest("Rollback Project", null, null);

        OAuth2User principal = new DefaultOAuth2User(
                List.of(),
                Map.of("email", owner.getUserName(), "name", owner.getFullName()),
                "email"
        );

        mockMvc.perform(post("/api/projects")
                        .with(oauth2Login().oauth2User(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());

        assertThat(projectRepository.findAll()).isEmpty();
    }
}