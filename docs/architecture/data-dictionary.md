# Data Dictionary

This dictionary describes the initial PostgreSQL persistence baseline for `AUTH-01`, `AUTH-02`, `RBAC-01` through `RBAC-04`, `PROJ-01`, `REQ-01` through `REQ-04`, `TC-01` through `TC-06`, `P2-01`, `P2-02`, `EXEC-01`, `EXEC-02`, `NFR-01`, and `NFR-02`.

All primary keys are UUIDs. Editable records include optimistic `version` columns. Mutable business records include audited soft-delete columns (`deleted_at`, `deleted_by`) unless the table is an immutable ledger or join/event table.

| Table | Owning Module | Purpose | Key Constraints |
| --- | --- | --- | --- |
| `approved_tenant` | Identity and Authentication | Approved Entra tenant registry for SSO binding. | Application users can bind only to tenant IDs present here. |
| `application_user` | Identity and Authentication | Pre-provisioned users, normalized contact email, Entra tenant/object binding, access state, and last observed display/contact token claims. | Unique active normalized email; unique active Entra tenant/object binding; email and stored token email/preferred username claims must be lowercase. |
| `global_administrator_assignment` | Identity and Authentication | Global Administrator grants. | One active assignment per user. |
| `access_role` | Project Administration | Tenant-scoped named role definitions and descriptions. | Case-insensitive unique name per tenant; one designated administrator role per tenant; optimistic version. |
| `access_role_permission` | Project Administration | The permissions currently inherited by every user assigned to a role. | Unique role/permission pair; permission-name check constraint. |
| `user_access_role_assignment` | Project Administration | Assigns exactly one tenant role to a user without per-user overrides. | Unique tenant/user assignment and tenant-consistent role foreign key. |
| `project` | Project Administration | Tenant-scoped project records. | Unique active project key/name per tenant. |
| `project_membership` | Project Administration | Project role assignment for Test Manager, Test Lead, Test Analyst. | One active membership per project/user; role/status checks. |
| `test_suite` | Project Administration | Reusable test suite catalog. | Unique active suite key per tenant/global scope. |
| `project_suite_assignment` | Project Administration | Assigns reusable suites to projects. | Unique active project/suite assignment. |
| `project_test_cycle` | Project Administration | Project-scoped test cycles. | Unique active cycle name per project; end date cannot precede start date. |
| `project_identifier_counter` | Project Administration | Concurrency-safe allocation for project-scoped `REQ-001` and `TC-001` identifiers. | Primary key project/type; allocation uses PostgreSQL upsert. |
| `uploaded_document` | Requirements | Upload metadata for named and `OTHER` readable requirement-document sources. | Project/suite/cycle FKs; source/status checks; object reference metadata only. |
| `generation_job` | AI Assistant | Provider-neutral generation job metadata. | Idempotency key unique when present; job/source/status checks. |
| `requirement` | Requirements | Project-scoped requirements with `ReqID`, source, status, and approval audit. | Unique active `req_id` and sequence per project; Draft/Approved status only; linked test cases block delete/soft-delete. |
| `predefined_test_case_template` | Test Management | Phase 2 predefined template catalog. | Unique active template key per tenant/global scope. |
| `test_case` | Test Management | Requirement-linked, ad hoc, AI, CSV, and predefined test case metadata. | Optional requirement; unique active `test_case_id` and sequence per project; required status enum; assignee must be active same-project membership; non-Draft delete/soft-delete blocked. |
| `audit_event` | Audit | Immutable audit event ledger. | Indexed by project/time and correlation ID. |
| `outbox_event` | Audit / Platform | Transactional outbox for Replit database-backed jobs and enterprise broker adapters. | Optional idempotency key unique; status check. |
| `async_job` | Audit / Platform | Database-backed asynchronous job table for Replit profile. | Optional idempotency key unique; status check; retry metadata. |
| `feature_flag` | Platform | Runtime flags for future execution/evidence contracts. | Execution/evidence flags default disabled. |
| `execution_run` | Execution Contracts | Future execution run metadata only. | Disabled by default behind `execution.contracts.enabled`; idempotency key unique when present. |
| `test_case_execution` | Execution Contracts | Future per-test-case execution metadata only. | Same-project FK to `test_case`; disabled by default behind `execution.contracts.enabled`. |
| `evidence_artifact` | Evidence | Future evidence artifact metadata and object references. | Project scoped; metadata only; disabled by default behind `evidence.artifacts.enabled`. |
| `evidence_artifact_link` | Evidence | Links evidence artifacts to requirements, test cases, or executions. | Must link at least one target; project-scoped FKs. |

## Identifier Allocation

Project-scoped human identifiers are allocated by `allocate_project_identifier(project_id, identifier_type)`. The function uses an atomic PostgreSQL `INSERT ... ON CONFLICT DO UPDATE ... RETURNING` statement so concurrent callers receive unique integer sequences. Application code formats those numbers as `REQ-001` or `TC-001`.

## Delete Guards

- Requirements cannot be deleted or soft-deleted while active test cases reference them.
- Test cases cannot be deleted or soft-deleted unless their status is exactly `Draft`.
- These protections are database triggers so they apply regardless of API or UI path.

## Isolation Guards

- Project-scoped rows include `tenant_id` and `project_id`; composite foreign keys keep the stored tenant aligned with the owning project.
- Project suite assignments can use only global suites or same-tenant suites.
- Requirement-linked test cases must use the same project suite assignment and cycle as the linked requirement.
- Predefined test cases must have no requirement link and must reference a predefined template.
- Future execution/evidence contract writes remain disabled until their feature flags are enabled.

## Development Seed Data

Development examples live in `db/dev-migration` and are loaded only when the `dev` Flyway location is enabled. Default production migrations do not seed customer examples or credentials.
