package org.example.aipowereddesigntbackend.Controller;

import org.example.aipowereddesigntbackend.Dto.ComponentRequest;
import org.example.aipowereddesigntbackend.Dto.CreateProjectRequest;
import org.example.aipowereddesigntbackend.Dto.InviteCollaboratorRequest;
import org.example.aipowereddesigntbackend.Enum.ComponentType;
import org.example.aipowereddesigntbackend.Enum.Role;
import org.example.aipowereddesigntbackend.Model.Account;
import org.example.aipowereddesigntbackend.Repository.AccountRepository;
import org.example.aipowereddesigntbackend.Repository.ProjectInvitationRepository;
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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
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
class ComponentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ProjectInvitationRepository invitationRepository;

    @MockitoBean
    private EmailService emailService;

    private Account owner;
    private Account collaborator;
    private Account stranger;

    @BeforeEach
    void setUp() {
        owner = saveAccount("owner");
        collaborator = saveAccount("collaborator");
        stranger = saveAccount("stranger");
    }

    private Account saveAccount(String label) {
        Account account = new Account();
        account.setUserName(label + "-" + UUID.randomUUID() + "@example.com");
        account.setFullName(label);
        account.setProvider("GOOGLE");
        account.setRole(Role.OWNER);
        return accountRepository.save(account);
    }

    private OAuth2User principalFor(Account account) {
        return new DefaultOAuth2User(
                List.of(),
                Map.of("email", account.getUserName(), "name", account.getFullName()),
                "email"
        );
    }

    private UUID createProject(Account asOwner) throws Exception {
        CreateProjectRequest request = new CreateProjectRequest("Architecture Project", null, null);
        String body = mockMvc.perform(post("/api/projects")
                        .with(oauth2Login().oauth2User(principalFor(asOwner)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(body).get("id").asText());
    }

    private void addAcceptedCollaborator(UUID projectId) throws Exception {
        mockMvc.perform(post("/api/projects/{id}/invitations", projectId)
                        .with(oauth2Login().oauth2User(principalFor(owner)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new InviteCollaboratorRequest(collaborator.getUserName()))))
                .andExpect(status().isCreated());

        String token = invitationRepository.findByInvitedEmailIgnoreCase(collaborator.getUserName()).get(0).getToken();

        mockMvc.perform(post("/api/invitations/{token}/accept", token)
                        .with(oauth2Login().oauth2User(principalFor(collaborator)))
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void addComponent_asOwner_returns201() throws Exception {
        UUID projectId = createProject(owner);
        ComponentRequest request = new ComponentRequest("Web Frontend", ComponentType.FRONTEND, "React SPA");

        mockMvc.perform(post("/api/projects/{projectId}/components", projectId)
                        .with(oauth2Login().oauth2User(principalFor(owner)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", org.hamcrest.Matchers.notNullValue()))
                .andExpect(jsonPath("$.name", is("Web Frontend")))
                .andExpect(jsonPath("$.type", is("FRONTEND")))
                .andExpect(jsonPath("$.projectId", is(projectId.toString())));
    }

    @Test
    void addComponent_asAcceptedCollaborator_returns201() throws Exception {
        UUID projectId = createProject(owner);
        addAcceptedCollaborator(projectId);

        ComponentRequest request = new ComponentRequest("Primary Database", ComponentType.DATABASE, null);

        mockMvc.perform(post("/api/projects/{projectId}/components", projectId)
                        .with(oauth2Login().oauth2User(principalFor(collaborator)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type", is("DATABASE")));
    }

    @Test
    void addComponent_byNonMember_isForbidden() throws Exception {
        UUID projectId = createProject(owner);
        ComponentRequest request = new ComponentRequest("Unauthorized Component", ComponentType.CACHE, null);

        mockMvc.perform(post("/api/projects/{projectId}/components", projectId)
                        .with(oauth2Login().oauth2User(principalFor(stranger)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void addComponent_withBlankName_returns400() throws Exception {
        UUID projectId = createProject(owner);
        ComponentRequest request = new ComponentRequest("   ", ComponentType.BACKEND, null);

        mockMvc.perform(post("/api/projects/{projectId}/components", projectId)
                        .with(oauth2Login().oauth2User(principalFor(owner)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addComponent_withMissingType_returns400() throws Exception {
        UUID projectId = createProject(owner);
        String payloadWithoutType = "{\"name\": \"Mystery Component\"}";

        mockMvc.perform(post("/api/projects/{projectId}/components", projectId)
                        .with(oauth2Login().oauth2User(principalFor(owner)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadWithoutType))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listComponents_returnsComponentsForProjectMember() throws Exception {
        UUID projectId = createProject(owner);
        mockMvc.perform(post("/api/projects/{projectId}/components", projectId)
                        .with(oauth2Login().oauth2User(principalFor(owner)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ComponentRequest("Backend API", ComponentType.BACKEND, null))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/projects/{projectId}/components", projectId)
                        .with(oauth2Login().oauth2User(principalFor(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Backend API")));
    }

    @Test
    void updateAndDeleteComponent_asOwner_succeeds() throws Exception {
        UUID projectId = createProject(owner);
        String createBody = mockMvc.perform(post("/api/projects/{projectId}/components", projectId)
                        .with(oauth2Login().oauth2User(principalFor(owner)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ComponentRequest("Cache Layer", ComponentType.CACHE, null))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID componentId = UUID.fromString(objectMapper.readTree(createBody).get("id").asText());

        mockMvc.perform(put("/api/components/{id}", componentId)
                        .with(oauth2Login().oauth2User(principalFor(owner)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ComponentRequest("Redis Cache", ComponentType.CACHE, "Updated"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Redis Cache")))
                .andExpect(jsonPath("$.description", is("Updated")));

        mockMvc.perform(delete("/api/components/{id}", componentId)
                        .with(oauth2Login().oauth2User(principalFor(owner)))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/projects/{projectId}/components", projectId)
                        .with(oauth2Login().oauth2User(principalFor(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void updateComponent_byNonMember_isForbidden() throws Exception {
        UUID projectId = createProject(owner);
        String createBody = mockMvc.perform(post("/api/projects/{projectId}/components", projectId)
                        .with(oauth2Login().oauth2User(principalFor(owner)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ComponentRequest("External Payment API", ComponentType.EXTERNAL_API, null))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID componentId = UUID.fromString(objectMapper.readTree(createBody).get("id").asText());

        mockMvc.perform(put("/api/components/{id}", componentId)
                        .with(oauth2Login().oauth2User(principalFor(stranger)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ComponentRequest("Hijacked", ComponentType.EXTERNAL_API, null))))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/components/{id}", componentId)
                        .with(oauth2Login().oauth2User(principalFor(stranger)))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void addComponent_toUnknownProject_returns404() throws Exception {
        ComponentRequest request = new ComponentRequest("Orphan Component", ComponentType.OTHER, null);

        mockMvc.perform(post("/api/projects/{projectId}/components", UUID.randomUUID())
                        .with(oauth2Login().oauth2User(principalFor(owner)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }
}