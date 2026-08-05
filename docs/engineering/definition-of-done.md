# Definition Of Done

A change is done only when the relevant product, architecture, security, and quality requirements are satisfied and evidenced.

## Before Implementation

- Read `AGENTS.md` and the governing docs for the touched area.
- Identify the requirement IDs, screen IDs, ADRs, and quality attributes affected.
- Plan the smallest complete vertical slice that can be verified end to end.
- Confirm or record ambiguities before implementation when a decision would change product behavior.

## During Implementation

- Preserve existing valid work and avoid unrelated refactors.
- Keep frontend work in React, TypeScript, and Material UI.
- Keep backend work in Java 21, Spring Boot, Spring Security, PostgreSQL, Flyway, and OpenAPI-first REST contracts.
- Enforce authorization in backend policies, not only in UI conditionals.
- Keep all project-scoped data isolated.
- Use provider interfaces for AI, evidence, storage, messaging, and integrations.
- Keep Replit and enterprise profiles working according to `DEPLOY-01`.
- Do not store credentials or secrets in test cases, logs, screenshots, fixtures, or docs.

## Verification

Run applicable checks for the touched surface and record exact evidence:

- TypeScript typecheck, lint, and unit tests.
- Java unit and integration tests.
- Flyway migration validation.
- OpenAPI lint/contract checks.
- Playwright E2E tests for affected flows.
- Accessibility checks for affected screens.
- Security and authorization negative tests.
- Upload validation tests when import paths change.
- Export/report tests when CSV, PDF, Excel, or Power BI paths change.
- Observability checks for logs, audit events, and correlation IDs when backend flows change.

If a check cannot be run, state the exact reason and residual risk.

## Done Criteria

- Every changed behavior maps to a requirement ID and, when applicable, a screen ID.
- API, UI, database, authorization, observability, and tests are updated together for the vertical slice.
- No production-only dependency prevents Replit startup.
- No enterprise adapter requirement is removed or hard-coded to one provider.
- Documentation and traceability are updated when behavior, APIs, roles, screens, decisions, or quality attributes change.
- Final response lists files changed, tests run, test results, skipped checks, and any product-owner confirmations still needed.

