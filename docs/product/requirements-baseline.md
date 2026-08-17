# Requirements Baseline

Source: `docs/source/UKG_QA_Test_Management_Implementation_V5.0 (1).docx` plus the implementation-baseline prompt dated 2026-07-28.

This document is authoritative for Phase 1 and Phase 2 implementation planning. It records product behavior and architecture requirements only; it does not authorize production feature code by itself.

## Scope

Phase 1 covers project setup, pre-provisioned users, SSO, RBAC, requirements, test cases through requirements, ad hoc test cases, and view/export.

Phase 2 covers predefined test case generation by project, suite, and cycle.

Future execution phases are decoupled:

- Phase 3: Timekeeping, Payroll export, Accrual, and Persona.
- Phase 4: Person Import, Business Structure, and Labor Categories.
- Phase 5: other UKG modules and integrations.

Phase 1 and Phase 2 must include extraction-ready contracts and an evidence model, but must not contain a hidden dependency on browser automation.

## Technology Stack

The default stack is:

| Layer | Baseline |
| --- | --- |
| Frontend | React, TypeScript, Material UI |
| Backend | Java 21, Spring Boot |
| API | OpenAPI-first REST APIs |
| Authentication | Microsoft Entra ID |
| Security | Spring Security |
| Database | PostgreSQL with Flyway migrations |
| Storage | Azure Blob Storage through a provider interface |
| Messaging | RabbitMQ or Azure Service Bus through broker adapters |
| Automation | Playwright for future PRO WFM automation |
| Integrations | UKG REST APIs, Boomi APIs, SFTP, database connectors |
| Reporting | Power BI, PDF, Excel |
| Observability | Application Insights and OpenTelemetry |
| Secrets | Azure Key Vault in enterprise profile; environment variables for portable profiles |
| Deployment | Docker containers for enterprise deployment |

Replit portability is mandatory: the production profile must serve the built React SPA and Spring API through one public port, bind to `0.0.0.0` and `PORT`, use external PostgreSQL/object storage, read secrets only from environment variables, and include `.replit` and `replit.nix`. It must not require Docker Compose, RabbitMQ, Azure Key Vault, Azure Blob Storage, or Application Insights merely to start. Enterprise adapters remain available.

## Service Responsibilities

| Service | Responsibility |
| --- | --- |
| Authentication Service | Entra ID authentication, token validation, tenant and immutable object ID binding, JWT/session policy, RBAC inputs |
| Project Management Service | Projects, environments, pre-provisioned users, project roles, suite and cycle assignment |
| Requirement Service | Requirement creation, upload extraction, manual entry, approval workflow, requirement deletion policy |
| Test Management Service | Test suites, test cycles, requirement-linked test cases, ad hoc test cases, predefined test cases, imports |
| Execution Service | Future scheduling, retries, polling, orchestration, and status coordination |
| Connector Service | UKG APIs, Boomi, SFTP, database connectors, and future browser automation adapters |
| Validation Service | Assertions, reconciliation, payroll/timekeeping rules, validation contracts |
| Evidence Service | Logs, screenshots, traces, API evidence, audit trail, object-storage abstraction |
| Reporting Service | Dashboards, Power BI integration, PDF export, Excel export, CSV export |
| AI Assistant Service | Provider-neutral requirement extraction, test generation, predefined generation, failure-analysis assistance |

## Requirement Catalog

