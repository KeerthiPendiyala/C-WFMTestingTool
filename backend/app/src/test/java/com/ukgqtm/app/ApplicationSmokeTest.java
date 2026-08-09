package com.ukgqtm.app;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
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

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration"
})
@AutoConfigureMockMvc
class ApplicationSmokeTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProjectApplicationService projects;

    @MockBean
    private ProjectStructureApplicationService projectStructures;

    @MockBean
    private AuthorizationPolicyService authorization;

    @MockBean
    private RequirementApplicationService requirements;

    @MockBean
    private RequirementGenerationApplicationService requirementGeneration;

    @MockBean
    private RequirementGenerationPersistenceService requirementGenerationPersistence;

    @MockBean
    private AuthStartupGuards authStartupGuards;

    @MockBean
    private AssignmentScopeAuthorizationService assignmentScope;

    @MockBean
    private UserAccessApplicationService userAccess;

    @MockBean
    private TestCaseApplicationService testCases;

    @Test
    void healthEndpointReturnsQuickStatusAndCorrelationId() throws Exception {
        mockMvc.perform(get("/api/v1/health").header("X-Correlation-Id", "test-correlation"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-Id", equalTo("test-correlation")))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("ukg-qa-test-management"));
    }

    @Test
    void readinessEndpointReturnsQuickStatus() throws Exception {
        mockMvc.perform(get("/api/v1/ready"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.check").value("ready"));
    }
}
