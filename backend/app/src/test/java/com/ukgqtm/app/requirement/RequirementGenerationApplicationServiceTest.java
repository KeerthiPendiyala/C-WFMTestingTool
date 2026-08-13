package com.ukgqtm.app.requirement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ukgqtm.ai.api.RequirementGenerationProvider;
import com.ukgqtm.ai.api.RequirementGenerationProvider.GeneratedRequirement;
import com.ukgqtm.ai.api.RequirementGenerationProvider.GenerationResult;
import com.ukgqtm.app.requirement.RequirementGenerationApplicationService.GenerationCommand;
import com.ukgqtm.app.requirement.RequirementGenerationApplicationService.RequirementGenerationResult;
import com.ukgqtm.app.security.AssignmentScopeAuthorizationService;
import com.ukgqtm.identity.api.AuthenticatedUser;
import com.ukgqtm.project.domain.Project;
import com.ukgqtm.project.domain.ProjectSuiteAssignment;
import com.ukgqtm.project.domain.ProjectTestCycle;
import com.ukgqtm.project.repository.ProjectRepository;
import com.ukgqtm.project.repository.ProjectSuiteAssignmentRepository;
import com.ukgqtm.project.repository.ProjectTestCycleRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class RequirementGenerationApplicationServiceTest {
    @Test
    void validatesScopeAndStructuredRequirementsBeforePersistence() {
        ProjectRepository projects = mock(ProjectRepository.class);
        ProjectSuiteAssignmentRepository assignments = mock(ProjectSuiteAssignmentRepository.class);
        ProjectTestCycleRepository cycles = mock(ProjectTestCycleRepository.class);
        DocumentContentExtractor extractor = mock(DocumentContentExtractor.class);
        RequirementGenerationProvider provider = mock(RequirementGenerationProvider.class);
        RequirementGenerationPersistenceService persistence = mock(RequirementGenerationPersistenceService.class);
        AssignmentScopeAuthorizationService assignmentScope = mock(AssignmentScopeAuthorizationService.class);
        RequirementGenerationApplicationService service = new RequirementGenerationApplicationService(
                projects, assignments, cycles, extractor, provider, persistence, assignmentScope);

        UUID actorId = UUID.randomUUID();
        AuthenticatedUser actor = new AuthenticatedUser(
                actorId, "tenant-1", "object-1", "Avery", "Admin", "avery@example.test", true);
        Project project = Project.create("tenant-1", "WFM", "WFM", null, actorId);
        ProjectSuiteAssignment assignment =
                ProjectSuiteAssignment.create("tenant-1", project.id(), UUID.randomUUID(), actorId);
        ProjectTestCycle cycle = ProjectTestCycle.create(
                "tenant-1", project.id(), "Cycle", LocalDate.now(), LocalDate.now().plusDays(7), null, actorId);
        GenerationCommand command = new GenerationCommand(project.id(), assignment.id(), cycle.id());
        var file = new MockMultipartFile("document", "requirements.csv", "text/csv", "content".getBytes());
        var extracted = new DocumentContentExtractor.ExtractedDocument(
                "requirements.csv", "CSV", "content", "content".getBytes());
        var generated = new GeneratedRequirement(
                "REQ-123 Clock in", "Capture time", List.of("Time is stored"), List.of(), List.of("Employee exists"));
        var cleaned = new GeneratedRequirement(
                "Clock in", "Capture time", List.of("Time is stored"), List.of(), List.of("Employee exists"));
        RequirementGenerationResult expected =
                new RequirementGenerationResult(UUID.randomUUID(), "requirements.csv", 1);

        when(projects.findByTenantIdAndIdAndActiveTrueAndDeletedAtIsNull("tenant-1", project.id()))
                .thenReturn(Optional.of(project));
        when(assignments.findByTenantIdAndProjectIdAndIdAndDeletedAtIsNull(
                        "tenant-1", project.id(), assignment.id()))
                .thenReturn(Optional.of(assignment));
        when(cycles.findByTenantIdAndProjectIdAndIdAndDeletedAtIsNull("tenant-1", project.id(), cycle.id()))
                .thenReturn(Optional.of(cycle));
        when(extractor.extract(file)).thenReturn(extracted);
        when(provider.generate(any())).thenReturn(new GenerationResult("configured-model", List.of(generated)));
        when(persistence.save(actor, command, extracted, "configured-model", List.of(cleaned), "corr-1"))
                .thenReturn(expected);

        assertThat(service.generate(actor, command, file, "corr-1")).isEqualTo(expected);
        verify(provider).generate(any());
        verify(persistence).save(actor, command, extracted, "configured-model", List.of(cleaned), "corr-1");
    }

    @Test
    void rejectsIncompleteAiOutputBeforePersistence() {
        ProjectRepository projects = mock(ProjectRepository.class);
        ProjectSuiteAssignmentRepository assignments = mock(ProjectSuiteAssignmentRepository.class);
        ProjectTestCycleRepository cycles = mock(ProjectTestCycleRepository.class);
        DocumentContentExtractor extractor = mock(DocumentContentExtractor.class);
        RequirementGenerationProvider provider = mock(RequirementGenerationProvider.class);
        RequirementGenerationPersistenceService persistence = mock(RequirementGenerationPersistenceService.class);
        AssignmentScopeAuthorizationService assignmentScope = mock(AssignmentScopeAuthorizationService.class);
        RequirementGenerationApplicationService service = new RequirementGenerationApplicationService(
                projects, assignments, cycles, extractor, provider, persistence, assignmentScope);
        AuthenticatedUser actor = new AuthenticatedUser(
                UUID.randomUUID(), "tenant-1", "object-1", "Avery", "Admin", "avery@example.test", true);
        Project project = mock(Project.class);
        ProjectSuiteAssignment assignment = mock(ProjectSuiteAssignment.class);
        ProjectTestCycle cycle = mock(ProjectTestCycle.class);
        GenerationCommand command =
                new GenerationCommand(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        var file = new MockMultipartFile("document", "requirements.csv", "text/csv", "content".getBytes());
        var extracted = new DocumentContentExtractor.ExtractedDocument(
                "requirements.csv", "CSV", "content", "content".getBytes());

        when(projects.findByTenantIdAndIdAndActiveTrueAndDeletedAtIsNull("tenant-1", command.projectId()))
                .thenReturn(Optional.of(project));
        when(assignments.findByTenantIdAndProjectIdAndIdAndDeletedAtIsNull(
                        "tenant-1", command.projectId(), command.projectSuiteAssignmentId()))
                .thenReturn(Optional.of(assignment));
        when(assignment.active()).thenReturn(true);
        when(cycles.findByTenantIdAndProjectIdAndIdAndDeletedAtIsNull(
                        "tenant-1", command.projectId(), command.testCycleId()))
                .thenReturn(Optional.of(cycle));
        when(cycle.active()).thenReturn(true);
        when(extractor.extract(file)).thenReturn(extracted);
        when(provider.generate(any())).thenReturn(new GenerationResult(
                "configured-model",
                List.of(new GeneratedRequirement(
                        "Clock in", "Capture time", List.of(), List.of(), List.of()))));

        assertThatThrownBy(() -> service.generate(actor, command, file, "corr-1"))
                .isInstanceOf(RequirementGenerationException.class)
                .hasMessageContaining("acceptance criteria");
        verifyNoInteractions(persistence);
    }
}
