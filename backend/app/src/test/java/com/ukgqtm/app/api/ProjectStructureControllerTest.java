package com.ukgqtm.app.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ukgqtm.app.project.ProjectStructureApplicationService;
import com.ukgqtm.app.project.ProjectStructureApplicationService.AssignSuiteCommand;
import com.ukgqtm.app.project.ProjectStructureApplicationService.ProjectCycleSummary;
import com.ukgqtm.app.project.ProjectStructureApplicationService.ProjectSuiteAssignmentSummary;
import com.ukgqtm.app.project.ProjectStructureApplicationService.SaveCycleCommand;
import com.ukgqtm.app.project.ProjectStructureApplicationService.SuiteCatalogSummary;
import com.ukgqtm.app.project.ProjectStructureApplicationService.UpdateSuiteCommand;
import com.ukgqtm.app.security.ApplicationUserAuthenticationToken;
import com.ukgqtm.app.security.AuthorizationPolicy;
import com.ukgqtm.app.security.AuthorizationPolicyService;
import com.ukgqtm.identity.api.AuthenticatedUser;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProjectStructureController.class)
@Import(ProjectStructureControllerTest.TestSecurityConfig.class)
class ProjectStructureControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProjectStructureApplicationService structures;

    @MockBean
    private AuthorizationPolicyService authorization;

    @Test
    void suiteAssignmentsAreReadableWithProjectViewForContentSelectors() throws Exception {
        AuthenticatedUser user = user(false);
        UUID projectId = UUID.randomUUID();
        when(structures.listProjectSuiteAssignments(user, projectId)).thenReturn(List.of(assignment(projectId)));

        mockMvc.perform(get("/api/v1/projects/{projectId}/suite-assignments", projectId)
                        .with(authentication(token(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignments[0].name").value("Timekeeping"));
    }

    @Test
    void testLeadCannotAssignSuite() throws Exception {
        AuthenticatedUser user = user(false);
        UUID projectId = UUID.randomUUID();
        doThrow(new AccessDeniedException("The requested resource is not available."))
                .when(authorization)
                .require(
                        any(),
                        eq(AuthorizationPolicy.PROJECT_MANAGE_SUITES),
                        eq(projectId),
                        eq("PROJECT"),
                        eq(projectId.toString()),
                        any());

        mockMvc.perform(post("/api/v1/projects/{projectId}/suite-assignments", projectId)
                        .with(authentication(token(user)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AssignSuiteCommand(null, "Timekeeping", "Core time capture"))))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void assignSuiteReturnsCreatedAssignment() throws Exception {
        AuthenticatedUser user = user(true);
        UUID projectId = UUID.randomUUID();
        when(structures.createOrAssignSuite(eq(user), eq(projectId), any(), any())).thenReturn(assignment(projectId));

        mockMvc.perform(post("/api/v1/projects/{projectId}/suite-assignments", projectId)
                        .with(authentication(token(user)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AssignSuiteCommand(null, "Timekeeping", "Core time capture"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.suiteKey").value("TIMEKEEPING"));
    }

    @Test
    void updateSuiteRequiresProjectManageSuitesForTheRequestedProject() throws Exception {
        AuthenticatedUser user = user(false);
        UUID projectId = UUID.randomUUID();
        UUID suiteId = UUID.randomUUID();
        doThrow(new AccessDeniedException("The requested resource is not available."))
                .when(authorization)
                .require(
                        any(),
                        eq(AuthorizationPolicy.PROJECT_MANAGE_SUITES),
                        eq(projectId),
                        eq("TEST_SUITE"),
                        eq(suiteId.toString()),
                        any());

        mockMvc.perform(patch("/api/v1/suites/{suiteId}?projectId={projectId}", suiteId, projectId)
                        .with(authentication(token(user)))
                        .header("If-Match", "0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateSuiteCommand("Integration", "Integration testing"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void cyclesAreReadableWithProjectView() throws Exception {
        AuthenticatedUser user = user(false);
        UUID projectId = UUID.randomUUID();
        when(structures.listProjectCycles(user, projectId)).thenReturn(List.of(cycle(projectId)));

        mockMvc.perform(get("/api/v1/projects/{projectId}/cycles", projectId)
                        .with(authentication(token(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cycles[0].name").value("Cycle 1"));
    }

    @Test
    void testAnalystCannotCreateCycle() throws Exception {
        AuthenticatedUser user = user(false);
        UUID projectId = UUID.randomUUID();
        doThrow(new AccessDeniedException("The requested resource is not available."))
                .when(authorization)
                .require(
                        any(),
                        eq(AuthorizationPolicy.PROJECT_MANAGE_CYCLES),
                        eq(projectId),
                        eq("PROJECT"),
                        eq(projectId.toString()),
                        any());

        mockMvc.perform(post("/api/v1/projects/{projectId}/cycles", projectId)
                        .with(authentication(token(user)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SaveCycleCommand("Cycle 1", LocalDate.now(), LocalDate.now(), "Description"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateCycleUsesOptimisticIfMatchHeader() throws Exception {
        AuthenticatedUser user = user(true);
        UUID projectId = UUID.randomUUID();
        UUID cycleId = UUID.randomUUID();
        when(structures.updateCycle(eq(user), eq(projectId), eq(cycleId), any(), eq("0"), any()))
                .thenReturn(cycle(projectId));

        mockMvc.perform(patch("/api/v1/projects/{projectId}/cycles/{cycleId}", projectId, cycleId)
                        .with(authentication(token(user)))
                        .header("If-Match", "0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SaveCycleCommand("Cycle 1", LocalDate.now(), LocalDate.now(), "Description"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(0));
    }

    private static ProjectSuiteAssignmentSummary assignment(UUID projectId) {
        return new ProjectSuiteAssignmentSummary(
                UUID.randomUUID(),
                projectId,
                UUID.randomUUID(),
                "TIMEKEEPING",
                "Timekeeping",
                "Core time capture",
                true,
                0,
                0);
    }

    private static ProjectCycleSummary cycle(UUID projectId) {
        return new ProjectCycleSummary(
                UUID.randomUUID(),
                projectId,
                "Cycle 1",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31),
                "Regression",
                true,
                0);
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
