package com.ukgqtm.app.testcase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ukgqtm.app.api.ApiConflictException;
import com.ukgqtm.app.testcase.PredefinedTestCaseTemplateApplicationService.SavePredefinedTemplateCommand;
import com.ukgqtm.audit.repository.AuditEventRepository;
import com.ukgqtm.identity.api.AuthenticatedUser;
import com.ukgqtm.project.domain.TestSuite;
import com.ukgqtm.project.repository.TestSuiteRepository;
import com.ukgqtm.testmanagement.domain.PredefinedTestCaseTemplate;
import com.ukgqtm.testmanagement.repository.PredefinedTestCaseTemplateRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PredefinedTestCaseTemplateApplicationServiceTest {
    private final PredefinedTestCaseTemplateRepository templates = mock(PredefinedTestCaseTemplateRepository.class);
    private final TestSuiteRepository suites = mock(TestSuiteRepository.class);
    private final AuditEventRepository auditEvents = mock(AuditEventRepository.class);
    private final PredefinedTestCaseTemplateApplicationService service =
            new PredefinedTestCaseTemplateApplicationService(templates, suites, auditEvents);

    @Test
    void createsPredefinedTemplateForSelectedSuiteOnly() {
        Fixture fixture = fixture();
        when(suites.findAvailableSuite(fixture.actor().tenantId(), fixture.suite().id()))
                .thenReturn(Optional.of(fixture.suite()));
        when(templates.existsByTenantIdAndTemplateKeyAndDeletedAtIsNull(
                        "tenant-1", "PD_TIMEKEEPING_VALIDATE_EMPLOYEE_CLOCK_IN"))
                .thenReturn(false);
        when(templates.save(any(PredefinedTestCaseTemplate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var created = service.create(
                fixture.actor(),
                new SavePredefinedTemplateCommand(
                        fixture.suite().id(), " Validate employee clock-in ", " Confirm time entry. "),
                "corr-1");

        assertThat(created.suiteId()).isEqualTo(fixture.suite().id());
        assertThat(created.suiteName()).isEqualTo("PD-Timekeeping");
        assertThat(created.templateKey()).isEqualTo("PD_TIMEKEEPING_VALIDATE_EMPLOYEE_CLOCK_IN");
        assertThat(created.header()).isEqualTo("Validate employee clock-in");
        assertThat(created.description()).isEqualTo("Confirm time entry.");
        assertThat(created.source()).isEqualTo("MANUAL");
        assertThat(created.active()).isTrue();
        verify(auditEvents).save(any());
    }

    @Test
    void rejectsDuplicatePredefinedTemplateHeaderForSuite() {
        Fixture fixture = fixture();
        when(suites.findAvailableSuite(fixture.actor().tenantId(), fixture.suite().id()))
                .thenReturn(Optional.of(fixture.suite()));
        when(templates.existsByTenantIdAndTemplateKeyAndDeletedAtIsNull(
                        "tenant-1", "PD_TIMEKEEPING_VALIDATE_EMPLOYEE_CLOCK_IN"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(
                        fixture.actor(),
                        new SavePredefinedTemplateCommand(
                                fixture.suite().id(), "Validate employee clock-in", "Confirm time entry."),
                        "corr-1"))
                .isInstanceOf(ApiConflictException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void updatesHeaderAndDescriptionWithoutChangingSuite() {
        Fixture fixture = fixture();
        PredefinedTestCaseTemplate template = template(fixture);
        when(templates.findAvailableById("tenant-1", template.id())).thenReturn(Optional.of(template));
        when(suites.findAvailableSuite(fixture.actor().tenantId(), fixture.suite().id()))
                .thenReturn(Optional.of(fixture.suite()));
        when(templates.existsTenantTemplateKeyExcluding(
                        "tenant-1", "PD_TIMEKEEPING_VALIDATE_ROSTER_TOTALS", template.id()))
                .thenReturn(false);

        var updated = service.update(
                fixture.actor(),
                template.id(),
                new SavePredefinedTemplateCommand(
                        fixture.suite().id(), "Validate roster totals", "Confirm totals by pay code."),
                "\"0\"",
                "corr-1");

        assertThat(updated.suiteId()).isEqualTo(fixture.suite().id());
        assertThat(updated.templateKey()).isEqualTo("PD_TIMEKEEPING_VALIDATE_ROSTER_TOTALS");
        assertThat(updated.header()).isEqualTo("Validate roster totals");
        assertThat(updated.description()).isEqualTo("Confirm totals by pay code.");
        verify(auditEvents).save(any());
    }

    @Test
    void rejectsSuiteChangeOnUpdate() {
        Fixture fixture = fixture();
        PredefinedTestCaseTemplate template = template(fixture);
        UUID otherSuiteId = UUID.randomUUID();
        when(templates.findAvailableById("tenant-1", template.id())).thenReturn(Optional.of(template));
        when(suites.findAvailableSuite(fixture.actor().tenantId(), fixture.suite().id()))
                .thenReturn(Optional.of(fixture.suite()));

        assertThatThrownBy(() -> service.update(
                        fixture.actor(),
                        template.id(),
                        new SavePredefinedTemplateCommand(otherSuiteId, "Validate roster totals", "Description"),
                        "\"0\"",
                        "corr-1"))
                .isInstanceOf(ApiConflictException.class)
                .hasMessageContaining("Test Suite cannot be changed");
    }

    @Test
    void rejectsNonPredefinedSuiteForTemplateManagement() {
        Fixture fixture = fixture();
        TestSuite suite = TestSuite.create(
                fixture.actor().tenantId(), "TIMEKEEPING", "Timekeeping", null, fixture.actor().userId());
        when(suites.findAvailableSuite(fixture.actor().tenantId(), suite.id())).thenReturn(Optional.of(suite));

        assertThatThrownBy(() -> service.create(
                        fixture.actor(),
                        new SavePredefinedTemplateCommand(suite.id(), "Validate employee clock-in", "Description"),
                        "corr-1"))
                .isInstanceOf(ApiConflictException.class)
                .hasMessageContaining("must start with PD-");
    }

    @Test
    void softDeletesWithOptimisticLocking() {
        Fixture fixture = fixture();
        PredefinedTestCaseTemplate template = template(fixture);
        when(templates.findAvailableById("tenant-1", template.id())).thenReturn(Optional.of(template));
        when(suites.findAvailableSuite(fixture.actor().tenantId(), fixture.suite().id()))
                .thenReturn(Optional.of(fixture.suite()));

        service.delete(fixture.actor(), template.id(), "\"0\"", "corr-1");

        assertThat(template.active()).isFalse();
        verify(auditEvents).save(any());
    }

    private static PredefinedTestCaseTemplate template(Fixture fixture) {
        return PredefinedTestCaseTemplate.create(
                "tenant-1",
                fixture.suite().id(),
                "PD_TIMEKEEPING_VALIDATE_EMPLOYEE_CLOCK_IN",
                "Validate employee clock-in",
                "Confirm time entry.",
                fixture.actor().userId());
    }

    private static Fixture fixture() {
        UUID actorId = UUID.randomUUID();
        AuthenticatedUser actor = new AuthenticatedUser(
                actorId, "tenant-1", "object-1", "Avery", "Admin", "avery@example.test", true);
        TestSuite suite = TestSuite.create("tenant-1", "PD_TIMEKEEPING", "PD-Timekeeping", null, actorId);
        return new Fixture(actor, suite);
    }

    private record Fixture(AuthenticatedUser actor, TestSuite suite) {}
}
