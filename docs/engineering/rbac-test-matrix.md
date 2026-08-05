# RBAC Test Matrix

This maintained matrix covers `RBAC-01` through `RBAC-04` and is expanded as workflow APIs are added. Server-side authorization is authoritative; frontend capability rendering mirrors these policies only for usability.

| Policy | Administrator | Test Manager | Test Lead | Test Analyst | Negative / Isolation Cases |
| --- | --- | --- | --- | --- | --- |
| `PROJECT_CREATE` | Allow all tenants approved for the user | Deny | Deny | Deny | Deny returns RFC 7807 and writes an authorization-denied audit event. |
| `PROJECT_VIEW` | Allow all same-tenant active projects | Allow assigned active projects | Allow assigned active projects | Allow assigned active projects | Guessed project UUID from another project returns generic 403 without existence details. |
| `PROJECT_MANAGE_USERS` | Allow all projects | Allow assigned projects | Deny | Deny | Client-supplied role names are never trusted as authorization input. |
| `PROJECT_MANAGE_SUITES` | Allow all projects | Allow assigned projects | Deny | Deny | Direct API calls by Test Lead/Test Analyst remain denied even if UI button is hidden. |
| `PROJECT_MANAGE_CYCLES` | Allow all projects | Allow assigned projects | Deny | Deny | Cross-project cycle assignment must be rejected by policy and schema constraints. |
| `REQUIREMENT_CREATE` | Allow | Allow assigned projects | Allow assigned projects | Allow assigned projects | Upload and generated-job APIs must require project membership or Administrator. |
| `REQUIREMENT_APPROVE` | Allow | Allow assigned projects | Deny | Deny | Approval denial must not reveal whether the requirement exists outside the project. |
| `REQUIREMENT_DELETE_UNLINKED` | Allow | Allow assigned projects | Allow assigned projects | Allow assigned projects | Linked requirement delete is blocked by persistence constraints. |
| `TEST_CASE_CREATE` | Allow | Allow assigned projects | Allow assigned projects | Allow assigned projects | Requirement-linked cases must remain project/suite/cycle scoped. |
| `TEST_CASE_ASSIGN` | Allow | Allow assigned projects | Allow assigned projects | Allow assigned projects | Assignee must be an active member of the same project; never accept a client-provided role as proof. |
| `TEST_CASE_DELETE_DRAFT` | Allow | Allow assigned projects | Allow assigned projects | Allow assigned projects | Non-Draft delete is blocked by policy and persistence constraints. |
| `PREDEFINED_CASE_GENERATE` | Allow | Allow assigned projects | Deny | Deny | Phase 2 generation must record source and project/suite/cycle authorization. |
| `PREDEFINED_CASE_DELETE` | Allow | Allow assigned projects | Deny | Deny | Follow `CONF-003` until the deletion policy is confirmed. |
| `TEST_CASE_VIEW_EXPORT` | Allow | Allow assigned projects | Allow assigned projects | Allow assigned projects | Bulk export must scope all selected IDs to authorized project filters. |
| `REPORT_VIEW` | Allow | Allow assigned projects | Allow assigned projects | Allow assigned projects | Report projections must not include cross-project records. |
| `UPLOAD_ACCESS` | Allow | Allow assigned projects | Allow assigned projects | Allow assigned projects | File upload/download must require membership and secure content validation. |
| `GENERATION_JOB_ACCESS` | Allow | Allow assigned projects | Allow assigned projects | Allow assigned projects | Jobs execute only against immutable authorized command context. |
| `EXPORT_DOWNLOAD` | Allow | Allow assigned projects | Allow assigned projects | Allow assigned projects | Download URLs require fresh authorization and audit. |
| `AUDIT_VIEW` | Allow | Allow assigned projects | Allow assigned projects | Allow assigned projects | Audit views are project-scoped unless Administrator. |
| `EVIDENCE_ACCESS` | Allow | Allow assigned projects | Allow assigned projects | Allow assigned projects | Evidence metadata and object references are project-scoped and provider-neutral. |

Automated coverage added in this slice:

| Test Surface | Coverage |
| --- | --- |
| Backend policy unit tests | Positive/negative policies by Administrator, Test Manager, Test Lead, Test Analyst; cross-project denied; sensitive denials audited. |
| Backend MVC tests | `GET /api/v1/projects`, `POST /api/v1/projects`, `GET /api/v1/projects/{projectId}` use scoped policy/service paths and RFC 7807 forbidden responses. |
| Architecture tests | App controllers do not depend directly on repositories and protected project controllers depend on the central authorization service. |
| Frontend unit tests | UI-02A renders `My Projects` without Create Project; UI-02B renders `All Projects` with Create Project from backend capability data. |
