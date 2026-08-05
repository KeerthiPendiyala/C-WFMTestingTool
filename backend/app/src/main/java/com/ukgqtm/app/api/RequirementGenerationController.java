package com.ukgqtm.app.api;

import com.ukgqtm.app.requirement.RequirementGenerationApplicationService;
import com.ukgqtm.app.requirement.RequirementGenerationApplicationService.GenerationCommand;
import com.ukgqtm.app.requirement.RequirementGenerationApplicationService.RequirementGenerationResult;
import com.ukgqtm.app.security.AuthenticatedPrincipal;
import com.ukgqtm.app.security.AuthorizationPolicy;
import com.ukgqtm.app.security.AuthorizationPolicyService;
import com.ukgqtm.identity.api.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(path = "/api/v1/generation-jobs", produces = MediaType.APPLICATION_JSON_VALUE)
public class RequirementGenerationController {
    private final RequirementGenerationApplicationService generation;
    private final AuthorizationPolicyService authorization;

    public RequirementGenerationController(
            RequirementGenerationApplicationService generation, AuthorizationPolicyService authorization) {
        this.generation = generation;
        this.authorization = authorization;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RequirementGenerationResult> generate(
            Authentication authentication,
            @RequestPart("document") MultipartFile document,
            @RequestParam("projectId") UUID projectId,
            @RequestParam("projectSuiteAssignmentId") UUID projectSuiteAssignmentId,
            @RequestParam("testCycleId") UUID testCycleId,
            HttpServletRequest request) {
        String correlationId = request.getHeader(ApiHeaders.CORRELATION_ID);
        require(authentication, AuthorizationPolicy.REQUIREMENT_CREATE, projectId, correlationId);
        require(authentication, AuthorizationPolicy.UPLOAD_ACCESS, projectId, correlationId);
        require(authentication, AuthorizationPolicy.GENERATION_JOB_ACCESS, projectId, correlationId);
        AuthenticatedUser actor = AuthenticatedPrincipal.require(authentication);
        RequirementGenerationResult result = generation.generate(
                actor,
                new GenerationCommand(projectId, projectSuiteAssignmentId, testCycleId),
                document,
                correlationId);
        return ResponseEntity.created(URI.create("/api/v1/generation-jobs/" + result.jobId())).body(result);
    }

    private void require(
            Authentication authentication, AuthorizationPolicy policy, UUID projectId, String correlationId) {
        authorization.require(authentication, policy, projectId, "GENERATION_JOB", null, correlationId);
    }
}
