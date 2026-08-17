package com.ukgqtm.app.requirement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ukgqtm.ai.api.RequirementGenerationProvider.GeneratedRequirement;
import com.ukgqtm.ai.repository.GenerationJobRepository;
import com.ukgqtm.app.requirement.RequirementGenerationApplicationService.GenerationCommand;
import com.ukgqtm.audit.repository.AuditEventRepository;
import com.ukgqtm.identity.api.AuthenticatedUser;
import com.ukgqtm.project.repository.ProjectIdentifierCounterRepository;
import com.ukgqtm.requirements.domain.Requirement;
import com.ukgqtm.requirements.domain.UploadedDocument;
import com.ukgqtm.requirements.repository.RequirementRepository;
import com.ukgqtm.requirements.repository.UploadedDocumentRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RequirementGenerationPersistenceServiceTest {
    @Test
    void savesGeneratedFieldsAgainstTheSelectedScope() {
        UploadedDocumentRepository documents = mock(UploadedDocumentRepository.class);
        GenerationJobRepository jobs = mock(GenerationJobRepository.class);
        RequirementRepository requirements = mock(RequirementRepository.class);
        ProjectIdentifierCounterRepository counters = mock(ProjectIdentifierCounterRepository.class);
        AuditEventRepository auditEvents = mock(AuditEventRepository.class);
        RequirementGenerationPersistenceService service = new RequirementGenerationPersistenceService(
                documents, jobs, requirements, counters, auditEvents);
        AuthenticatedUser actor = new AuthenticatedUser(
                UUID.randomUUID(), "tenant-1", "object-1", "Avery", "Admin", "avery@example.test", true);
        GenerationCommand command =
                new GenerationCommand(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        var extracted = new DocumentContentExtractor.ExtractedDocument(
                "requirements.csv", "CSV", "text/csv", "content", "content".getBytes());
        var generated = new GeneratedRequirement(
                "Clock in",
                "Capture employee time",
                List.of("The timestamp is stored"),
                List.of("The employee is active"),
                List.of("Timekeeping is available"));

        when(documents.save(any(UploadedDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jobs.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(requirements.save(any(Requirement.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(counters.allocate(command.projectId(), "REQ")).thenReturn(9);

        var result = service.save(actor, command, extracted, "configured-model", List.of(generated), "corr-1");

        ArgumentCaptor<Requirement> requirementCaptor = ArgumentCaptor.forClass(Requirement.class);
        verify(requirements).save(requirementCaptor.capture());
        Requirement saved = requirementCaptor.getValue();
        assertThat(saved.projectId()).isEqualTo(command.projectId());
        assertThat(saved.projectSuiteAssignmentId()).isEqualTo(command.projectSuiteAssignmentId());
        assertThat(saved.testCycleId()).isEqualTo(command.testCycleId());
        assertThat(saved.reqId()).isEqualTo("REQ-009");
        assertThat(saved.sourceType()).isEqualTo("AI");
        assertThat(saved.acceptanceCriteria()).contains("The timestamp is stored");
        assertThat(saved.assumptions()).contains("The employee is active");
        assertThat(saved.dependencies()).contains("Timekeeping is available");
        assertThat(result.generatedRequirementCount()).isEqualTo(1);
        verify(auditEvents).save(any());
    }
}
