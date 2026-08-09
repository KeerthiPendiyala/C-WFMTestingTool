package com.ukgqtm.app.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ukgqtm.app.security.ApplicationUserAuthenticationToken;
import com.ukgqtm.app.security.AuthorizationPolicy;
import com.ukgqtm.app.security.AuthorizationPolicyService;
import com.ukgqtm.app.testcase.TestCaseApplicationService;
import com.ukgqtm.app.testcase.TestCaseApplicationService.CreateAdhocTestCaseCommand;
import com.ukgqtm.app.testcase.TestCaseApplicationService.CreateManualTestCaseCommand;
import com.ukgqtm.app.testcase.TestCaseApplicationService.TestCaseGenerationResult;
import com.ukgqtm.app.testcase.TestCaseApplicationService.TestCaseSummary;
import com.ukgqtm.identity.api.AuthenticatedUser;
import java.time.Instant;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TestCaseController.class)
@Import(TestCaseControllerTest.TestSecurityConfig.class)
class TestCaseControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TestCaseApplicationService testCases;

    @MockBean
    private AuthorizationPolicyService authorization;

    @Test
    void createManualDeniedBeforeServiceWhenRolePolicyFails() throws Exception {
        AuthenticatedUser user = user();
        UUID projectId = UUID.randomUUID();
        doThrow(new AccessDeniedException("The requested resource is not available."))
                .when(authorization)
                .require(
                        any(),
                        eq(AuthorizationPolicy.TEST_CASE_CREATE),
                        eq(projectId),
                        eq("TEST_CASE"),
                        eq(null),
                        any());

        mockMvc.perform(post("/api/v1/test-cases")
                        .with(authentication(token(user)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateManualTestCaseCommand(
                                projectId,
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                "Clock-in",
                                "Validate clock-in"))))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        verifyNoInteractions(testCases);
    }

    @Test
    void csvImportUsesSameCreatePolicyAsManualCreation() throws Exception {
        AuthenticatedUser user = user();
        UUID projectId = UUID.randomUUID();
        UUID requirementId = UUID.randomUUID();
        var csv = new MockMultipartFile(
                "csv",
                "test-cases.csv",
                "text/csv",
                "Test Case Header,Description\r\nClock-in case,Validate clock-in\r\n".getBytes());
        when(testCases.importCsv(any(), any(), any(), eq("csv-key-1"), any()))
                .thenReturn(new TestCaseGenerationResult(UUID.randomUUID(), 1, List.of()));

        mockMvc.perform(multipart("/api/v1/test-cases:import-csv")
                        .file(csv)
                        .param("projectId", projectId.toString())
                        .param("requirementId", requirementId.toString())
                        .header(ApiHeaders.IDEMPOTENCY_KEY, "csv-key-1")
                        .with(authentication(token(user))))
                .andExpect(status().isCreated());

        verify(authorization)
                .require(
                        any(),
                        eq(AuthorizationPolicy.TEST_CASE_CREATE),
                        eq(projectId),
                        eq("TEST_CASE"),
                        eq(null),
                        any());
        verify(authorization, never())
                .require(
                        any(),
                        eq(AuthorizationPolicy.UPLOAD_ACCESS),
                        eq(projectId),
                        eq("TEST_CASE"),
                        eq(null),
                any());
    }

    @Test
    void createAdhocManualRejectsHiddenRequirementIdBeforeService() throws Exception {
        AuthenticatedUser user = user();
        UUID projectId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/test-cases/adhoc")
                        .with(authentication(token(user)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectId": "%s",
                                  "projectSuiteAssignmentId": "%s",
                                  "testCycleId": "%s",
                                  "requirementId": "%s",
                                  "header": "Ad hoc case",
                                  "description": "No requirement link allowed"
                                }
                                """
                                .formatted(
                                        projectId,
                                        UUID.randomUUID(),
                                        UUID.randomUUID(),
                                        UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        verifyNoInteractions(testCases);
    }

    @Test
    void createAdhocManualUsesCreatePolicyAndAdhocCommand() throws Exception {
        AuthenticatedUser user = user();
        UUID projectId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        UUID cycleId = UUID.randomUUID();
        when(testCases.createAdhocManual(any(), any(), any()))
                .thenReturn(summary(projectId, assignmentId, cycleId));

        mockMvc.perform(post("/api/v1/test-cases/adhoc")
                        .with(authentication(token(user)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateAdhocTestCaseCommand(
                                projectId, assignmentId, cycleId, "Ad hoc case", "No requirement link"))))
                .andExpect(status().isCreated());

        verify(authorization)
                .require(
                        any(),
                        eq(AuthorizationPolicy.TEST_CASE_CREATE),
                        eq(projectId),
                        eq("TEST_CASE"),
                        eq(null),
                        any());
        verify(testCases).createAdhocManual(any(), any(), any());
    }

    @Test
    void adhocCsvImportRejectsRequirementParameterBeforeService() throws Exception {
        AuthenticatedUser user = user();
        UUID projectId = UUID.randomUUID();
        var csv = new MockMultipartFile(
                "csv",
                "test-cases.csv",
                "text/csv",
                "Test Case Header,Description\r\nAd hoc case,Validate no link\r\n".getBytes());

        mockMvc.perform(multipart("/api/v1/test-cases/adhoc:import-csv")
                        .file(csv)
                        .param("projectId", projectId.toString())
                        .param("projectSuiteAssignmentId", UUID.randomUUID().toString())
                        .param("testCycleId", UUID.randomUUID().toString())
                        .param("requirementId", UUID.randomUUID().toString())
                        .header(ApiHeaders.IDEMPOTENCY_KEY, "adhoc-csv-key-1")
                        .with(authentication(token(user))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(testCases);
    }

    private static ApplicationUserAuthenticationToken token(AuthenticatedUser user) {
        return new ApplicationUserAuthenticationToken(user, null, List.of());
    }

    private static AuthenticatedUser user() {
        return new AuthenticatedUser(
                UUID.randomUUID(),
                "tenant-1",
                UUID.randomUUID().toString(),
                "Avery",
                "Tester",
                "avery@example.test",
                false);
    }

    private static TestCaseSummary summary(UUID projectId, UUID assignmentId, UUID cycleId) {
        return new TestCaseSummary(
                UUID.randomUUID(),
                projectId,
                "WFM",
                assignmentId,
                UUID.randomUUID(),
                "Timekeeping",
                cycleId,
                "Cycle 1",
                null,
                null,
                null,
                null,
                "TC-001",
                "Ad hoc case",
                "No requirement link",
                "Draft",
                "MANUAL_ADHOC",
                Instant.now(),
                null,
                null,
                null,
                0);
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