| ID | Requirement |
| --- | --- |
| AUTH-01 | Users are pre-provisioned with first name, last name, and email before access. Production login uses Microsoft Entra ID SSO. |
| AUTH-02 | On first successful SSO login, bind the approved user record to the token tenant ID and immutable object ID. Never use mutable email or preferred username as the authorization key. Reject unapproved tenants and unprovisioned users. |
| AUTH-03 | The password form shown in the supplied login screenshot is development/mock only and must be blocked in production. |
| RBAC-01 | Supported roles are global Administrator plus project roles Test Manager, Test Lead, and Test Analyst. |
| RBAC-02 | Only Administrator can create projects. Administrator inherits Test Manager actions across projects unless ADR-0002 is changed. |
| RBAC-03 | Test Manager sees only assigned projects and can manage project users/roles, suites, and cycles for those projects. |
| RBAC-04 | Test Lead and Test Analyst cannot create or assign suites or cycles. |
| RBAC-05 | Administrators manage tenant-scoped roles on an independent Roles & Permissions page. A role has a name, description, and any combination of View, Create, Edit, Execute, Delete, Approve Requirements, and Manage Assignments. |
| RBAC-06 | Every managed user is assigned exactly one role and receives permissions from that role; per-user permission overrides are not supported. Role permission edits change effective frontend and backend access for all assigned users. |
| RBAC-07 | Backend APIs enforce the same effective role permissions as the frontend for create, edit, execute, delete, approval, assignment, and view operations. |
| PROJ-01 | Projects contain assigned users, suites, cycles, and environments needed for UKG QA management. Seed examples may use Australian Broadcasting Corporation and Austin Health. |
| REQ-01 | Any project member can create requirements manually or from any readable document after selecting project, suite, and cycle. |
| REQ-02 | Requirement fields are ReqID, header, description, acceptance criteria, dependencies, suite, cycle, created date, and status. Status is Draft by default. |
| REQ-03 | Only Test Manager or Administrator can approve requirements. |
| REQ-04 | A requirement can be deleted only when no test case is linked to it. Deletion is audited soft delete by default. |
| TC-01 | Any project member can create test cases from requirements by selecting one combined ReqID-header value. Creation modes are AI generation, manual entry, and CSV upload. |
| TC-02 | Manual and CSV requirement-linked input contains only Test Case Header and Description. |
| TC-03 | Test case fields are ReqID, TestCaseID, suite, cycle, project-member assignee, header, description, status, created date, and due date. Assignee and due date may be null at creation. |
| TC-04 | Test case statuses are exactly Draft, Inprogress, Defect, Resolved, Not applicable, and Retest. |
| TC-05 | A test case can be deleted only while its status is Draft. Deletion is audited soft delete by default. |
| TC-06 | Ad hoc test cases are manual or CSV-created without a requirement. ReqID is null or blank and all other test case field, status, assignee, due-date, and deletion rules apply. |
| VIEW-01 | Test case viewing starts with project selection. Suite and cycle filters are dependent on the selected project. Other test-case filters are supported. |
| VIEW-02 | Test case list APIs use server-side pagination and sorting, support row selection, and export selected or filtered results to CSV and PDF. |
| P2-01 | In Phase 2, Test Manager or Administrator can generate predefined test cases by project, suite, and cycle. ReqID is null. Source is tracked. |
| P2-02 | Test Manager or Administrator can delete predefined generated test cases under the recorded deletion policy. |
| EXEC-01 | Future Playwright-based PRO WFM automation may record steps and capture screenshots, traces, and API evidence. Credentials and secrets are references only and are never stored in test cases. |
| EXEC-02 | Evidence is project-scoped, audited, access-controlled, and stored behind a provider interface. |
| INT-01 | Connectors must support UKG REST APIs, Boomi APIs, SFTP, and database connectors behind provider-neutral interfaces. |
| REPORT-01 | Reporting supports Power BI, PDF, Excel, and CSV outputs with project-scoped access controls. |
| UI-REQ-01 | The UI uses a light green Material UI theme and implements the thirteen supplied screen concepts mapped in `ui-screen-map.md`. |
| UI-REQ-02 | Navigation under Test Cases is Through Requirements, Adhoc Test Cases, Pre Defined Test Cases, and View / Export. Requirement Management navigation is Generate Requirements, Add Manually, and Manage Requirements. Roles & Permissions is an independent item directly above Users, followed by Audit Logs. |
| SEED-01 | Seed/example data may use Australian Broadcasting Corporation, Austin Health, Timekeeping, Integration, and Personas. |
| DEPLOY-01 | Replit portability must be preserved as described in the Technology Stack section while enterprise Docker, Key Vault, Azure Blob, broker, and Application Insights adapters remain available. |
| ARCH-01 | Phase 1 and Phase 2 default to a modular monolith with extraction-ready service/module boundaries. |
| NFR-01 | Quality requirements include strict TypeScript, Java package/module boundaries, OpenAPI-first contract discipline, Flyway migrations, accessible UI, structured logs, correlation IDs, OpenTelemetry, and audit events. |
| NFR-02 | Security and reliability requirements include unit/integration/E2E tests, no hard-coded credentials, secure uploads, provider-neutral AI, no cross-project data leakage, server-side authorization, and secret-safe logs. |

## Ambiguities And Product Owner Confirmations

These items are recorded and must not be silently resolved in implementation:

| Topic | Baseline Default | Needs Confirmation |
| --- | --- | --- |
| Administrator project visibility | Administrator inherits Test Manager actions across all projects. | Whether Administrators should also appear as explicit project members for audit/reporting. |
| Requirement deletion actor | Source says any project member can delete unlinked requirements. Baseline keeps that rule but uses audited soft delete. | Whether deletion should be restricted to Test Manager/Administrator. |
| Test case deletion actor | Source says any project member can delete Draft test cases. Baseline keeps that rule but uses audited soft delete. | Whether deletion should be restricted by creator, assignee, or manager role. |
| Requirement approval statuses | Only Draft and Approved are explicit. | Whether rejected/retired statuses are needed. |
| Test case status spelling | Status value is exactly `Inprogress` per prompt. | Whether product wants the more common display spelling `In Progress` while retaining API enum `Inprogress`. |
| Phase 2 deletion policy | Prompt says delete under recorded deletion policy. Baseline applies Draft-only soft delete unless changed. | Whether generated predefined cases can be deleted after non-Draft execution statuses. |
| Assignee and due date | Baseline allows null at creation. | Whether UI must require these before approval, execution, or export. |
| DOC upload support | DOC is required as an input type. | Whether legacy `.doc` conversion is allowed in Replit or only enterprise profile. |
