# RBAC Matrix

Authorization is enforced server-side for every protected action. Frontend role checks only improve usability and must never be the sole control.

The names below are seeded defaults. Administrators may create additional roles and edit the permission sets. Effective access is resolved from `user_access_role_assignment` and `access_role_permission` at request time, so changing a role changes every assigned user's backend access without copying permissions onto users.

Legend: `Allow` means permitted when project scope and membership rules are satisfied. `Deny` means forbidden by default.

| Capability | Administrator | Test Manager | Test Lead | Test Analyst | Notes |
| --- | --- | --- | --- | --- | --- |
| Sign in through production Entra ID SSO | Allow | Allow | Allow | Allow | User must be pre-provisioned and tenant-approved. |
| Use development/mock password form | Dev only | Dev only | Dev only | Dev only | Blocked in production. |
| Create project | Allow | Deny | Deny | Deny | Administrator-only. |
| View project | Allow all projects | Assigned projects | Assigned projects | Assigned projects | No cross-project leakage. |
| Manage project users and roles | Allow | Allow for assigned project | Deny | Deny | Test Manager manages users/roles only within assigned projects. |
| Create or assign test suites | Allow | Allow for assigned project | Deny | Deny | Administrator inherits Test Manager actions. |
| Create or assign test cycles | Allow | Allow for assigned project | Deny | Deny | Administrator inherits Test Manager actions. |
| Create requirement manually | Allow | Allow | Allow | Allow | Must be project member unless Administrator override applies. |
| Create requirement from any readable document | Allow | Allow | Allow | Allow | Secure upload controls required. |
| Approve requirement | Allow | Allow | Deny | Deny | Approval changes Draft to Approved. |
| Delete unlinked requirement | Allow | Allow | Allow | Allow | Baseline follows source rule; audited soft delete. Needs PO confirmation. |
| Create test case from requirement with AI | Allow | Allow | Allow | Allow | Combined ReqID-header input required. |
| Create test case from requirement manually/CSV | Allow | Allow | Allow | Allow | Manual/CSV input includes header and description only. |
| Create ad hoc test case manually/CSV | Allow | Allow | Allow | Allow | ReqID null or blank. |
| Assign test case to project member | Allow | Allow | Allow | Allow | Assignee must be a member of the same project; assignee may be null at creation. |
| Delete Draft test case | Allow | Allow | Allow | Allow | Baseline follows source rule; audited soft delete. Needs PO confirmation. |
| Delete non-Draft test case | Deny | Deny | Deny | Deny | Applies to requirement-linked and ad hoc test cases. |
| Generate predefined test cases | Allow | Allow | Deny | Deny | Phase 2 only. |
| Delete predefined generated test cases | Allow | Allow | Deny | Deny | Follows recorded deletion policy. |
| View/filter test cases | Allow | Allow | Allow | Allow | Project-first filter and project-scoped result set. |
| Export test cases to CSV/PDF | Allow | Allow | Allow | Allow | Export respects filters, selection, and project authorization. |
| View reports | Allow | Allow | Allow | Allow | Report data is project-scoped. |
| Manage enterprise adapters/secrets | Allow | Deny | Deny | Deny | Secrets are references only; no raw secret display. |
