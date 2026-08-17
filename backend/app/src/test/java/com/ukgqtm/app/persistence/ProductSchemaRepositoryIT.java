package com.ukgqtm.app.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ukgqtm.app.UkgQaTestManagementApplication;
import com.ukgqtm.project.repository.ProjectIdentifierCounterRepository;
import com.ukgqtm.requirements.repository.RequirementRepository;
import com.ukgqtm.testmanagement.repository.TestCaseRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(classes = UkgQaTestManagementApplication.class)
@Testcontainers(disabledWithoutDocker = true)
class ProductSchemaRepositoryIT {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("ukgqtm")
            .withUsername("ukgqtm")
            .withPassword("ukgqtm");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ProjectIdentifierCounterRepository identifierCounters;

    @Autowired
    private RequirementRepository requirements;

    @Autowired
    private TestCaseRepository testCases;

    @Test
    void allocatesProjectScopedRequirementIdentifiersConcurrently() throws Exception {
        var context = createProjectContext("ALLOC");
        var pool = Executors.newFixedThreadPool(8);
        try {
            List<Callable<Integer>> allocations = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                allocations.add(() -> identifierCounters.allocate(context.projectId(), "REQ"));
            }

            List<Integer> values = new ArrayList<>();
            for (var future : pool.invokeAll(allocations)) {
                values.add(future.get());
            }

            Collections.sort(values);
            assertThat(values).containsExactlyElementsOf(
                    java.util.stream.IntStream.rangeClosed(1, 20).boxed().toList());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void enforcesProjectScopedRequirementIdentifierUniqueness() {
        var context = createProjectContext("REQUNIQUE");
        insertRequirement(context, UUID.randomUUID(), 1, "REQ-001");

        assertThatThrownBy(() -> insertRequirement(context, UUID.randomUUID(), 2, "REQ-001"))
                .hasRootCauseInstanceOf(org.postgresql.util.PSQLException.class);
    }

    @Test
    void preventsDeletingRequirementWhileLinkedTestCasesExist() {
        var context = createProjectContext("LINKEDREQ");
        UUID requirementId = UUID.randomUUID();
        insertRequirement(context, requirementId, 1, "REQ-001");
        insertTestCase(context, UUID.randomUUID(), requirementId, 1, "TC-001", "Draft", context.membershipId());

        assertThatThrownBy(() -> jdbc.update("UPDATE requirement SET deleted_at = now() WHERE id = ?", requirementId))
                .hasRootCauseMessage("ERROR: requirement cannot be deleted while linked test cases exist");
    }

    @Test
    void preventsDeletingNonDraftTestCases() {
        var context = createProjectContext("NONDRAFT");
        UUID testCaseId = UUID.randomUUID();
        insertTestCase(context, testCaseId, null, 1, "TC-001", "Inprogress", context.membershipId());

        assertThatThrownBy(() -> jdbc.update("UPDATE test_case SET deleted_at = now() WHERE id = ?", testCaseId))
                .hasRootCauseMessage("ERROR: test case can be deleted only while Draft");
    }

    @Test
    void rejectsAssigneeMembershipFromAnotherProject() {
        var firstProject = createProjectContext("ASSIGN-A");
        var secondProject = createProjectContext("ASSIGN-B");

        assertThatThrownBy(() -> insertTestCase(
                        firstProject, UUID.randomUUID(), null, 1, "TC-001", "Draft", secondProject.membershipId()))
                .hasRootCauseMessage("ERROR: test case assignee must be an active member of the same project");
    }

    @Test
    void rejectsTenantMismatchForProjectScopedRecords() {
        var context = createProjectContext("TENANT-A");

        assertThatThrownBy(() -> jdbc.update("""
                        INSERT INTO requirement
                            (id, tenant_id, project_id, project_suite_assignment_id, test_cycle_id,
                             req_sequence, req_id, header, description, source_type)
                        VALUES (?, 'wrong-tenant', ?, ?, ?, 1, 'REQ-001', 'Header', 'Description', 'MANUAL')
                        """,
                        UUID.randomUUID(),
                        context.projectId(),
                        context.suiteAssignmentId(),
                        context.cycleId()))
                .hasRootCauseInstanceOf(org.postgresql.util.PSQLException.class);
    }

    @Test
    void acceptsExpandedRequirementDocumentSourceTypes() {
        var context = createProjectContext("XLSDOC");

        int inserted = jdbc.update("""
                INSERT INTO uploaded_document
                    (tenant_id, project_id, project_suite_assignment_id, test_cycle_id,
                     original_filename, content_type, byte_size, source_type)
                VALUES (?, ?, ?, ?, 'requirements.xls', 'application/vnd.ms-excel', 1024, 'XLS')
                """,
                context.tenantId(),
                context.projectId(),
                context.suiteAssignmentId(),
                context.cycleId());

        assertThat(inserted).isEqualTo(1);

        int otherInserted = jdbc.update("""
                INSERT INTO uploaded_document
                    (tenant_id, project_id, project_suite_assignment_id, test_cycle_id,
                     original_filename, content_type, byte_size, source_type)
                VALUES (?, ?, ?, ?, 'requirements.custom', 'text/plain', 512, 'OTHER')
                """,
                context.tenantId(),
                context.projectId(),
                context.suiteAssignmentId(),
                context.cycleId());

        assertThat(otherInserted).isEqualTo(1);
    }

    @Test
    void rejectsRequirementLinkedTestCaseWithMismatchedSuiteOrCycle() {
        var context = createProjectContext("REQSCOPE");
        UUID requirementId = UUID.randomUUID();
        insertRequirement(context, requirementId, 1, "REQ-001");
        UUID otherCycleId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO project_test_cycle (id, tenant_id, project_id, name)
                VALUES (?, ?, ?, 'Other cycle')
                """, otherCycleId, context.tenantId(), context.projectId());

        assertThatThrownBy(() -> jdbc.update("""
                        INSERT INTO test_case
                            (id, tenant_id, project_id, requirement_id, test_case_sequence, test_case_id,
                             project_suite_assignment_id, test_cycle_id, header, description, status, source_type)
                        VALUES (?, ?, ?, ?, 1, 'TC-001', ?, ?, 'Header', 'Description', 'Draft', 'MANUAL')
                        """,
                        UUID.randomUUID(),
                        context.tenantId(),
                        context.projectId(),
                        requirementId,
                        context.suiteAssignmentId(),
                        otherCycleId))
                .hasRootCauseInstanceOf(org.postgresql.util.PSQLException.class);
    }

    @Test
    void predefinedTestCasesMustBeTemplateBackedAndUnlinkedToRequirements() {
        var context = createProjectContext("PREDEF");

        assertThatThrownBy(() -> insertTestCase(
                        context, UUID.randomUUID(), null, 1, "TC-001", "Draft", context.membershipId(), "PREDEFINED"))
                .hasRootCauseInstanceOf(org.postgresql.util.PSQLException.class);
    }

    @Test
    void futureExecutionContractsRemainDisabledBehindFeatureFlags() {
        var context = createProjectContext("EXECFLAG");

        assertThatThrownBy(() -> jdbc.update("""
                        INSERT INTO execution_run (id, tenant_id, project_id, status)
                        VALUES (?, ?, ?, 'REQUESTED')
                        """, UUID.randomUUID(), context.tenantId(), context.projectId()))
                .hasRootCauseMessage("ERROR: execution contract status changes require an enabled feature flag");
    }

    @Test
    void repositoryQueriesRemainProjectScoped() {
        var firstProject = createProjectContext("ISO-A");
        var secondProject = createProjectContext("ISO-B");
        UUID firstRequirement = UUID.randomUUID();
        UUID secondRequirement = UUID.randomUUID();
        insertRequirement(firstProject, firstRequirement, 1, "REQ-001");
        insertRequirement(secondProject, secondRequirement, 1, "REQ-001");
        insertTestCase(firstProject, UUID.randomUUID(), firstRequirement, 1, "TC-001", "Draft", firstProject.membershipId());
        insertTestCase(secondProject, UUID.randomUUID(), secondRequirement, 1, "TC-001", "Draft", secondProject.membershipId());

        assertThat(requirements.findByProjectIdAndDeletedAtIsNull(firstProject.projectId())).hasSize(1);
        assertThat(requirements.findByProjectIdAndDeletedAtIsNull(secondProject.projectId())).hasSize(1);
        assertThat(testCases.findByProjectIdAndDeletedAtIsNull(firstProject.projectId())).hasSize(1);
        assertThat(testCases.findByProjectIdAndDeletedAtIsNull(secondProject.projectId())).hasSize(1);
    }

    private ProjectContext createProjectContext(String label) {
        String safeLabel = label.replace("-", "");
        String tenantId = "tenant-" + label.toLowerCase();
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID membershipId = UUID.randomUUID();
        UUID suiteId = UUID.randomUUID();
        UUID suiteAssignmentId = UUID.randomUUID();
        UUID cycleId = UUID.randomUUID();

        jdbc.update("""
                INSERT INTO application_user
                    (id, first_name, last_name, normalized_contact_email, access_status)
                VALUES (?, 'Test', 'User', ?, 'ACTIVE')
                """, userId, "user-" + userId + "@example.test");
        jdbc.update("""
                INSERT INTO project (id, tenant_id, project_key, name)
                VALUES (?, ?, ?, ?)
                """, projectId, tenantId, safeLabel.toUpperCase(), "Project " + label);
        jdbc.update("""
                INSERT INTO project_membership (id, tenant_id, project_id, user_id, project_role)
                VALUES (?, ?, ?, ?, 'Test Analyst')
                """, membershipId, tenantId, projectId, userId);
        jdbc.update("""
                INSERT INTO test_suite (id, tenant_id, suite_key, name)
                VALUES (?, ?, ?, ?)
                """, suiteId, tenantId, safeLabel.toUpperCase() + "SUITE", "Suite " + label);
        jdbc.update("""
                INSERT INTO project_suite_assignment (id, tenant_id, project_id, suite_id)
                VALUES (?, ?, ?, ?)
                """, suiteAssignmentId, tenantId, projectId, suiteId);
        jdbc.update("""
                INSERT INTO project_test_cycle (id, tenant_id, project_id, name, start_date, end_date)
                VALUES (?, ?, ?, ?, ?, ?)
                """, cycleId, tenantId, projectId, "Cycle " + label, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));

        return new ProjectContext(tenantId, projectId, membershipId, suiteAssignmentId, cycleId);
    }

    private void insertRequirement(ProjectContext context, UUID requirementId, int sequence, String reqId) {
        jdbc.update("""
                INSERT INTO requirement
                    (id, tenant_id, project_id, project_suite_assignment_id, test_cycle_id,
                     req_sequence, req_id, header, description, source_type)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'Requirement header', 'Requirement description', 'MANUAL')
                """,
                requirementId,
                context.tenantId(),
                context.projectId(),
                context.suiteAssignmentId(),
                context.cycleId(),
                sequence,
                reqId);
    }

    private void insertTestCase(
            ProjectContext context,
            UUID testCaseUuid,
            UUID requirementId,
            int sequence,
            String testCaseId,
            String status,
            UUID assigneeMembershipId) {
        insertTestCase(context, testCaseUuid, requirementId, sequence, testCaseId, status, assigneeMembershipId, "MANUAL");
    }

    private void insertTestCase(
            ProjectContext context,
            UUID testCaseUuid,
            UUID requirementId,
            int sequence,
            String testCaseId,
            String status,
            UUID assigneeMembershipId,
            String sourceType) {
        jdbc.update("""
                INSERT INTO test_case
                    (id, tenant_id, project_id, requirement_id, test_case_sequence, test_case_id,
                     project_suite_assignment_id, test_cycle_id, assignee_membership_id,
                     header, description, status, source_type)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'Test case header', 'Test case description', ?, ?)
                """,
                testCaseUuid,
                context.tenantId(),
                context.projectId(),
                requirementId,
                sequence,
                testCaseId,
                context.suiteAssignmentId(),
                context.cycleId(),
                assigneeMembershipId,
                status,
                sourceType);
    }

    private record ProjectContext(
            String tenantId,
            UUID projectId,
            UUID membershipId,
            UUID suiteAssignmentId,
            UUID cycleId) {}
}
