package com.ukgqtm.app.api;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ukgqtm.app.project.ProjectApplicationService;
import com.ukgqtm.app.project.ProjectApplicationService.CreateProjectCommand;
import com.ukgqtm.app.project.ProjectApplicationService.AddProjectMemberCommand;
import com.ukgqtm.app.project.ProjectApplicationService.ChangeProjectMemberRoleCommand;
import com.ukgqtm.app.project.ProjectApplicationService.ProjectMembershipSummary;
import com.ukgqtm.app.project.ProjectApplicationService.ProjectSummary;
import com.ukgqtm.app.security.ApplicationUserAuthenticationToken;
import com.ukgqtm.app.security.AuthorizationPolicy;
import com.ukgqtm.app.security.AuthorizationPolicyService;
import com.ukgqtm.identity.api.AuthenticatedUser;
import com.ukgqtm.project.domain.AccessPermission;
import com.ukgqtm.project.domain.ProjectRole;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProjectController.class)
@Import(ProjectControllerTest.TestSecurityConfig.class)
class ProjectControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProjectApplicationService projects;

    @MockBean
    private AuthorizationPolicyService authorization;

    @Test
    void ui02aListsMyProjectsWithoutCreateProjectForProjectRole() throws Exception {
        AuthenticatedUser user = user(false);
        when(authorization.globalCapabilities(user)).thenReturn(EnumSet.noneOf(AuthorizationPolicy.class));
        when(projects.visibleProjects(user)).thenReturn(List.of(project("Australian Broadcasting Corporation")));

        mockMvc.perform(get("/api/v1/projects").with(authentication(token(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scopeLabel").value("My Projects"))
                .andExpect(jsonPath("$.canCreateProject").value(false))
                .andExpect(jsonPath("$.projects[0].suiteCount").value(3))
                .andExpect(jsonPath("$.projects[0].cycleCount").value(2))
                .andExpect(jsonPath("$.projects[0].userCount").value(4))
                .andExpect(jsonPath("$.projects[0].name").value("Australian Broadcasting Corporation"));
    }

    @Test
    void ui02bListsAllProjectsWithCreateProjectForAdministrator() throws Exception {
        AuthenticatedUser user = user(true);
        when(authorization.globalCapabilities(user)).thenReturn(EnumSet.allOf(AuthorizationPolicy.class));
        when(projects.visibleProjects(user)).thenReturn(List.of(project("Austin Health")));

        mockMvc.perform(get("/api/v1/projects").with(authentication(token(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scopeLabel").value("All Projects"))
                .andExpect(jsonPath("$.canCreateProject").value(true))
                .andExpect(jsonPath("$.projects[0].name").value("Austin Health"));
    }

    @Test
    void manageUsersListsMembershipsForAuthorizedProjectManager() throws Exception {
        AuthenticatedUser user = user(false);
        UUID projectId = UUID.randomUUID();
        when(projects.listProjectMemberships(user, projectId)).thenReturn(List.of(membership(ProjectRole.TEST_MANAGER)));

        mockMvc.perform(get("/api/v1/projects/{projectId}/memberships", projectId).with(authentication(token(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberships[0].email").value("avery@example.test"))
                .andExpect(jsonPath("$.memberships[0].projectRole").value("Test Manager"));
    }

    @Test
    void manageUsersCrossProjectAccessIsDeniedBeforeServiceLookup() throws Exception {
        AuthenticatedUser user = user(false);
        UUID guessedProjectId = UUID.randomUUID();
        doThrow(new AccessDeniedException("The requested resource is not available."))
                .when(authorization)
                .require(
                        any(),
                        eq(AuthorizationPolicy.PROJECT_MANAGE_USERS),
                        eq(guessedProjectId),
                        eq("PROJECT"),
                        eq(guessedProjectId.toString()),
                        any());

        mockMvc.perform(get("/api/v1/projects/{projectId}/memberships", guessedProjectId).with(authentication(token(user))))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void addMembershipValidatesAndReturnsCreatedUser() throws Exception {
        AuthenticatedUser user = user(true);
        UUID projectId = UUID.randomUUID();
        ProjectMembershipSummary created = membership(ProjectRole.TEST_LEAD);
        AddProjectMemberCommand command = new AddProjectMemberCommand(
                "Beth", "Smith", "beth.smith@example.test", ProjectRole.TEST_LEAD);
        when(projects.addProjectMembership(eq(user), eq(projectId), any(), any())).thenReturn(created);

        mockMvc.perform(post("/api/v1/projects/{projectId}/memberships", projectId)
                        .with(authentication(token(user)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.projectRole").value("Test Lead"));
    }

    @Test
    void changeMembershipRoleUsesCentralAuthorization() throws Exception {
        AuthenticatedUser user = user(true);
        UUID projectId = UUID.randomUUID();
        UUID membershipId = UUID.randomUUID();
        when(projects.changeProjectMembershipRole(eq(user), eq(projectId), eq(membershipId), any(), any()))
                .thenReturn(membership(ProjectRole.TEST_ANALYST));

        mockMvc.perform(patch("/api/v1/projects/{projectId}/memberships/{membershipId}", projectId, membershipId)
                        .with(authentication(token(user)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangeProjectMemberRoleCommand(ProjectRole.TEST_ANALYST, false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectRole").value("Test Analyst"));
    }

    @Test
    void disableMembershipReturnsNoContent() throws Exception {
        AuthenticatedUser user = user(true);
        UUID projectId = UUID.randomUUID();
        UUID membershipId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/projects/{projectId}/memberships/{membershipId}", projectId, membershipId)
                        .with(authentication(token(user))))
                .andExpect(status().isNoContent());
    }

    @Test
    void createProjectDeniedUsesProblemDetailsWithoutExistenceLeakage() throws Exception {
        AuthenticatedUser user = user(false);
        doThrow(new AccessDeniedException("The requested resource is not available."))
                .when(authorization)
                .require(any(), eq(AuthorizationPolicy.PROJECT_CREATE), eq(null), eq("PROJECT"), eq(null), any());

        mockMvc.perform(post("/api/v1/projects")
                        .with(authentication(token(user)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateProjectCommand("NEW", "New Project", "Description"))))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("The requested resource is not available."));
    }

    @Test
    void guessedCrossProjectIdIsDeniedBeforeProjectLookup() throws Exception {
        AuthenticatedUser user = user(false);
        UUID guessedProjectId = UUID.randomUUID();
        doThrow(new AccessDeniedException("The requested resource is not available."))
                .when(authorization)
                .require(
                        any(),
                        eq(AuthorizationPolicy.PROJECT_VIEW),
                        eq(guessedProjectId),
                        eq("PROJECT"),
                        eq(guessedProjectId.toString()),
                        any());

        mockMvc.perform(get("/api/v1/projects/{projectId}", guessedProjectId).with(authentication(token(user))))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("The requested resource is not available."));
    }

    @Test
    void visibleProjectIncludesProjectCapabilities() throws Exception {
        AuthenticatedUser user = user(false);
        ProjectSummary summary = project("Australian Broadcasting Corporation");
        when(projects.visibleProject(user, summary.id())).thenReturn(Optional.of(summary));
        when(authorization.projectCapabilities(user, summary.id()))
                .thenReturn(EnumSet.of(AuthorizationPolicy.PROJECT_VIEW, AuthorizationPolicy.TEST_CASE_VIEW_EXPORT));
        when(authorization.projectPermissions(user, summary.id()))
                .thenReturn(EnumSet.of(AccessPermission.VIEW, AccessPermission.CREATE));

        mockMvc.perform(get("/api/v1/projects/{projectId}", summary.id()).with(authentication(token(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.project.name").value("Australian Broadcasting Corporation"))
                .andExpect(jsonPath("$.capabilities[0]", containsString("PROJECT_VIEW")))
                .andExpect(jsonPath("$.permissions[1]").value("VIEW"));
    }

    @Test
    void unauthenticatedProjectRequestUsesProblemDetails() throws Exception {
        mockMvc.perform(get("/api/v1/projects"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")));
    }

    @Test
    void jsonEndpointsRejectUnsupportedRequestContentTypeWithProblemDetails() throws Exception {
        AuthenticatedUser user = user(true);

        mockMvc.perform(post("/api/v1/projects")
                        .with(authentication(token(user)))
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("name=Bad"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Unsupported Media Type"));
    }

    @Test
    void jsonEndpointsRejectUnsupportedAcceptHeaderWithProblemDetails() throws Exception {
        AuthenticatedUser user = user(true);

        mockMvc.perform(get("/api/v1/projects")
                        .with(authentication(token(user)))
                        .accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Not Acceptable"));
    }

    private static ApplicationUserAuthenticationToken token(AuthenticatedUser user) {
        return new ApplicationUserAuthenticationToken(user, null, List.of());
    }

    private static AuthenticatedUser user(boolean administrator) {
        return new AuthenticatedUser(
                UUID.randomUUID(),
                "tenant-1",
                UUID.randomUUID().toString(),
                "Avery",
                "Tester",
                "avery@example.test",
                administrator);
    }

    private static ProjectSummary project(String name) {
        return new ProjectSummary(UUID.randomUUID(), name.substring(0, 3).toUpperCase(), name, "Timekeeping", true, 3, 2, 4);
    }

    private static ProjectMembershipSummary membership(ProjectRole role) {
        return new ProjectMembershipSummary(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Avery",
                "Tester",
                "avery@example.test",
                role.databaseValue(),
                "ACTIVE",
                "INVITED",
                false);
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            http.csrf(AbstractHttpConfigurer::disable);
            http.authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated());
            http.exceptionHandling(exceptions -> exceptions.authenticationEntryPoint((request, response, exception) -> {
                response.setStatus(401);
                response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                response.getWriter().write("{\"title\":\"Unauthorized\",\"status\":401}");
            }));
            return http.build();
        }
    }
}
