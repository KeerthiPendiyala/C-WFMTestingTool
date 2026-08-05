# ER Diagram

This diagram shows the initial persistence baseline. Future execution/evidence contract tables are present as metadata only and remain disabled behind feature flags until a later approved implementation.

```mermaid
erDiagram
    APPROVED_TENANT ||--o{ APPLICATION_USER : approves_binding
    APPLICATION_USER ||--o{ GLOBAL_ADMINISTRATOR_ASSIGNMENT : grants
    APPLICATION_USER ||--o{ PROJECT_MEMBERSHIP : assigned
    PROJECT ||--o{ PROJECT_MEMBERSHIP : has
    PROJECT ||--o{ PROJECT_SUITE_ASSIGNMENT : has
    TEST_SUITE ||--o{ PROJECT_SUITE_ASSIGNMENT : assigned
    PROJECT ||--o{ PROJECT_TEST_CYCLE : has
    PROJECT ||--o{ PROJECT_IDENTIFIER_COUNTER : allocates

    PROJECT ||--o{ UPLOADED_DOCUMENT : scopes
    PROJECT_SUITE_ASSIGNMENT ||--o{ UPLOADED_DOCUMENT : selected
    PROJECT_TEST_CYCLE ||--o{ UPLOADED_DOCUMENT : selected
    UPLOADED_DOCUMENT ||--o{ GENERATION_JOB : starts
    PROJECT ||--o{ GENERATION_JOB : scopes

    PROJECT ||--o{ REQUIREMENT : owns
    PROJECT_SUITE_ASSIGNMENT ||--o{ REQUIREMENT : classifies
    PROJECT_TEST_CYCLE ||--o{ REQUIREMENT : schedules
    UPLOADED_DOCUMENT ||--o{ REQUIREMENT : sources
    GENERATION_JOB ||--o{ REQUIREMENT : generates

    TEST_SUITE ||--o{ PREDEFINED_TEST_CASE_TEMPLATE : catalogs
    PROJECT ||--o{ TEST_CASE : owns
    REQUIREMENT ||--o{ TEST_CASE : links
    PROJECT_SUITE_ASSIGNMENT ||--o{ TEST_CASE : classifies
    PROJECT_TEST_CYCLE ||--o{ TEST_CASE : schedules
    PROJECT_MEMBERSHIP ||--o{ TEST_CASE : assignee
    PREDEFINED_TEST_CASE_TEMPLATE ||--o{ TEST_CASE : sources
    GENERATION_JOB ||--o{ TEST_CASE : generates

    PROJECT ||--o{ AUDIT_EVENT : records
    PROJECT ||--o{ OUTBOX_EVENT : emits
    PROJECT ||--o{ ASYNC_JOB : runs

    FEATURE_FLAG ||--o{ EXECUTION_RUN : gates
    PROJECT ||--o{ EXECUTION_RUN : owns
    EXECUTION_RUN ||--o{ TEST_CASE_EXECUTION : contains
    TEST_CASE ||--o{ TEST_CASE_EXECUTION : executes

    FEATURE_FLAG ||--o{ EVIDENCE_ARTIFACT : gates
    PROJECT ||--o{ EVIDENCE_ARTIFACT : owns
    EVIDENCE_ARTIFACT ||--o{ EVIDENCE_ARTIFACT_LINK : links
    REQUIREMENT ||--o{ EVIDENCE_ARTIFACT_LINK : referenced
    TEST_CASE ||--o{ EVIDENCE_ARTIFACT_LINK : referenced
    TEST_CASE_EXECUTION ||--o{ EVIDENCE_ARTIFACT_LINK : referenced
```
