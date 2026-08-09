package com.ukgqtm.app.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ukgqtm.app.project.ProjectApplicationService;
import com.ukgqtm.app.project.ProjectStructureApplicationService;
import com.ukgqtm.app.requirement.RequirementApplicationService;
import com.ukgqtm.app.requirement.RequirementGenerationApplicationService;
import com.ukgqtm.app.requirement.RequirementGenerationPersistenceService;
import com.ukgqtm.app.security.AuthorizationPolicyService;
import com.ukgqtm.app.security.AssignmentScopeAuthorizationService;
import com.ukgqtm.app.testcase.TestCaseApplicationService;
import com.ukgqtm.app.user.UserAccessApplicationService;
import com.ukgqtm.app.config.AuthStartupGuards;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration"
})
@AutoConfigureMockMvc
class OpenApiContractTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProjectApplicationService projects;

    @MockBean
    private ProjectStructureApplicationService structures;

    @MockBean
    private AuthorizationPolicyService authorization;

    @MockBean
    private RequirementApplicationService requirements;

    @MockBean
    private RequirementGenerationApplicationService requirementGeneration;

    @MockBean
    private RequirementGenerationPersistenceService requirementGenerationPersistence;

    @MockBean
    private TestCaseApplicationService testCases;

    @MockBean
    private AuthStartupGuards authStartupGuards;

    @MockBean
    private AssignmentScopeAuthorizationService assignmentScope;

    @MockBean
    private UserAccessApplicationService userAccess;

    @Test
    void openApiDefinesV1ConventionsAndImplementedPaths() throws Exception {
        String openApi = Files.readString(Path.of("..", "..", "docs", "api", "openapi.yaml"));

        assertThat(openApi).contains("currentBasePath: /api/v1");
        assertThat(openApi).contains("/api/v1/health:");
        assertThat(openApi).contains("/api/v1/ready:");
        assertThat(openApi).contains("/api/v1/auth/me:");
        assertThat(openApi).contains("/api/v1/auth/logout:");
        assertThat(openApi).contains("/api/v1/projects:");
        assertThat(openApi).contains("/api/v1/projects/{projectId}:");
        assertThat(openApi).contains("/api/v1/projects/{projectId}/memberships:");
        assertThat(openApi).contains("/api/v1/projects/{projectId}/memberships/{membershipId}:");
        assertThat(openApi).contains("/api/v1/suites:");
        assertThat(openApi).contains("/api/v1/suites/{suiteId}:");
        assertThat(openApi).contains("/api/v1/projects/{projectId}/suite-assignments:");
        assertThat(openApi).contains("/api/v1/projects/{projectId}/suite-assignments/{assignmentId}:");
        assertThat(openApi).contains("/api/v1/projects/{projectId}/cycles:");
        assertThat(openApi).contains("/api/v1/projects/{projectId}/cycles/{cycleId}:");
        assertThat(openApi).contains("/api/v1/requirements:");
        assertThat(openApi).contains("/api/v1/generation-jobs:");
        assertThat(openApi).contains("/api/v1/requirements/{requirementId}:approve:");
        assertThat(openApi).contains("/api/v1/requirements/{requirementId}:");
        assertThat(openApi).contains("/api/v1/test-cases:");
        assertThat(openApi).contains("/api/v1/test-cases/adhoc:");
        assertThat(openApi).contains("/api/v1/test-cases/adhoc:import-csv:");
        assertThat(openApi).contains("/api/v1/test-cases/adhoc:csv-sample:");
        assertThat(openApi).contains("/api/v1/test-cases:generate:");
        assertThat(openApi).contains("/api/v1/test-cases:import-csv:");
        assertThat(openApi).contains("/api/v1/test-cases:csv-sample:");
        assertThat(openApi).contains("/api/v1/test-cases/{testCaseId}:");
        assertThat(openApi).contains("Idempotency-Key");
        assertThat(openApi).contains("If-Match");
        assertThat(openApi).contains("FileDownload");
        assertThat(openApi).contains("UploadJobStatus");
    }

    @Test
    void healthBehaviorMatchesOpenApiContract() throws Exception {
        mockMvc.perform(get("/api/v1/health").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().exists(ApiHeaders.CORRELATION_ID))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.check").value("health"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void protectedProjectEndpointMatchesProblemDetailsContract() throws Exception {
        mockMvc.perform(get("/api/v1/projects").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().exists(ApiHeaders.CORRELATION_ID))
                .andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void unknownV1PathReturnsProblemDetailsWithoutRecordLeakage() throws Exception {
        mockMvc.perform(get("/api/v1/does-not-exist").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail", containsString("Authentication is required")));
    }
}
