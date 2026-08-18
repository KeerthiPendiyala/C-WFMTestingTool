package com.ukgqtm.app.api;

import com.ukgqtm.app.project.ProjectStructureApplicationService;
import com.ukgqtm.app.project.ProjectStructureApplicationService.AssignSuiteCommand;
import com.ukgqtm.app.project.ProjectStructureApplicationService.ProjectCycleSummary;
import com.ukgqtm.app.project.ProjectStructureApplicationService.ProjectSuiteAssignmentSummary;
import com.ukgqtm.app.project.ProjectStructureApplicationService.SaveCycleCommand;
import com.ukgqtm.app.project.ProjectStructureApplicationService.SuiteCatalogSummary;
import com.ukgqtm.app.project.ProjectStructureApplicationService.UpdateSuiteCommand;
import com.ukgqtm.app.security.AuthenticatedPrincipal;
import com.ukgqtm.app.security.AuthorizationPolicy;
import com.ukgqtm.app.security.AuthorizationPolicyService;
import com.ukgqtm.identity.api.AuthenticatedUser;
import com.ukgqtm.project.domain.AccessPermission;
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
@RequestMapping(path = "/api/v1", produces = MediaType.APPLICATION_JSON_VALUE)
public class ProjectStructureController {
    private final ProjectStructureApplicationService structures;
    private final AuthorizationPolicyService authorization;

    public ProjectStructureController(
            ProjectStructureApplicationService structures, AuthorizationPolicyService authorization) {
        this.structures = structures;
        this.authorization = authorization;
    }

    @GetMapping("/suites")
    @PreAuthorize("isAuthenticated()")
    public SuiteCatalogResponse listSuites(
            Authentication authentication,
            @RequestParam(name = "projectId", required = false) UUID projectId,
            HttpServletRequest request) {
        AuthenticatedUser user = AuthenticatedPrincipal.require(authentication);
        if (projectId == null) {
            authorization.requireGlobalOrAnyProjectPermission(
                    authentication,
                    AccessPermission.EXECUTE,
                    "PREDEFINED_TEST_CASE_TEMPLATE",
                    null,
                    request.getHeader(ApiHeaders.CORRELATION_ID));
            return new SuiteCatalogResponse(structures.listAvailableSuiteCatalog(user));
        }
        authorization.require(
                authentication,
                AuthorizationPolicy.PROJECT_VIEW,
                projectId,
                "PROJECT",
                projectId.toString(),
                request.getHeader(ApiHeaders.CORRELATION_ID));
        return new SuiteCatalogResponse(structures.listSuiteCatalog(user, projectId));
    }

    @PatchMapping(path = "/suites/{suiteId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public SuiteCatalogSummary updateSuite(
            Authentication authentication,
            @PathVariable("suiteId") UUID suiteId,
            @RequestParam("projectId") UUID projectId,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody UpdateSuiteCommand command,
            HttpServletRequest request) {
        authorization.require(
                authentication,
                AuthorizationPolicy.SUITE_EDIT,
                projectId,
                "TEST_SUITE",
                suiteId.toString(),
                request.getHeader(ApiHeaders.CORRELATION_ID));
        AuthenticatedUser user = AuthenticatedPrincipal.require(authentication);
        return structures.updateSuite(user, projectId, suiteId, command, ifMatch, request.getHeader(ApiHeaders.CORRELATION_ID));
    }

