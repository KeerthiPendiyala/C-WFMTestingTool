package com.ukgqtm.app.api;

import com.ukgqtm.app.security.AuthenticatedPrincipal;
import com.ukgqtm.app.security.AuthorizationPolicyService;
import com.ukgqtm.app.testcase.PredefinedTestCaseTemplateApplicationService;
import com.ukgqtm.app.testcase.PredefinedTestCaseTemplateApplicationService.PredefinedTemplateListResponse;
import com.ukgqtm.app.testcase.PredefinedTestCaseTemplateApplicationService.PredefinedTemplateSummary;
import com.ukgqtm.app.testcase.PredefinedTestCaseTemplateApplicationService.SavePredefinedTemplateCommand;
import com.ukgqtm.identity.api.AuthenticatedUser;
import com.ukgqtm.project.domain.AccessPermission;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
@RequestMapping(path = "/api/v1/predefined-test-case-templates", produces = MediaType.APPLICATION_JSON_VALUE)
public class PredefinedTestCaseTemplateController {
    private final PredefinedTestCaseTemplateApplicationService templates;
    private final AuthorizationPolicyService authorization;

    public PredefinedTestCaseTemplateController(
            PredefinedTestCaseTemplateApplicationService templates, AuthorizationPolicyService authorization) {
        this.templates = templates;
        this.authorization = authorization;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public PredefinedTemplateListResponse list(
            Authentication authentication, @RequestParam("suiteId") UUID suiteId, HttpServletRequest request) {
        requireManage(authentication, AccessPermission.EXECUTE, null, request);
        AuthenticatedUser user = AuthenticatedPrincipal.require(authentication);
        return new PredefinedTemplateListResponse(templates.list(user, suiteId));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PredefinedTemplateSummary> create(
            Authentication authentication,
            @Valid @RequestBody SavePredefinedTemplateCommand command,
            HttpServletRequest request) {
        requireManage(authentication, AccessPermission.EXECUTE, null, request);
        AuthenticatedUser user = AuthenticatedPrincipal.require(authentication);
        PredefinedTemplateSummary created =
                templates.create(user, command, request.getHeader(ApiHeaders.CORRELATION_ID));
        return ResponseEntity.created(URI.create("/api/v1/predefined-test-case-templates/" + created.id()))
                .body(created);
    }

    @PatchMapping(path = "/{templateId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public PredefinedTemplateSummary update(
            Authentication authentication,
            @PathVariable("templateId") UUID templateId,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody SavePredefinedTemplateCommand command,
            HttpServletRequest request) {
        requireManage(authentication, AccessPermission.EXECUTE, templateId, request);
        AuthenticatedUser user = AuthenticatedPrincipal.require(authentication);
        return templates.update(user, templateId, command, ifMatch, request.getHeader(ApiHeaders.CORRELATION_ID));
    }

    @DeleteMapping("/{templateId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(
            Authentication authentication,
            @PathVariable("templateId") UUID templateId,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            HttpServletRequest request) {
        requireManage(authentication, AccessPermission.DELETE, templateId, request);
        AuthenticatedUser user = AuthenticatedPrincipal.require(authentication);
        templates.delete(user, templateId, ifMatch, request.getHeader(ApiHeaders.CORRELATION_ID));
        return ResponseEntity.noContent().build();
    }

    private void requireManage(
            Authentication authentication, AccessPermission permission, UUID templateId, HttpServletRequest request) {
        authorization.requireGlobalOrAnyProjectPermission(
                authentication,
                permission,
                "PREDEFINED_TEST_CASE_TEMPLATE",
                templateId == null ? null : templateId.toString(),
                request.getHeader(ApiHeaders.CORRELATION_ID));
    }
}
