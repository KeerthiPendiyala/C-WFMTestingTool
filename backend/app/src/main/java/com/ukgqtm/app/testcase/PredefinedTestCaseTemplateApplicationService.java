package com.ukgqtm.app.testcase;

import com.ukgqtm.app.api.ApiConflictException;
import com.ukgqtm.audit.domain.AuditEvent;
import com.ukgqtm.audit.repository.AuditEventRepository;
import com.ukgqtm.identity.api.AuthenticatedUser;
import com.ukgqtm.project.domain.TestSuite;
import com.ukgqtm.project.repository.TestSuiteRepository;
import com.ukgqtm.testmanagement.domain.PredefinedTestCaseTemplate;
import com.ukgqtm.testmanagement.repository.PredefinedTestCaseTemplateRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PredefinedTestCaseTemplateApplicationService {
    private static final String DISPLAY_PREDEFINED_SUITE_PREFIX = "PD-";
    private static final String KEY_PREDEFINED_SUITE_PREFIX = "PD_";

    private final PredefinedTestCaseTemplateRepository templates;
    private final TestSuiteRepository suites;
    private final AuditEventRepository auditEvents;

    public PredefinedTestCaseTemplateApplicationService(
            PredefinedTestCaseTemplateRepository templates,
            TestSuiteRepository suites,
            AuditEventRepository auditEvents) {
        this.templates = templates;
        this.suites = suites;
        this.auditEvents = auditEvents;
    }

    @Transactional(readOnly = true)
    public List<PredefinedTemplateSummary> list(AuthenticatedUser user, UUID suiteId) {
        TestSuite suite = requirePredefinedSuite(user, suiteId);
        return templates.findAvailableBySuite(user.tenantId(), suite.id()).stream()
                .map(template -> summarize(template, suite))
                .toList();
    }

    @Transactional
    public PredefinedTemplateSummary create(
            AuthenticatedUser actor, SavePredefinedTemplateCommand command, String correlationId) {
        TestSuite suite = requirePredefinedSuite(actor, command.suiteId());
        String header = requireText(command.header(), "Test Case Header", 300);
        String description = requireText(command.description(), "Test Case Description", 20_000);
        String templateKey = templateKey(suite, header);
        if (templates.existsByTenantIdAndTemplateKeyAndDeletedAtIsNull(actor.tenantId(), templateKey)) {
            throw new ApiConflictException("A Pre Defined Test Case with this header already exists for the selected Test Suite.");
        }
        PredefinedTestCaseTemplate template = templates.save(PredefinedTestCaseTemplate.create(
                actor.tenantId(), suite.id(), templateKey, header, description, actor.userId()));
        audit(actor, "PREDEFINED_TEST_CASE_TEMPLATE_CREATED", template.id(), correlationId);
        return summarize(template, suite);
    }

    @Transactional
    public PredefinedTemplateSummary update(
            AuthenticatedUser actor,
            UUID templateId,
            SavePredefinedTemplateCommand command,
            String ifMatch,
            String correlationId) {
        PredefinedTestCaseTemplate template = requireTemplate(actor, templateId);
        requireVersion(template.version(), ifMatch);
        TestSuite suite = requirePredefinedSuite(actor, template.suiteId());
        if (!suite.id().equals(command.suiteId())) {
            throw new ApiConflictException("Test Suite cannot be changed for a Pre Defined Test Case.");
        }
        String header = requireText(command.header(), "Test Case Header", 300);
        String description = requireText(command.description(), "Test Case Description", 20_000);
        String templateKey = templateKey(suite, header);
        if (templates.existsTenantTemplateKeyExcluding(actor.tenantId(), templateKey, template.id())) {
            throw new ApiConflictException("A Pre Defined Test Case with this header already exists for the selected Test Suite.");
        }
        template.update(templateKey, header, description, actor.userId());
        audit(actor, "PREDEFINED_TEST_CASE_TEMPLATE_UPDATED", template.id(), correlationId);
        return summarize(template, suite);
    }

    @Transactional
    public void delete(AuthenticatedUser actor, UUID templateId, String ifMatch, String correlationId) {
        PredefinedTestCaseTemplate template = requireTemplate(actor, templateId);
        requireVersion(template.version(), ifMatch);
        requirePredefinedSuite(actor, template.suiteId());
        template.softDelete(actor.userId());
        audit(actor, "PREDEFINED_TEST_CASE_TEMPLATE_DELETED", template.id(), correlationId);
    }

    private PredefinedTestCaseTemplate requireTemplate(AuthenticatedUser user, UUID templateId) {
        return templates.findAvailableById(user.tenantId(), templateId)
                .orElseThrow(() -> new AccessDeniedException("The requested resource is not available."));
    }

    private TestSuite requireSuite(AuthenticatedUser user, UUID suiteId) {
        return suites.findAvailableSuite(user.tenantId(), suiteId)
                .filter(TestSuite::active)
                .orElseThrow(() -> new AccessDeniedException("The requested resource is not available."));
    }

    private TestSuite requirePredefinedSuite(AuthenticatedUser user, UUID suiteId) {
        TestSuite suite = requireSuite(user, suiteId);
        if (!isPredefinedSuite(suite)) {
            throw new ApiConflictException("Test Suite must start with PD- for Pre Defined Test Cases.");
        }
        return suite;
    }

    private static boolean isPredefinedSuite(TestSuite suite) {
        String name = normalize(suite.name());
        String key = normalize(suite.suiteKey());
        return name.startsWith(DISPLAY_PREDEFINED_SUITE_PREFIX)
                || key.startsWith(DISPLAY_PREDEFINED_SUITE_PREFIX)
                || key.startsWith(KEY_PREDEFINED_SUITE_PREFIX);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private void audit(AuthenticatedUser actor, String action, UUID templateId, String correlationId) {
        auditEvents.save(AuditEvent.project(
                action,
                actor.userId().toString(),
                actor.tenantId(),
                null,
                "PREDEFINED_TEST_CASE_TEMPLATE",
                templateId.toString(),
                correlationId));
    }

    private static String requireText(String value, String label, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new ApiConflictException(label + " is required.");
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new ApiConflictException(label + " must be " + maxLength + " characters or fewer.");
        }
        return trimmed;
    }

    private static String templateKey(TestSuite suite, String header) {
        String headerKey = header.toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (headerKey.isBlank()) {
            headerKey = "TEMPLATE";
        }
        String prefix = suite.suiteKey() == null || suite.suiteKey().isBlank() ? "SUITE" : suite.suiteKey();
        String key = prefix + "_" + headerKey;
        return key.length() <= 120 ? key : key.substring(0, 120).replaceAll("_+$", "");
    }

    private static void requireVersion(int currentVersion, String ifMatch) {
        if (ifMatch == null || ifMatch.isBlank()) {
            throw new ApiConflictException("If-Match is required for this operation.");
        }
        String normalized = ifMatch.replace("\"", "").trim();
        if (String.valueOf(currentVersion).equals(normalized)) {
            return;
        }
        try {
            Integer.parseInt(normalized);
        } catch (NumberFormatException exception) {
            throw new ApiConflictException("If-Match must contain a numeric version.");
        }
        throw new ApiConflictException("The resource has changed. Refresh and retry.");
    }

    public record SavePredefinedTemplateCommand(
            @NotNull UUID suiteId,
            @NotBlank @Size(max = 300) String header,
            @NotBlank @Size(max = 20_000) String description) {}

    public record PredefinedTemplateSummary(
            UUID id,
            UUID suiteId,
            String suiteKey,
            String suiteName,
            String templateKey,
            String header,
            String description,
            String source,
            boolean active,
            int version) {}

    public record PredefinedTemplateListResponse(List<PredefinedTemplateSummary> templates) {}

    private static PredefinedTemplateSummary summarize(PredefinedTestCaseTemplate template, TestSuite suite) {
        return new PredefinedTemplateSummary(
                template.id(),
                suite.id(),
                suite.suiteKey(),
                suite.name(),
                template.templateKey(),
                template.header(),
                template.description(),
                template.source(),
                template.active(),
                template.version());
    }
}