    @DeleteMapping("/suites/{suiteId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteSuite(
            Authentication authentication,
            @PathVariable("suiteId") UUID suiteId,
            @RequestParam("projectId") UUID projectId,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            HttpServletRequest request) {
        authorization.require(
                authentication,
                AuthorizationPolicy.SUITE_DELETE,
                projectId,
                "TEST_SUITE",
                suiteId.toString(),
                request.getHeader(ApiHeaders.CORRELATION_ID));
        AuthenticatedUser user = AuthenticatedPrincipal.require(authentication);
        structures.deleteSuite(user, projectId, suiteId, ifMatch, request.getHeader(ApiHeaders.CORRELATION_ID));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/projects/{projectId}/suite-assignments")
    @PreAuthorize("isAuthenticated()")
    public ProjectSuiteAssignmentListResponse listSuiteAssignments(
            Authentication authentication, @PathVariable("projectId") UUID projectId, HttpServletRequest request) {
        authorization.require(
                authentication,
                AuthorizationPolicy.PROJECT_VIEW,
                projectId,
                "PROJECT",
                projectId.toString(),
                request.getHeader(ApiHeaders.CORRELATION_ID));
        AuthenticatedUser user = AuthenticatedPrincipal.require(authentication);
        return new ProjectSuiteAssignmentListResponse(structures.listProjectSuiteAssignments(user, projectId));
    }

    @PostMapping(path = "/projects/{projectId}/suite-assignments", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProjectSuiteAssignmentSummary> assignSuite(
            Authentication authentication,
            @PathVariable("projectId") UUID projectId,
            @Valid @RequestBody AssignSuiteCommand command,
            HttpServletRequest request) {
        authorization.require(
                authentication,
                AuthorizationPolicy.SUITE_CREATE,
                projectId,
                "PROJECT",
                projectId.toString(),
                request.getHeader(ApiHeaders.CORRELATION_ID));
        AuthenticatedUser user = AuthenticatedPrincipal.require(authentication);
        ProjectSuiteAssignmentSummary assignment =
                structures.createOrAssignSuite(user, projectId, command, request.getHeader(ApiHeaders.CORRELATION_ID));
        return ResponseEntity.created(
                        URI.create("/api/v1/projects/" + projectId + "/suite-assignments/" + assignment.id()))
                .body(assignment);
    }

    @DeleteMapping("/projects/{projectId}/suite-assignments/{assignmentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> unassignSuite(
            Authentication authentication,
            @PathVariable("projectId") UUID projectId,
            @PathVariable("assignmentId") UUID assignmentId,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            HttpServletRequest request) {
        authorization.require(
                authentication,
                AuthorizationPolicy.SUITE_MANAGE_ASSIGNMENTS,
                projectId,
                "PROJECT_SUITE_ASSIGNMENT",
                assignmentId.toString(),
                request.getHeader(ApiHeaders.CORRELATION_ID));
        AuthenticatedUser user = AuthenticatedPrincipal.require(authentication);
        structures.unassignSuite(user, projectId, assignmentId, ifMatch, request.getHeader(ApiHeaders.CORRELATION_ID));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/projects/{projectId}/cycles")
    @PreAuthorize("isAuthenticated()")
    public ProjectCycleListResponse listCycles(
            Authentication authentication, @PathVariable("projectId") UUID projectId, HttpServletRequest request) {
        authorization.require(
                authentication,
                AuthorizationPolicy.PROJECT_VIEW,
                projectId,
                "PROJECT",
                projectId.toString(),
                request.getHeader(ApiHeaders.CORRELATION_ID));
        AuthenticatedUser user = AuthenticatedPrincipal.require(authentication);
        return new ProjectCycleListResponse(structures.listProjectCycles(user, projectId));
    }

    @PostMapping(path = "/projects/{projectId}/cycles", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProjectCycleSummary> createCycle(
            Authentication authentication,
            @PathVariable("projectId") UUID projectId,
            @Valid @RequestBody SaveCycleCommand command,
            HttpServletRequest request) {
        authorization.require(
                authentication,
                AuthorizationPolicy.CYCLE_CREATE,
                projectId,
                "PROJECT",
                projectId.toString(),
                request.getHeader(ApiHeaders.CORRELATION_ID));
        AuthenticatedUser user = AuthenticatedPrincipal.require(authentication);
        ProjectCycleSummary cycle = structures.createCycle(user, projectId, command, request.getHeader(ApiHeaders.CORRELATION_ID));
        return ResponseEntity.created(URI.create("/api/v1/projects/" + projectId + "/cycles/" + cycle.id()))
                .body(cycle);
    }

    @PatchMapping(path = "/projects/{projectId}/cycles/{cycleId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ProjectCycleSummary updateCycle(
            Authentication authentication,
            @PathVariable("projectId") UUID projectId,
            @PathVariable("cycleId") UUID cycleId,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody SaveCycleCommand command,
            HttpServletRequest request) {
        authorization.require(
                authentication,
                AuthorizationPolicy.CYCLE_EDIT,
                projectId,
                "PROJECT_TEST_CYCLE",
                cycleId.toString(),
                request.getHeader(ApiHeaders.CORRELATION_ID));
        AuthenticatedUser user = AuthenticatedPrincipal.require(authentication);
        return structures.updateCycle(user, projectId, cycleId, command, ifMatch, request.getHeader(ApiHeaders.CORRELATION_ID));
    }

    @DeleteMapping("/projects/{projectId}/cycles/{cycleId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteCycle(
            Authentication authentication,
            @PathVariable("projectId") UUID projectId,
            @PathVariable("cycleId") UUID cycleId,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            HttpServletRequest request) {
        authorization.require(
                authentication,
                AuthorizationPolicy.CYCLE_DELETE,
                projectId,
                "PROJECT_TEST_CYCLE",
                cycleId.toString(),
                request.getHeader(ApiHeaders.CORRELATION_ID));
        AuthenticatedUser user = AuthenticatedPrincipal.require(authentication);
        structures.deleteCycle(user, projectId, cycleId, ifMatch, request.getHeader(ApiHeaders.CORRELATION_ID));
        return ResponseEntity.noContent().build();
    }

    public record SuiteCatalogResponse(List<SuiteCatalogSummary> suites) {}

    public record ProjectSuiteAssignmentListResponse(List<ProjectSuiteAssignmentSummary> assignments) {}

    public record ProjectCycleListResponse(List<ProjectCycleSummary> cycles) {}
}
