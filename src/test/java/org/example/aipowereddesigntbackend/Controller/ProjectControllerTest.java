package org.example.aipowereddesigntbackend.Controller;

import org.example.aipowereddesigntbackend.Dto.CreateProjectRequest;
import org.example.aipowereddesigntbackend.Dto.InviteCollaboratorRequest;
import org.example.aipowereddesigntbackend.Enum.Role;
import org.example.aipowereddesigntbackend.Model.Account;
import org.example.aipowereddesigntbackend.Model.ProjectMember;
import org.example.aipowereddesigntbackend.Repository.AccountRepository;
import org.example.aipowereddesigntbackend.Repository.ProjectInvitationRepository;
import org.example.aipowereddesigntbackend.Repository.ProjectMemberRepository;
import org.example.aipowereddesigntbackend.Repository.ProjectRepository;
import org.example.aipowereddesigntbackend.Service.EmailService;
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
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private ProjectInvitationRepository invitationRepository;

    @MockitoBean
    private EmailService emailService;

    private Account googleAccount;
    private Account githubAccount;

    @BeforeEach
    void setUp() {
        googleAccount = new Account();
        googleAccount.setUserName("google-user-" + UUID.randomUUID() + "@example.com");
        googleAccount.setFullName("Google User");
        googleAccount.setProvider("GOOGLE");
        googleAccount.setRole(Role.OWNER);
        googleAccount = accountRepository.save(googleAccount);

        githubAccount = new Account();
        githubAccount.setUserName("github-user-" + UUID.randomUUID() + "@example.com");
        githubAccount.setFullName("GitHub User");
        githubAccount.setProvider("GITHUB");
        githubAccount.setRole(Role.OWNER);
        githubAccount = accountRepository.save(githubAccount);
    }

    private OAuth2User principalFor(Account account) {
        return new DefaultOAuth2User(
                List.of(),
                Map.of("email", account.getUserName(), "name", account.getFullName()),
                "email"
        );
    }

    @Test
    void createProject_asAuthenticatedGoogleUser_returns201AndPersistsProjectWithOwner() throws Exception {
        CreateProjectRequest request = new CreateProjectRequest(
                "E-commerce Platform",
                "System architecture for an online shopping platform",
                "Authentication, payment processing and product management"
        );

        mockMvc.perform(post("/api/projects")
                        .with(oauth2Login().oauth2User(principalFor(googleAccount)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("E-commerce Platform")))
                .andExpect(jsonPath("$.ownerId", is(googleAccount.getId().toString())))
                .andExpect(jsonPath("$.role", is("OWNER")))
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andExpect(jsonPath("$.updatedAt", notNullValue()));

        assertThat(projectRepository.findAll()).hasSize(1);
        List<ProjectMember> members = projectMemberRepository.findByProjectId(
                projectRepository.findAll().get(0).getId()
        );
        assertThat(members).hasSize(1);
        assertThat(members.get(0).getRole()).isEqualTo(Role.OWNER);
        assertThat(members.get(0).getAccount().getId()).isEqualTo(googleAccount.getId());
    }

    @Test
    void createProject_asAuthenticatedGithubUser_returns201AndAssignsOwnership() throws Exception {
        CreateProjectRequest request = new CreateProjectRequest("Inventory System", null, null);

        mockMvc.perform(post("/api/projects")
                        .with(oauth2Login().oauth2User(principalFor(githubAccount)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerId", is(githubAccount.getId().toString())))
                .andExpect(jsonPath("$.role", is("OWNER")));
    }

    @Test
    void createProject_withoutAuthentication_isRejected() throws Exception {
        CreateProjectRequest request = new CreateProjectRequest("Unauthorized Project", null, null);

        mockMvc.perform(post("/api/projects")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is3xxRedirection());

        assertThat(projectRepository.findAll()).isEmpty();
    }

    @Test
    void createProject_withBlankName_returns400() throws Exception {
        CreateProjectRequest request = new CreateProjectRequest("   ", "desc", null);

        mockMvc.perform(post("/api/projects")
                        .with(oauth2Login().oauth2User(principalFor(googleAccount)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        assertThat(projectRepository.findAll()).isEmpty();
    }

    @Test
    void createProject_withNameExceedingMaxLength_returns400() throws Exception {
        CreateProjectRequest request = new CreateProjectRequest("a".repeat(151), null, null);

        mockMvc.perform(post("/api/projects")
                        .with(oauth2Login().oauth2User(principalFor(googleAccount)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createProject_withForgedOwnershipFields_ignoresThemAndAssignsAuthenticatedUserAsOwner() throws Exception {
        String payloadWithForgedOwner = """
                {
                  "name": "Forged Ownership Project",
                  "ownerId": "11111111-1111-1111-1111-111111111111",
                  "role": "OWNER"
                }
                """;

        mockMvc.perform(post("/api/projects")
                        .with(oauth2Login().oauth2User(principalFor(googleAccount)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadWithForgedOwner))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerId", is(googleAccount.getId().toString())));

        List<ProjectMember> members = projectMemberRepository.findByProjectId(
                projectRepository.findAll().get(0).getId()
        );
        assertThat(members).hasSize(1);
        assertThat(members.get(0).getAccount().getId()).isEqualTo(googleAccount.getId());
    }

    private UUID createProject(Account asOwner, String name) throws Exception {
        CreateProjectRequest request = new CreateProjectRequest(name, "Initial description", null);
        String body = mockMvc.perform(post("/api/projects")
                        .with(oauth2Login().oauth2User(principalFor(asOwner)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(body).get("id").asText());
    }

    private void addAcceptedCollaborator(UUID projectId, Account owner, Account invitee) throws Exception {
        mockMvc.perform(post("/api/projects/{id}/invitations", projectId)
                        .with(oauth2Login().oauth2User(principalFor(owner)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new InviteCollaboratorRequest(invitee.getUserName()))))
                .andExpect(status().isCreated());

        String token = invitationRepository.findByInvitedEmailIgnoreCase(invitee.getUserName()).get(0).getToken();

        mockMvc.perform(post("/api/invitations/{token}/accept", token)
                        .with(oauth2Login().oauth2User(principalFor(invitee)))
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void getProject_asOwner_returnsDetailWithComponents() throws Exception {
        UUID projectId = createProject(googleAccount, "Detailed Project");

        mockMvc.perform(post("/api/projects/{projectId}/components", projectId)
                        .with(oauth2Login().oauth2User(principalFor(googleAccount)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Web Frontend\", \"type\": \"FRONTEND\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/projects/{projectId}", projectId)
                        .with(oauth2Login().oauth2User(principalFor(googleAccount))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.project.id", is(projectId.toString())))
                .andExpect(jsonPath("$.project.role", is("OWNER")))
                .andExpect(jsonPath("$.components", hasSize(1)))
                .andExpect(jsonPath("$.components[0].name", is("Web Frontend")));
    }

    @Test
    void getProject_asAcceptedCollaborator_returnsDetail() throws Exception {
        UUID projectId = createProject(googleAccount, "Shared Project");
        addAcceptedCollaborator(projectId, googleAccount, githubAccount);

        mockMvc.perform(get("/api/projects/{projectId}", projectId)
                        .with(oauth2Login().oauth2User(principalFor(githubAccount))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.project.role", is("COLLABORATOR")));
    }

    @Test
    void getProject_byNonMember_isForbidden() throws Exception {
        UUID projectId = createProject(googleAccount, "Private Project");

        mockMvc.perform(get("/api/projects/{projectId}", projectId)
                        .with(oauth2Login().oauth2User(principalFor(githubAccount))))
                .andExpect(status().isForbidden());
    }

    @Test
    void getProject_withUnknownId_returns404() throws Exception {
        mockMvc.perform(get("/api/projects/{projectId}", UUID.randomUUID())
                        .with(oauth2Login().oauth2User(principalFor(googleAccount))))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateProject_asOwner_updatesFields() throws Exception {
        UUID projectId = createProject(googleAccount, "Old Name");
        CreateProjectRequest update = new CreateProjectRequest("New Name", "New description", "New requirements");

        mockMvc.perform(put("/api/projects/{projectId}", projectId)
                        .with(oauth2Login().oauth2User(principalFor(googleAccount)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("New Name")))
                .andExpect(jsonPath("$.description", is("New description")))
                .andExpect(jsonPath("$.requirements", is("New requirements")));
    }

    @Test
    void updateProject_asAcceptedCollaborator_updatesFields() throws Exception {
        UUID projectId = createProject(googleAccount, "Collab Edit Project");
        addAcceptedCollaborator(projectId, googleAccount, githubAccount);

        CreateProjectRequest update = new CreateProjectRequest("Updated By Collaborator", null, null);

        mockMvc.perform(put("/api/projects/{projectId}", projectId)
                        .with(oauth2Login().oauth2User(principalFor(githubAccount)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated By Collaborator")))
                .andExpect(jsonPath("$.role", is("COLLABORATOR")));
    }

    @Test
    void updateProject_byNonMember_isForbidden() throws Exception {
        UUID projectId = createProject(googleAccount, "Private Project");
        CreateProjectRequest update = new CreateProjectRequest("Hijacked", null, null);

        mockMvc.perform(put("/api/projects/{projectId}", projectId)
                        .with(oauth2Login().oauth2User(principalFor(githubAccount)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteProject_asOwner_deletesProjectAndItsComponents() throws Exception {
        UUID projectId = createProject(googleAccount, "Doomed Project");

        mockMvc.perform(post("/api/projects/{projectId}/components", projectId)
                        .with(oauth2Login().oauth2User(principalFor(googleAccount)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Soon Gone\", \"type\": \"CACHE\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/projects/{projectId}", projectId)
                        .with(oauth2Login().oauth2User(principalFor(googleAccount)))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        assertThat(projectRepository.findById(projectId)).isEmpty();

        mockMvc.perform(get("/api/projects")
                        .with(oauth2Login().oauth2User(principalFor(googleAccount))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void deleteProject_byAcceptedCollaborator_isForbidden() throws Exception {
        UUID projectId = createProject(googleAccount, "Collaborator Cannot Delete");
        addAcceptedCollaborator(projectId, googleAccount, githubAccount);

        mockMvc.perform(delete("/api/projects/{projectId}", projectId)
                        .with(oauth2Login().oauth2User(principalFor(githubAccount)))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        assertThat(projectRepository.findById(projectId)).isPresent();
    }

    @Test
    void deleteProject_byNonMember_isForbidden() throws Exception {
        UUID projectId = createProject(googleAccount, "Untouchable Project");

        mockMvc.perform(delete("/api/projects/{projectId}", projectId)
                        .with(oauth2Login().oauth2User(principalFor(githubAccount)))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        assertThat(projectRepository.findById(projectId)).isPresent();
    }
}