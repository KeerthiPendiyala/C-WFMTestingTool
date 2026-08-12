package com.ukgqtm.app.api;

import com.ukgqtm.app.requirement.RequirementApplicationService;
import com.ukgqtm.app.requirement.RequirementApplicationService.CreateManualRequirementCommand;
import com.ukgqtm.app.requirement.RequirementApplicationService.RequirementSummary;
import com.ukgqtm.app.requirement.RequirementApplicationService.UpdateRequirementCommand;
import com.ukgqtm.app.security.AuthenticatedPrincipal;
import com.ukgqtm.app.security.AuthorizationPolicy;
import com.ukgqtm.app.security.AuthorizationPolicyService;
import com.ukgqtm.identity.api.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/requirements", produces = MediaType.APPLICATION_JSON_VALUE)
public class RequirementController {
    private final RequirementApplicationService requirements;
    private final AuthorizationPolicyService authorization;

    public RequirementController(
            RequirementApplicationService requirements, AuthorizationPolicyService authorization) {
        this.requirements = requirements;
        this.authorization = authorization;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public RequirementListResponse list(
            Authentication authentication,
            @RequestParam("projectId") UUID projectId,
            HttpServletRequest request) {
        require(authentication, AuthorizationPolicy.PROJECT_VIEW, projectId, null, request);
        AuthenticatedUser user = AuthenticatedPrincipal.require(authentication);
        return new RequirementListResponse(requirements.list(user, projectId));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RequirementSummary> createManual(
            Authentication authentication,
            @Valid @RequestBody CreateManualRequirementCommand command,
            HttpServletRequest request) {
        require(authentication, AuthorizationPolicy.REQUIREMENT_CREATE, command.projectId(), null, request);
        AuthenticatedUser user = AuthenticatedPrincipal.require(authentication);
        RequirementSummary created =
                requirements.createManual(user, command, request.getHeader(ApiHeaders.CORRELATION_ID));
        return ResponseEntity.created(URI.create("/api/v1/requirements/" + created.id())).body(created);
    }

    @PostMapping("/{requirementId}:approve")
    @PreAuthorize("isAuthenticated()")
    public RequirementSummary approve(
            Authentication authentication,
            @PathVariable("requirementId") UUID requirementId,
            @RequestParam("projectId") UUID projectId,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            HttpServletRequest request) {
        require(authentication, AuthorizationPolicy.REQUIREMENT_APPROVE, projectId, requirementId, request);
        AuthenticatedUser user = AuthenticatedPrincipal.require(authentication);
        return requirements.approve(
                user, projectId, requirementId, ifMatch, request.getHeader(ApiHeaders.CORRELATION_ID));
    }

    @PatchMapping(path = "/{requirementId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RequirementSummary update(
            Authentication authentication,
            @PathVariable("requirementId") UUID requirementId,
            @RequestParam("projectId") UUID projectId,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody UpdateRequirementCommand command,
            HttpServletRequest request) {
        require(authentication, AuthorizationPolicy.REQUIREMENT_EDIT, projectId, requirementId, request);
        AuthenticatedUser user = AuthenticatedPrincipal.require(authentication);
        return requirements.update(
                user, projectId, requirementId, command, ifMatch, request.getHeader(ApiHeaders.CORRELATION_ID));
    }

    @DeleteMapping("/{requirementId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(
            Authentication authentication,
            @PathVariable("requirementId") UUID requirementId,
            @RequestParam("projectId") UUID projectId,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            HttpServletRequest request) {
        require(authentication, AuthorizationPolicy.REQUIREMENT_DELETE_UNLINKED, projectId, requirementId, request);
        AuthenticatedUser user = AuthenticatedPrincipal.require(authentication);
        requirements.delete(
                user, projectId, requirementId, ifMatch, request.getHeader(ApiHeaders.CORRELATION_ID));
        return ResponseEntity.noContent().build();
    }

    private void require(
            Authentication authentication,
            AuthorizationPolicy policy,
            UUID projectId,
            UUID requirementId,
            HttpServletRequest request) {
        authorization.require(
                authentication,
                policy,
                projectId,
                "REQUIREMENT",
                requirementId == null ? null : requirementId.toString(),
                request.getHeader(ApiHeaders.CORRELATION_ID));
    }

    public record RequirementListResponse(List<RequirementSummary> requirements) {}
}
