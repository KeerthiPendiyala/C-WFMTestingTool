# Requirement Traceability

This table maps every baseline requirement to planned module ownership, OpenAPI REST surface, screen coverage, and planned tests. API paths are planning anchors, not implemented endpoints.

| ID | Planned Module | Planned API / Contract | Screen | Planned Test Evidence |
| --- | --- | --- | --- | --- |
| AUTH-01 | Authentication | MSAL SPA authorization-code PKCE, `GET /api/v1/auth/me` | UI-01 | SSO entry, protected route, pre-provisioned user lookup, production profile blocks local password login |
| AUTH-02 | Authentication | JWT claims contract, first-login binding inside `GET /api/v1/auth/me`, `POST /api/v1/auth/logout` | UI-01 | Signed JWT validation, tenant rejection, immutable object ID binding, email change does not alter authorization |
| AUTH-03 | Authentication, Frontend Shell | profile guard for local auth | UI-01 | Production tests verify no password form submission path and backend startup guard rejects local auth in production |
| RBAC-01 | Authentication, Project Management | RBAC policy matrix, `PATCH /api/v1/users/{userId}` | UI-02 through UI-13 | Role fixture unit tests, Administrator user-edit/password-reset tests, and authorization integration tests |
| RBAC-02 | Project Management | `POST /api/v1/projects`, authorization policy | UI-02, UI-03 | Administrator can create; project roles cannot; Administrator can manage across projects |
| RBAC-03 | Project Management | `GET /api/v1/projects`, authenticated project-permission profile, project membership APIs | UI-02, UI-03, UI-04, UI-05 | Users see assigned projects only; explicit permissions control project operations; Administrator retains full access |
| RBAC-04 | Project Management | suite/cycle create, edit, delete, and assignment APIs | UI-04, UI-05 | Operation-specific permissions are enforced in UI and backend with cross-project denial |
| RBAC-05 | Project Administration | `GET/POST /api/v1/roles`, `PATCH /api/v1/roles/{roleId}` | UI-14 | Administrator creates/edits tenant roles, individual permissions, and Select All |
| RBAC-06 | Identity, Project Administration | user create/update `roleId`, role assignment repositories | UI-03, UI-14 | User forms have a required dynamic role dropdown and no permission overrides; role edits change user summaries/effective access |
| RBAC-07 | Security | authorization policy service and protected project APIs | UI-02 through UI-14 | Backend rejection and frontend action gating tests cover effective role permissions |
| PROJ-01 | Project Management | `GET/POST /api/v1/projects`, `GET /api/v1/projects/{projectId}` | UI-02, UI-03 | Project seed data and project-scoped list tests |
| REQ-01 | Requirement | `POST /api/v1/requirements`, planned `POST /api/v1/requirements:extract` | UI-06, UI-07, UI-08 | Manual creation is API-backed; readable-document extraction remains provider-gated |
| REQ-02 | Requirement | requirement schema and `GET /api/v1/requirements?projectId={projectId}` | UI-07, UI-08, UI-09 | Draft default, allocated `REQ-###`, field persistence, and project-scoped list tests |
| REQ-03 | Requirement | `POST /api/v1/requirements/{requirementId}:approve` | UI-09 | Server-side Test Manager/Admin policy plus optimistic version check |
| REQ-04 | Requirement, Test Management | `DELETE /api/v1/requirements/{requirementId}` | UI-09 | Audited soft delete, linked-test-case rejection, and optimistic version check |
| TC-01 | Test Management, AI Assistant | `POST /api/v1/test-cases:generate-from-requirement`, `POST /api/v1/test-cases` | UI-10 | Combined ReqID-header selection, AI/manual/CSV creation tests |
| TC-02 | Test Management | CSV import contract | UI-10 | CSV accepts only header/description input and rejects extra unsafe fields |
| TC-03 | Test Management | test case schema | UI-10, UI-11, UI-13 | Field persistence, project-member assignee validation, null assignee/due date |
| TC-04 | Test Management | test case status enum | UI-10, UI-11, UI-13 | Exact enum validation for Draft/Inprogress/Defect/Resolved/Not applicable/Retest |
| TC-05 | Test Management | `DELETE /api/v1/test-cases/{testCaseId}` | UI-10, UI-11, UI-13 | Delete Draft only, reject non-Draft, audit soft delete |
| TC-06 | Test Management | `POST /api/v1/test-cases/adhoc`, CSV import contract | UI-11 | Ad hoc manual/CSV create with null ReqID and same delete/status rules |
| VIEW-01 | Test Management, Reporting | `GET /api/v1/test-cases`, filter contract | UI-13 | Project-first filtering, dependent suite/cycle filters, no cross-project leakage |
| VIEW-02 | Reporting | `GET /api/v1/test-cases`, `POST /api/v1/exports/test-cases` | UI-13 | Server pagination/sort, row selection, CSV/PDF export tests |
| P2-01 | Test Management, AI Assistant | `POST /api/v1/predefined-test-cases:generate` | UI-12 | Test Manager/Admin generate by project/suite/cycle, ReqID null, source tracked |
| P2-02 | Test Management | `DELETE /api/v1/test-cases/{testCaseId}` with source policy | UI-12, UI-13 | Predefined generated delete follows deletion policy and audit event |
| EXEC-01 | Execution, Connector, Evidence | execution/evidence contracts, secret reference schema | Future execution screens | Contract tests proving test cases store secret references only |
| EXEC-02 | Evidence | `POST /api/v1/evidence`, `GET /api/v1/evidence/{id}` | Future evidence/report screens | Project-scoped evidence access, audit, provider-interface tests |
| INT-01 | Connector | connector SPI and adapter contracts | Future connector admin screens | UKG, Boomi, SFTP, DB connector contract tests with secret references |
| REPORT-01 | Reporting | `POST /api/v1/reports`, export APIs | UI-13 | Power BI link contract, PDF/Excel/CSV export authorization tests |
| UI-REQ-01 | Frontend Shell | theme tokens and screen registry | UI-01 through UI-13 | Visual regression, accessibility, responsive layout checks |
| UI-REQ-02 | Frontend Navigation | route map | UI-06, UI-10, UI-11, UI-12, UI-13 | Navigation route tests and role-aware menu tests |
| SEED-01 | Project Management, Test Management | seed fixture contract | UI-02 through UI-13 | Seed data loads without hard-coded customer secrets |
| DEPLOY-01 | Deployment, Platform | Replit and enterprise profile contracts | N/A | Replit smoke starts on `PORT`; enterprise profile loads adapters by config |
| ARCH-01 | All backend modules | module boundaries and OpenAPI contracts | N/A | Architecture tests for package boundaries and API ownership |
| NFR-01 | All modules | logging, tracing, migration, OpenAPI, accessibility contracts | All screens | TypeScript strict, Java tests, Flyway validation, OpenAPI lint, a11y tests |
| NFR-02 | All modules | security, upload, AI, tenant/project isolation contracts | All screens | Security tests, upload scanning/validation, provider-neutral AI tests, leakage tests |

