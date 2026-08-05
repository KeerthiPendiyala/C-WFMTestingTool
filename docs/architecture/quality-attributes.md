# Quality Attributes

Quality attributes are non-functional requirements that apply to every implementation slice.

## Security

- Enforce authorization server-side for every project, suite, cycle, requirement, test case, evidence, report, and admin action.
- Bind users to Entra tenant ID plus immutable object ID. Do not authorize by mutable email or preferred username.
- Reject unapproved tenants, unprovisioned users, and cross-project access.
- Never hard-code credentials. Secrets are references only and are supplied through environment variables, Key Vault, or approved secret providers.
- Validate and scan uploads. Restrict file types to approved contracts and handle PDF/DOCX/DOC/CSV parsing safely.
- Keep AI provider-neutral and prevent prompts, logs, and outputs from leaking secrets or cross-project data.

## Reliability And Recoverability

- Use PostgreSQL with Flyway migrations.
- Use idempotency keys for generation, import, export, evidence, and future execution operations where retries are possible.
- Use audited soft delete for baseline deletion behavior.
- Use structured error responses and OpenAPI-defined validation errors.
- Replit profile starts with in-process/database-backed jobs; enterprise profile may use RabbitMQ or Azure Service Bus.

## Observability

- Emit structured logs with correlation IDs.
- Propagate trace context through frontend, API, jobs, connector calls, evidence capture, and reporting.
- Use OpenTelemetry. Enterprise profile may export to Application Insights.
- Emit audit events for authentication binding, project membership changes, role changes, requirement approval/deletion, test case generation/import/deletion, exports, evidence access, and integration configuration changes.

## Maintainability

- Use strict TypeScript.
- Keep Java 21/Spring Boot package and module boundaries aligned to the service responsibilities in `requirements-baseline.md`.
- Use OpenAPI-first REST API discipline; generated or validated clients must match the spec.
- Avoid hidden dependencies between Phase 1/2 and future browser automation.
- Preserve provider interfaces for AI, evidence storage, object storage, messaging, connectors, and reporting.

## Usability And Accessibility

- Implement the thirteen supplied Material UI screen concepts with a light green theme.
- Meet accessible UI expectations: keyboard navigation, focus states, semantic labels, contrast, table captions/headers, and screen-reader-friendly form errors.
- Project selection must be the first scoping control for project data.
- Dependent filters must not expose inaccessible suite/cycle/test data.

## Testability

- Unit tests cover policy decisions, validation, parsing, service logic, and status transitions.
- Integration tests cover Spring Security, repository boundaries, Flyway migrations, OpenAPI contracts, imports, exports, and provider adapters.
- Playwright E2E tests cover role-specific UI flows, accessible navigation, filtering, export, and production auth guards.
- Tests must include negative cases for unapproved tenants, unprovisioned users, unauthorized roles, cross-project access, unsafe uploads, invalid statuses, and deletion policies.

