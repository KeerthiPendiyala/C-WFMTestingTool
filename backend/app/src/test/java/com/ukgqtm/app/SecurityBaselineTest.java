package com.ukgqtm.app;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.http.MediaType;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import com.ukgqtm.app.project.ProjectApplicationService;
import com.ukgqtm.app.project.ProjectStructureApplicationService;
import com.ukgqtm.app.requirement.RequirementApplicationService;
import com.ukgqtm.app.requirement.RequirementGenerationApplicationService;
import com.ukgqtm.app.requirement.RequirementGenerationPersistenceService;
import com.ukgqtm.app.role.RoleApplicationService;
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
class SecurityBaselineTest {
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
    private RoleApplicationService roles;

    @MockBean
    private TestCaseApplicationService testCases;

    @Test
    void healthIsPublic() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk());
    }

    @Test
    void protectedApiRequiresAuthenticationWithProblemDetails() throws Exception {
        mockMvc.perform(get("/api/v1/projects"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }
}
