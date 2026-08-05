# Architecture Decision Register

This register records implementation defaults and open decisions. Product or architecture changes should update this file and the requirement traceability docs together.

| ID | Decision | Status | Rationale | Consequences / Follow-up |
| --- | --- | --- | --- | --- |
| ADR-0001 | Use a modular monolith for Phase 1 and Phase 2 with extraction-ready modules. | Accepted baseline | Speeds delivery while preserving service boundaries for future extraction. | Keep Java package boundaries, module APIs, and database ownership clear. |
| ADR-0002 | Administrator inherits Test Manager actions across projects. | Accepted baseline | Prompt explicitly sets this unless changed. | Product owner to confirm whether Administrator must also be explicit project member for audit. |
| ADR-0003 | Assignee and due date may be null at test case creation. | Accepted baseline | Prompt requires fields but records this as an implementation default. | Product owner to confirm whether required before approval, execution, or export. |
| ADR-0004 | Use audited soft delete for requirements and test cases. | Accepted baseline | Preserves auditability and traceability while satisfying delete behavior. | Product owner to confirm retention windows and restore behavior. |
| ADR-0005 | AI Assistant is provider-neutral. | Accepted baseline | Avoids binding product behavior to one model/provider and supports enterprise controls. | Store prompts, inputs, outputs, and citations according to audit/privacy policy without leaking secrets. |
| ADR-0006 | Jobs run in-process or database-backed for Replit; enterprise profiles can use RabbitMQ or Azure Service Bus broker adapters. | Accepted baseline | Replit must start without requiring a broker while enterprise deployment keeps durable broker options. | Design job APIs and idempotency so the execution model can change by profile. |
| ADR-0007 | Evidence storage is behind a provider interface. | Accepted baseline | Supports Azure Blob in enterprise and alternate external object storage in portable profiles. | Evidence records store metadata, access policy, and object references, not raw credentials. |
| ADR-0008 | Production authentication uses Entra ID SSO; mock password login is development-only. | Accepted baseline | Source screenshot includes a password form but prompt blocks it in production. | Add profile-level guard and E2E tests before any auth implementation ships. |
| ADR-0009 | Use OpenAPI-first REST contracts. | Accepted baseline | Supports frontend/backend parallel work and extraction-ready service boundaries. | API changes require spec updates and contract tests. |
| ADR-0010 | Preserve Replit and enterprise runtime profiles. | Accepted baseline | Prompt requires one-public-port Replit portability without removing enterprise adapters. | Add `.replit` and `replit.nix` when implementation scaffolding begins. |
| ADR-0011 | First-login SSO binding matches pre-provisioned users by normalized contact claims only until immutable `tid` plus `oid` is bound. | Accepted implementation default | AUTH-02 forbids mutable email as the authorization key after binding but still requires an approved pre-provisioned linking procedure. | Product owner to confirm whether invite tokens or admin approval should replace contact-claim matching before production rollout. |
| ADR-0012 | Local username/password login is Administrator-only, environment-configured and session-backed for development/test. | Accepted implementation default | User requested an Administrator credential while preserving SSO; AUTH-03 permits only development/mock password auth and blocks production use. | Do not commit credential values. Prefer `LOCAL_ADMIN_PASSWORD_HASH`. Keep production startup guard active. |

## Conflicts And Ambiguities

| ID | Topic | Conflict / Ambiguity | Baseline Handling | Product Owner Confirmation Needed |
| --- | --- | --- | --- | --- |
| CONF-001 | Requirement deletion role | Source allows any project member to delete unlinked requirements; stricter enterprise RBAC might prefer manager-only deletion. | Any project member can delete unlinked requirements; audited soft delete. | Confirm role restriction and restore workflow. |
| CONF-002 | Test case deletion role | Source allows any project member to delete Draft test cases. | Any project member can delete Draft test cases; audited soft delete. | Confirm whether creator/assignee/manager constraints are needed. |
| CONF-003 | Phase 2 deletion policy | Prompt says manager can delete under recorded deletion policy but does not define whether non-Draft predefined cases can be deleted. | Use Draft-only deletion unless changed. | Confirm generated-case deletion after status transitions. |
| CONF-004 | Status spelling | Prompt requires `Inprogress`. | API enum remains exactly `Inprogress`. | Confirm whether UI display can show `In Progress`. |
| CONF-005 | Legacy DOC upload | `.doc` is required, but portable conversion support may vary. | Keep as requirement; implementation must document profile-specific conversion capability. | Confirm acceptable fallback when `.doc` conversion is unavailable in Replit. |
| CONF-006 | Future execution UX | Future evidence and Playwright execution are required conceptually but no screen concepts are supplied. | Define contracts and evidence model only in Phase 1/2. | Confirm execution/evidence screens before Phase 3. |
| CONF-007 | Phase 5 wording | Source says Phase 5 is decoupled but references "Phase 4 will be decoupled" in the Phase 5 sentence. | Treat Phase 5 as decoupled from Phases 1-4. | Confirm corrected wording. |
| CONF-008 | Local password form behavior | AUTH-03 requires the screenshot password form to be development/mock only, but does not define a production-equivalent mock token issuer. | Implement a local-only, Administrator-only, session-backed login when explicitly enabled; no custom SPA token format is implemented. | Confirm whether non-Administrator local demo credentials are ever needed. |
