# UKG QA & Test Management - Agent Instructions

This repository is establishing the implementation baseline for the UKG Test Management Tool application. Treat the documents under `docs/product`, `docs/architecture`, and `docs/engineering` as governing product and engineering requirements.

## Standard Codex Completion Contract

Before changing this repository, Codex agents must:

1. Read the governing docs before changes: `AGENTS.md`, product baseline docs, architecture decisions, quality attributes, and definition of done.
2. Plan first for non-trivial work, and keep the plan aligned to the documented requirement IDs and screen IDs.
3. Implement complete vertical slices when feature work is explicitly requested; do not leave intentionally broken paths or hidden TODO dependencies.
4. Enforce server-side authorization for every protected action; UI checks are convenience only.
5. Keep integrations provider-neutral behind documented interfaces, including AI, evidence storage, messaging, object storage, and enterprise adapters.
6. Preserve both Replit and enterprise profiles. Replit must be able to start without Docker Compose, RabbitMQ, Azure Key Vault, Azure Blob Storage, or Application Insights merely to boot.
7. Run all applicable tests and checks for the touched surface: TypeScript, Java, migrations, API contracts, unit, integration, and E2E as applicable.
8. Report exact evidence in the final response: commands run, results, skipped checks, and any known gaps.
9. Never expose secrets, credentials, tokens, connection strings, or customer data in code, logs, fixtures, screenshots, documentation examples, or final responses.
10. Avoid unrelated refactors, formatting churn, dependency churn, or metadata noise.
11. Do not commit, push, open PRs, deploy, or publish unless the user explicitly requests that action.

## Baseline Constraints

- Do not add production feature code unless the user explicitly asks for implementation.
- Preserve existing valid work, source documents, screenshots, and diagrams.
- Tie new requirements, tests, APIs, and screens back to stable IDs from `docs/product/requirements-baseline.md` and `docs/product/ui-screen-map.md`.
- Record conflicts and ambiguities in `docs/architecture/decision-register.md` instead of silently resolving them.
- Keep project-scoped data isolated across the frontend, backend, database, storage, logs, evidence, reports, and AI prompts.

## Repository Commands

Run from the repository root unless noted:

- Install frontend dependencies: `pnpm -C frontend install --frozen-lockfile`
- Generate frontend API contract client: `pnpm -C frontend generate:api`
- Check generated frontend API contract client: `pnpm -C frontend check:api`
- Frontend format check: `pnpm -C frontend format`
- Frontend lint: `pnpm -C frontend lint`
- Frontend type check: `pnpm -C frontend typecheck`
- Frontend unit tests: `pnpm -C frontend test`
- Frontend Playwright smoke tests: `pnpm -C frontend test:e2e`
- Backend validation: `powershell -NoProfile -File scripts/backend.ps1 format`
- Backend unit tests: `powershell -NoProfile -File scripts/backend.ps1 test`
- Backend integration and migration tests: `powershell -NoProfile -File scripts/backend.ps1 integration-test`
- Full production package build: `powershell -NoProfile -File scripts/build.ps1`
- Local backend run: `powershell -NoProfile -File scripts/run-local.ps1`
- Docker run: `docker compose --env-file .env.local -f infra/docker/docker-compose.yml up --build app`
- Replit build: `bash scripts/replit-build.sh`
- Replit run: `bash scripts/replit-run.sh`

Java 21 is required for backend commands. The Maven wrapper under `backend/` downloads Maven 3.9.9 on first use.
