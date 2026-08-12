package com.ukgqtm.app.api;

import com.ukgqtm.app.project.ProjectApplicationService;
import com.ukgqtm.app.project.ProjectApplicationService.AddProjectMemberCommand;
import com.ukgqtm.app.project.ProjectApplicationService.ChangeProjectMemberRoleCommand;
import com.ukgqtm.app.project.ProjectApplicationService.CreateProjectCommand;
import com.ukgqtm.app.project.ProjectApplicationService.ProjectMembershipSummary;
import com.ukgqtm.app.project.ProjectApplicationService.ProjectSummary;
import com.ukgqtm.app.security.AuthenticatedPrincipal;
import com.ukgqtm.app.security.AuthorizationPolicy;
import com.ukgqtm.app.security.AuthorizationPolicyService;
import com.ukgqtm.identity.api.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/projects", produces = MediaType.APPLICATION_JSON_VALUE)
public class ProjectController {
    private final ProjectApplicationService projects;
    private final AuthorizationPolicyService authorization;

    public ProjectController(ProjectApplicationService projects, AuthorizationPolicyService authorization) {
        this.projects = projects;
        this.authorization = authorization;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ProjectListResponse list(Authentication authentication) {
        AuthenticatedUser user = AuthenticatedPrincipal.require(authentication);
        List<ProjectSummary> visibleProjects = projects.visibleProjects(user);
        Set<AuthorizationPolicy> capabilities = EnumSet.noneOf(AuthorizationPolicy.class);
        capabilities.addAll(authorization.globalCapabilities(user));
        visibleProjects.forEach(project -> capabilities.addAll(authorization.projectCapabilities(user, project.id())));
        return new ProjectListResponse(
                user.globalAdministrator() ? "All Projects" : "My Projects",
                user.globalAdministrator(),
                user.globalAdministrator(),
                capabilities.stream().map(Enum::name).sorted().toList(),
                visibleProjects);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProjectSummary> create(
            Authentication authentication,
            @Valid @RequestBody CreateProjectCommand command,
            HttpServletRequest request) {
        authorization.require(
                authentication,
                AuthorizationPolicy.PROJECT_CREATE,
                null,
                "PROJECT",
                null,
                request.getHeader(ApiHeaders.CORRELATION_ID));
        AuthenticatedUser user = AuthenticatedPrincipal.require(authentication);
        ProjectSummary created = projects.createProject(user, command, request.getHeader(ApiHeaders.CORRELATION_ID));
        return ResponseEntity.created(URI.create("/api/v1/projects/" + created.id())).body(created);
    }

    @GetMapping("/{projectId}")
    @PreAuthorize("isAuthenticated()")
    public ProjectDetailResponse get(
            Authentication authentication, @PathVariable("projectId") UUID projectId, HttpServletRequest request) {
        authorization.require(
                authentication,
                AuthorizationPolicy.PROJECT_VIEW,
                projectId,
                "PROJECT",
                projectId.toString(),
                request.getHeader(ApiHeaders.CORRELATION_ID));
        AuthenticatedUser user = AuthenticatedPrincipal.require(authentication);
        ProjectSummary project = projects.visibleProject(user, projectId)
                .orElseThrow(() -> new AccessDeniedException("The requested resource is not available."));
        Set<AuthorizationPolicy> capabilities = authorization.projectCapabilities(user, projectId);
        List<ProjectMembershipSummary> memberships = capabilities.contains(AuthorizationPolicy.PROJECT_MANAGE_USERS)
                ? projects.projectDetail(user, projectId).memberships()
                : List.of();
        return new ProjectDetailResponse(
                project,
                capabilities.stream().map(Enum::name).sorted().toList(),
                authorization.projectPermissions(user, projectId).stream().map(Enum::name).sorted().toList(),
                memberships);
    }

    @GetMapping("/{projectId}/memberships")
    @PreAuthorize("isAuthenticated()")
    public ProjectMembershipListResponse listMemberships(
            Authentication authentication, @PathVariable("projectId") UUID projectId, HttpServletRequest request) {
        authorization.require(
                authentication,
                AuthorizationPolicy.PROJECT_MANAGE_USERS,
                projectId,
                "PROJECT",
                projectId.toString(),
                request.getHeader(ApiHeaders.CORRELATION_ID));
        AuthenticatedUser user = AuthenticatedPrincipal.require(authentication);
        return new ProjectMembershipListResponse(projects.listProjectMemberships(user, projectId));
    }

    @PostMapping(path = "/{projectId}/memberships", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProjectMembershipSummary> addMembership(
            Authentication authentication,
            @PathVariable("projectId") UUID projectId,
            @Valid @RequestBody AddProjectMemberCommand command,
            HttpServletRequest request) {
        authorization.require(
                authentication,
                AuthorizationPolicy.PROJECT_MANAGE_USERS,
                projectId,
                "PROJECT",
                projectId.toString(),
                request.getHeader(ApiHeaders.CORRELATION_ID));
        AuthenticatedUser user = AuthenticatedPrincipal.require(authentication);
        ProjectMembershipSummary created =
                projects.addProjectMembership(user, projectId, command, request.getHeader(ApiHeaders.CORRELATION_ID));
        return ResponseEntity.created(URI.create("/api/v1/projects/" + projectId + "/memberships/" + created.id()))
                .body(created);
    }

    @PatchMapping(path = "/{projectId}/memberships/{membershipId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ProjectMembershipSummary changeMembershipRole(
            Authentication authentication,
            @PathVariable("projectId") UUID projectId,
            @PathVariable("membershipId") UUID membershipId,
            @Valid @RequestBody ChangeProjectMemberRoleCommand command,
            HttpServletRequest request) {
        authorization.require(
                authentication,
                AuthorizationPolicy.PROJECT_MANAGE_USERS,
                projectId,
                "PROJECT_MEMBERSHIP",
                membershipId.toString(),
                request.getHeader(ApiHeaders.CORRELATION_ID));
        AuthenticatedUser user = AuthenticatedPrincipal.require(authentication);
        return projects.changeProjectMembershipRole(
                user, projectId, membershipId, command, request.getHeader(ApiHeaders.CORRELATION_ID));
    }

    @DeleteMapping("/{projectId}/memberships/{membershipId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> disableMembership(
            Authentication authentication,
            @PathVariable("projectId") UUID projectId,
            @PathVariable("membershipId") UUID membershipId,
            @RequestParam(name = "allowLastManagerOverride", defaultValue = "false") boolean allowLastManagerOverride,
            HttpServletRequest request) {
        authorization.require(
                authentication,
                AuthorizationPolicy.PROJECT_MANAGE_USERS,
                projectId,
                "PROJECT_MEMBERSHIP",
                membershipId.toString(),
                request.getHeader(ApiHeaders.CORRELATION_ID));
        AuthenticatedUser user = AuthenticatedPrincipal.require(authentication);
        projects.disableProjectMembership(
                user, projectId, membershipId, allowLastManagerOverride, request.getHeader(ApiHeaders.CORRELATION_ID));
        return ResponseEntity.noContent().build();
    }

    public record ProjectListResponse(
            String scopeLabel,
            boolean allProjects,
            boolean canCreateProject,
            List<String> globalCapabilities,
            List<ProjectSummary> projects) {}

    public record ProjectDetailResponse(
            ProjectSummary project,
            List<String> capabilities,
            List<String> permissions,
            List<ProjectMembershipSummary> memberships) {}

    public record ProjectMembershipListResponse(List<ProjectMembershipSummary> memberships) {}
}
