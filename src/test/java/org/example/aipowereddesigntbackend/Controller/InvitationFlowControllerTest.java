package org.example.aipowereddesigntbackend.Controller;

import org.example.aipowereddesigntbackend.Dto.CreateProjectRequest;
import org.example.aipowereddesigntbackend.Dto.InviteCollaboratorRequest;
import org.example.aipowereddesigntbackend.Enum.Role;
import org.example.aipowereddesigntbackend.Model.Account;
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

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InvitationFlowControllerTest {

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

    private Account owner;
    private Account invitee;

    @BeforeEach
    void setUp() {
        owner = new Account();
        owner.setUserName("owner-" + UUID.randomUUID() + "@example.com");
        owner.setFullName("Owner User");
        owner.setProvider("GOOGLE");
        owner.setRole(Role.OWNER);
        owner = accountRepository.save(owner);

        invitee = new Account();
        invitee.setUserName("invitee-" + UUID.randomUUID() + "@example.com");
        invitee.setFullName("Invitee User");
        invitee.setProvider("GITHUB");
        invitee.setRole(Role.OWNER);
        invitee = accountRepository.save(invitee);
    }

    private OAuth2User principalFor(Account account) {
        return new DefaultOAuth2User(
                List.of(),
                Map.of("email", account.getUserName(), "name", account.getFullName()),
                "email"
        );
    }

    private UUID createProject(Account asOwner, String name) throws Exception {
        CreateProjectRequest request = new CreateProjectRequest(name, null, null);

        String body = mockMvc.perform(post("/api/projects")
                        .with(oauth2Login().oauth2User(principalFor(asOwner)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return UUID.fromString(objectMapper.readTree(body).get("id").asText());
    }

    private String inviteCollaboratorAndGetToken(UUID projectId, Account asOwner, String invitedEmail) throws Exception {
        mockMvc.perform(post("/api/projects/{id}/invitations", projectId)
                        .with(oauth2Login().oauth2User(principalFor(asOwner)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new InviteCollaboratorRequest(invitedEmail)))
                )
                .andExpect(status().isCreated());

        return invitationRepository.findByInvitedEmailIgnoreCase(invitedEmail).get(0).getToken();
    }

    @Test
    void fullInvitationLifecycle_inviteAcceptAndAppearInProjectList() throws Exception {
        UUID projectId = createProject(owner, "Collaborative Project");
        String token = inviteCollaboratorAndGetToken(projectId, owner, invitee.getUserName());

        verify(emailService).sendProjectInvitationEmail(
                org.mockito.ArgumentMatchers.eq(invitee.getUserName()), anyString(), anyString(), anyString());

        mockMvc.perform(get("/api/invitations/me")
                        .with(oauth2Login().oauth2User(principalFor(invitee))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status", is("PENDING")))
                .andExpect(jsonPath("$[0].projectId", is(projectId.toString())));

        mockMvc.perform(post("/api/invitations/{token}/accept", token)
                        .with(oauth2Login().oauth2User(principalFor(invitee)))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role", is("COLLABORATOR")))
                .andExpect(jsonPath("$.projectId", is(projectId.toString())));

        mockMvc.perform(get("/api/projects")
                        .with(oauth2Login().oauth2User(principalFor(invitee))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is(projectId.toString())))
                .andExpect(jsonPath("$[0].role", is("COLLABORATOR")));
    }

    @Test
    void declineInvitation_setsStatusToDeclinedAndDoesNotCreateMembership() throws Exception {
        UUID projectId = createProject(owner, "Project To Decline");
        String token = inviteCollaboratorAndGetToken(projectId, owner, invitee.getUserName());

        mockMvc.perform(post("/api/invitations/{token}/decline", token)
                        .with(oauth2Login().oauth2User(principalFor(invitee)))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/invitations/me")
                        .with(oauth2Login().oauth2User(principalFor(invitee))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status", is("DECLINED")));

        mockMvc.perform(get("/api/projects")
                        .with(oauth2Login().oauth2User(principalFor(invitee))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));
    }

    @Test
    void revokeInvitation_preventsLaterAcceptance() throws Exception {
        UUID projectId = createProject(owner, "Project To Revoke");
        String token = inviteCollaboratorAndGetToken(projectId, owner, invitee.getUserName());
        UUID invitationId = invitationRepository.findByInvitedEmailIgnoreCase(invitee.getUserName()).get(0).getId();

        mockMvc.perform(delete("/api/projects/{projectId}/invitations/{invitationId}", projectId, invitationId)
                        .with(oauth2Login().oauth2User(principalFor(owner)))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/invitations/{token}/accept", token)
                        .with(oauth2Login().oauth2User(principalFor(invitee)))
                        .with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    void invite_byNonOwner_isForbidden() throws Exception {
        UUID projectId = createProject(owner, "Private Project");

        mockMvc.perform(post("/api/projects/{id}/invitations", projectId)
                        .with(oauth2Login().oauth2User(principalFor(invitee)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new InviteCollaboratorRequest("someone-else@example.com"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void acceptInvitation_withDifferentAccountThanInvited_isForbidden() throws Exception {
        UUID projectId = createProject(owner, "Mismatched Invite Project");
        String token = inviteCollaboratorAndGetToken(projectId, owner, invitee.getUserName());

        Account bystander = new Account();
        bystander.setUserName("bystander-" + UUID.randomUUID() + "@example.com");
        bystander.setFullName("Bystander");
        bystander.setProvider("GOOGLE");
        bystander.setRole(Role.OWNER);
        bystander = accountRepository.save(bystander);

        mockMvc.perform(post("/api/invitations/{token}/accept", token)
                        .with(oauth2Login().oauth2User(principalFor(bystander)))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void removeMember_removesCollaboratorButNotOwner() throws Exception {
        UUID projectId = createProject(owner, "Membership Management Project");
        String token = inviteCollaboratorAndGetToken(projectId, owner, invitee.getUserName());

        mockMvc.perform(post("/api/invitations/{token}/accept", token)
                        .with(oauth2Login().oauth2User(principalFor(invitee)))
                        .with(csrf()))
                .andExpect(status().isOk());

        String membersBody = mockMvc.perform(get("/api/projects/{id}/members", projectId)
                        .with(oauth2Login().oauth2User(principalFor(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        var membersJson = objectMapper.readTree(membersBody);
        UUID ownerMemberId = null;
        UUID collaboratorMemberId = null;
        for (var node : membersJson) {
            if (node.get("role").asText().equals("OWNER")) {
                ownerMemberId = UUID.fromString(node.get("id").asText());
            } else {
                collaboratorMemberId = UUID.fromString(node.get("id").asText());
            }
        }

        mockMvc.perform(delete("/api/projects/{projectId}/members/{memberId}", projectId, ownerMemberId)
                        .with(oauth2Login().oauth2User(principalFor(owner)))
                        .with(csrf()))
                .andExpect(status().isConflict());

        mockMvc.perform(delete("/api/projects/{projectId}/members/{memberId}", projectId, collaboratorMemberId)
                        .with(oauth2Login().oauth2User(principalFor(owner)))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/projects")
                        .with(oauth2Login().oauth2User(principalFor(invitee))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));
    }
}