## Screen Traceability

| Screen ID | Planned Module | Planned API / Contract | Screen Name | Planned Test Evidence |
| --- | --- | --- | --- | --- |
| UI-01 | Authentication, Frontend Shell | Entra SSO callback, `GET /api/v1/auth/me`, local-auth profile guard | Login - SSO | Production profile hides/blocks password form; SSO tenant/user binding tests |
| UI-02 | Project Management | `GET /api/v1/projects`, `POST /api/v1/projects` | Project Dashboard | Role-specific dashboard visibility and Administrator create-project action |
| UI-03 | Project Management, Authentication | `POST /api/v1/users`, `PATCH /api/v1/users/{userId}`, project membership and permission APIs | Manage Project & Users | Administrator-only user creation/editing and optional password reset, role/status/project/permission assignment, table refresh, validation, and 403 authorization tests |
| UI-04 | Project Management | suite view/create/edit/delete/assignment APIs | Manage Test Suites | Project-scoped permission controls, missing-permission 403s, assignment isolation, and Administrator override |
| UI-05 | Project Management | cycle view/create/edit/delete APIs | Manage Test Cycles | Project-scoped Create/Edit/Delete controls and backend enforcement |
| UI-06 | Requirement, Frontend Navigation | requirement route map | Manage Requirements | Navigation to Generate Requirements, Add Manually, and Manage Requirements |
| UI-07 | Requirement, AI Assistant | `POST /api/v1/requirements:extract` | Upload Requirement Document | Secure upload and extraction tests for named and unrecognized readable formats |
| UI-08 | Requirement | `POST /api/v1/requirements` | Add Requirement Manually | Project/suite/cycle selection plus header, description, acceptance criteria, and dependencies persisted as a Draft |
| UI-09 | Requirement | requirement list, edit, approve, delete APIs | Manage Requirements | API-backed list plus role-gated edit, approve and delete actions |
| UI-10 | Test Management, AI Assistant | requirement-linked test case create/generate/import APIs | Manage Test Cases Through Requirements | ReqID-header selection, AI/manual/CSV creation, status and delete rules |
| UI-11 | Test Management | ad hoc test case create/import APIs | Manage Adhoc Test Cases | Null ReqID, manual/CSV creation, status and delete rules |
| UI-12 | Test Management, AI Assistant | `POST /api/v1/predefined-test-cases:generate` | Generate Pre Defined Test Cases | Test Manager/Admin generation by project/suite/cycle, source tracking |
| UI-13 | Test Management, Reporting | `GET /api/v1/test-cases`, export APIs | View / Export Test Cases | Project-first filters, pagination, sorting, selection, CSV/PDF export |
