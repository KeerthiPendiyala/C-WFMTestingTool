package com.ukgqtm.app.requirement;

import com.ukgqtm.ai.api.RequirementGenerationProvider.GeneratedRequirement;
import com.ukgqtm.ai.domain.GenerationJob;
import com.ukgqtm.ai.repository.GenerationJobRepository;
import com.ukgqtm.app.requirement.RequirementGenerationApplicationService.GenerationCommand;
import com.ukgqtm.app.requirement.RequirementGenerationApplicationService.RequirementGenerationResult;
import com.ukgqtm.audit.domain.AuditEvent;
import com.ukgqtm.audit.repository.AuditEventRepository;
import com.ukgqtm.identity.api.AuthenticatedUser;
import com.ukgqtm.project.repository.ProjectIdentifierCounterRepository;
import com.ukgqtm.requirements.domain.Requirement;
import com.ukgqtm.requirements.domain.UploadedDocument;
import com.ukgqtm.requirements.repository.RequirementRepository;
import com.ukgqtm.requirements.repository.UploadedDocumentRepository;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RequirementGenerationPersistenceService {
    private final UploadedDocumentRepository documents;
    private final GenerationJobRepository jobs;
    private final RequirementRepository requirements;
    private final ProjectIdentifierCounterRepository counters;
    private final AuditEventRepository auditEvents;

    public RequirementGenerationPersistenceService(
            UploadedDocumentRepository documents,
            GenerationJobRepository jobs,
            RequirementRepository requirements,
            ProjectIdentifierCounterRepository counters,
            AuditEventRepository auditEvents) {
        this.documents = documents;
        this.jobs = jobs;
        this.requirements = requirements;
        this.counters = counters;
        this.auditEvents = auditEvents;
    }

    @Transactional
    public RequirementGenerationResult save(
            AuthenticatedUser actor,
            GenerationCommand command,
            DocumentContentExtractor.ExtractedDocument extracted,
            String model,
            List<GeneratedRequirement> generated,
            String correlationId) {
        UploadedDocument document = documents.save(UploadedDocument.create(
                actor.tenantId(),
                command.projectId(),
                command.projectSuiteAssignmentId(),
                command.testCycleId(),
                extracted.filename(),
                extracted.contentType(),
                extracted.bytes().length,
                extracted.sourceType(),
                sha256(extracted.bytes()),
                actor.userId()));
        GenerationJob job = jobs.save(GenerationJob.succeeded(
                actor.tenantId(), command.projectId(), model, document.id(), actor.userId()));

        for (GeneratedRequirement item : generated) {
            int sequence = counters.allocate(command.projectId(), "REQ");
            Requirement requirement = requirements.save(Requirement.createGenerated(
                    actor.tenantId(),
                    command.projectId(),
                    command.projectSuiteAssignmentId(),
                    command.testCycleId(),
                    sequence,
                    item.header().trim(),
                    item.description().trim(),
                    join(item.acceptanceCriteria()),
                    join(item.assumptions()),
                    join(item.dependencies()),
                    document.id(),
                    job.id()));
            auditEvents.save(AuditEvent.project(
                    "REQUIREMENT_IMPORTED",
                    actor.userId().toString(),
                    actor.tenantId(),
                    command.projectId(),
                    "REQUIREMENT",
                    requirement.id().toString(),
                    correlationId));
        }
        return new RequirementGenerationResult(job.id(), extracted.filename(), generated.size());
    }

    private static String join(List<String> values) {
        return values.stream().map(String::trim).map(value -> "- " + value).collect(java.util.stream.Collectors.joining("\n"));
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }
}
