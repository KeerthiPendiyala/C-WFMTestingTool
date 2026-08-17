# Data Flow

This document describes core data flows and event boundaries for `AUTH-01` through `AUTH-03`, `REQ-01` through `REQ-04`, `TC-01` through `TC-06`, `VIEW-01`, `VIEW-02`, `P2-01`, `P2-02`, `EXEC-01`, `EXEC-02`, `REPORT-01`, `NFR-01`, and `NFR-02`.

## SSO Login And Binding

```mermaid
sequenceDiagram
    actor User
    participant SPA as React SPA
    participant Auth as Identity Module
    participant Entra as Microsoft Entra ID
    participant DB as PostgreSQL
    participant Audit as Audit Module

    User->>SPA: Open application
    SPA->>Auth: Request production login
    Auth->>Entra: Redirect to Entra authorization endpoint
    Entra-->>Auth: Return token with tenant ID and object ID
    Auth->>DB: Find pre-provisioned user by approved tenant and object ID binding
    alt First successful login for pre-provisioned email
        Auth->>DB: Bind user to tenant ID and immutable object ID
        Auth->>Audit: Emit UserBoundToEntraIdentity
    else Unapproved tenant or unprovisioned user
        Auth-->>SPA: Reject access
        Auth->>Audit: Emit AuthenticationRejected
    end
    Auth-->>SPA: Return authenticated session context
```

Authorization never uses mutable email or preferred username as the key after binding.

## Upload Requirement Flow

```mermaid
sequenceDiagram
    actor Member as Project Member
    participant SPA as React SPA
    participant Req as Requirements Module
    participant Project as Project Administration Module
    participant AI as AI Assistant Module
    participant Evidence as Evidence Module
    participant DB as PostgreSQL
    participant Audit as Audit Module

    Member->>SPA: Select project, suite, cycle and upload a readable document
    SPA->>Req: Submit upload with idempotency key
    Req->>Project: Verify membership and suite/cycle scope
    Req->>Req: Validate file type, size, and parsing contract
    Req->>Evidence: Store upload reference if retention policy requires it
    Req->>AI: Request provider-neutral extraction
    AI-->>Req: Return requirement candidates
    Req->>DB: Persist Draft requirements and outbox events
    Req->>Audit: Emit RequirementImported
    Req-->>SPA: Return created Draft requirements
```

The upload flow must reject unsafe or unauthorized files before AI processing.

## AI Test Generation Flow

```mermaid
sequenceDiagram
    actor Member as Project Member
    participant SPA as React SPA
    participant Test as Test Management Module
    participant Req as Requirements Module
    participant Project as Project Administration Module
    participant AI as AI Assistant Module
    participant DB as PostgreSQL
    participant Audit as Audit Module

    Member->>SPA: Select combined ReqID-header
    SPA->>Test: Generate test cases with idempotency key
    Test->>Project: Verify project membership and suite/cycle scope
    Test->>Req: Load approved or allowed requirement projection
    Test->>AI: Generate test cases from requirement projection
    AI-->>Test: Return generated headers and descriptions
    Test->>DB: Persist Draft test cases with source metadata and outbox events
    Test->>Audit: Emit TestCaseGenerated
    Test-->>SPA: Return generated Draft test cases
```

AI output cannot directly write requirement or test case tables. The owning module validates and persists accepted results.

## Export Flow

```mermaid
sequenceDiagram
    actor Member as Project Member
    participant SPA as React SPA
    participant Reporting as Reporting Module
    participant Test as Test Management Module
    participant Evidence as Evidence Module
    participant Audit as Audit Module

    Member->>SPA: Choose project-first filters and selected rows
    SPA->>Reporting: Request CSV or PDF export
    Reporting->>Test: Query authorized test case projection
    Test-->>Reporting: Return paged or selected project-scoped rows
    Reporting->>Evidence: Store export artifact reference
    Reporting->>Audit: Emit ExportCompleted
    Reporting-->>SPA: Return download reference
```

Exports respect server-side authorization, filters, sorting, pagination, and selected rows.

## Future Execution Flow

```mermaid
sequenceDiagram
    actor Manager as Test Manager
    participant SPA as React SPA
    participant Exec as Execution Contracts Module
    participant Worker as Future Playwright Worker
    participant Conn as Connector Contracts Module
    participant Val as Validation Contracts Module
    participant Evidence as Evidence Module
    participant Audit as Audit Module

    Manager->>SPA: Request future execution
    SPA->>Exec: Submit execution command with secret references
    Exec->>Audit: Emit ExecutionRequested
    Exec-->>Worker: Dispatch command in Phase 3+
    Worker->>Conn: Resolve connector using secret references
    Worker->>Val: Evaluate assertions and reconciliation rules
    Worker->>Evidence: Store screenshots, traces, logs, and API evidence
    Evidence->>Audit: Emit EvidenceRecorded
    Worker-->>Exec: Return execution result
```

Phase 1/2 provides contracts and evidence boundaries only. It must not depend on browser automation to create requirements, create test cases, generate predefined cases, or export data.

## Data Classification

| Data | Classification | Handling |
| --- | --- | --- |
| Entra tenant ID and object ID | Sensitive identity metadata | Store for authorization binding; audit access and changes. |
| First name, last name, email | Personal data | Store for provisioning and display; do not use mutable email as authorization key after binding. |
| Project, suite, cycle, requirement, test case data | Customer project data | Tenant/project scoped; included in audit and export controls. |
| Uploaded requirement documents | Customer confidential content | Validate, scan, parse safely, store only through approved evidence/storage policy. |
| AI prompts and responses | Derived customer content | Provider-neutral handling, project scoped, no secrets, audit metadata. |
| Export artifacts | Customer confidential content | Store with project-scoped access and audit downloads. |
| Evidence screenshots/traces/API payloads | Highly sensitive test evidence | Store behind Evidence provider, strict access control, retention policy, and audit. |
| Secrets and credentials | Secret | Store references only; raw values live in environment variables or approved secret providers. |
