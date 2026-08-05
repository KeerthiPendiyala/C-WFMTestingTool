package com.ukgqtm.app.requirement;

import com.ukgqtm.ai.api.RequirementGenerationProvider;
import com.ukgqtm.ai.api.RequirementGenerationProvider.GeneratedRequirement;
import com.ukgqtm.ai.api.RequirementGenerationProvider.GenerationRequest;
import com.ukgqtm.app.security.AssignmentScopeAuthorizationService;
import com.ukgqtm.identity.api.AuthenticatedUser;
import com.ukgqtm.project.domain.ProjectSuiteAssignment;
import com.ukgqtm.project.domain.ProjectTestCycle;
import com.ukgqtm.project.repository.ProjectRepository;
import com.ukgqtm.project.repository.ProjectSuiteAssignmentRepository;
import com.ukgqtm.project.repository.ProjectTestCycleRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class RequirementGenerationApplicationService {
    private static final int MAX_REQUIREMENTS = 100;
    private final ProjectRepository projects;
    private final ProjectSuiteAssignmentRepository suiteAssignments;
    private final ProjectTestCycleRepository cycles;
    private final DocumentContentExtractor extractor;
    private final RequirementGenerationProvider provider;
    private final RequirementGenerationPersistenceService persistence;
    private final AssignmentScopeAuthorizationService assignmentScope;

    public RequirementGenerationApplicationService(
            ProjectRepository projects,
            ProjectSuiteAssignmentRepository suiteAssignments,
            ProjectTestCycleRepository cycles,
            DocumentContentExtractor extractor,
            RequirementGenerationProvider provider,
            RequirementGenerationPersistenceService persistence,
            AssignmentScopeAuthorizationService assignmentScope) {
        this.projects = projects;
        this.suiteAssignments = suiteAssignments;
        this.cycles = cycles;
        this.extractor = extractor;
        this.provider = provider;
        this.persistence = persistence;
        this.assignmentScope = assignmentScope;
    }

    public RequirementGenerationResult generate(
            AuthenticatedUser actor, GenerationCommand command, MultipartFile document, String correlationId) {
        requireScope(actor, command);
        DocumentContentExtractor.ExtractedDocument extracted = extractor.extract(document);
        var generated = provider.generate(new GenerationRequest(extracted.filename(), extracted.content()));
        List<GeneratedRequirement> validated = validate(generated.requirements());
        return persistence.save(
                actor, command, extracted, generated.model(), validated, correlationId);
    }

    private void requireScope(AuthenticatedUser actor, GenerationCommand command) {
        projects.findByTenantIdAndIdAndActiveTrueAndDeletedAtIsNull(actor.tenantId(), command.projectId())
                .orElseThrow(() -> new AccessDeniedException("The requested resource is not available."));
        suiteAssignments
                .findByTenantIdAndProjectIdAndIdAndDeletedAtIsNull(
                        actor.tenantId(), command.projectId(), command.projectSuiteAssignmentId())
                .filter(ProjectSuiteAssignment::active)
                .orElseThrow(() -> new AccessDeniedException("The requested resource is not available."));
        cycles.findByTenantIdAndProjectIdAndIdAndDeletedAtIsNull(
                        actor.tenantId(), command.projectId(), command.testCycleId())
                .filter(ProjectTestCycle::active)
                .orElseThrow(() -> new AccessDeniedException("The requested resource is not available."));
        assignmentScope.requireAccess(
                actor,
                command.projectId(),
                command.projectSuiteAssignmentId(),
                command.testCycleId());
    }

    private static List<GeneratedRequirement> validate(List<GeneratedRequirement> requirements) {
        if (requirements == null || requirements.isEmpty()) {
            throw invalidResponse("OpenAI did not identify any requirements in the document.");
        }
        if (requirements.size() > MAX_REQUIREMENTS) {
            throw invalidResponse("OpenAI returned too many requirements in one response.");
        }
        for (GeneratedRequirement requirement : requirements) {
            requireText(requirement == null ? null : requirement.header(), 300, "header");
            requireText(requirement.description(), 20_000, "description");
            requireList(requirement.acceptanceCriteria(), true, "acceptance criteria");
            requireList(requirement.assumptions(), false, "assumptions");
            requireList(requirement.dependencies(), false, "dependencies");
        }
        return List.copyOf(requirements);
    }

    private static void requireText(String value, int maxLength, String field) {
        if (value == null || value.isBlank() || value.length() > maxLength) {
            throw invalidResponse("OpenAI returned an invalid requirement " + field + ".");
        }
    }

    private static void requireList(List<String> values, boolean required, String field) {
        if (values == null || (required && values.isEmpty()) || values.size() > 50) {
            throw invalidResponse("OpenAI returned invalid " + field + ".");
        }
        int combinedLength = 0;
        for (String value : values) {
            if (value == null || value.isBlank() || value.length() > 2_000) {
                throw invalidResponse("OpenAI returned invalid " + field + ".");
            }
            combinedLength += value.length();
        }
        if (combinedLength > 20_000) {
            throw invalidResponse("OpenAI returned too much " + field + " content.");
        }
    }

    private static RequirementGenerationException invalidResponse(String message) {
        return new RequirementGenerationException(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }

    public record GenerationCommand(
            UUID projectId, UUID projectSuiteAssignmentId, UUID testCycleId) {}

    public record RequirementGenerationResult(
            UUID jobId, String documentName, int generatedRequirementCount) {}
}
