# ADR-003: Provider Adapters

## Status

Accepted.

## Context

The baseline requires provider-neutral AI, evidence storage behind a provider interface, RabbitMQ or Azure Service Bus, Azure Blob Storage, Azure Key Vault, Application Insights/OpenTelemetry, UKG REST APIs, Boomi APIs, SFTP, database connectors, and Replit portability. Relevant requirements: `EXEC-01`, `EXEC-02`, `INT-01`, `REPORT-01`, `DEPLOY-01`, `NFR-01`, and `NFR-02`.

## Decision

Production code will depend on provider interfaces rather than concrete cloud or vendor implementations for:

- AI generation.
- Object/evidence storage.
- Messaging and jobs.
- Secrets.
- Monitoring and telemetry export.
- UKG/Boomi/SFTP/database connectors.
- Future browser execution workers.

Adapters may be selected by profile/configuration. Business modules must not import Azure, broker, AI-provider, or connector SDKs directly.

## Consequences

- Replit can start with environment-secret and database-backed job adapters.
- Enterprise can enable Azure Blob, Key Vault, Application Insights, RabbitMQ, or Azure Service Bus adapters.
- Tests can use fake/in-memory adapters without weakening production security rules.
- Adapter implementations must scrub secrets from logs and expose only stable references to business modules.

## Validation

Dependency checks should reject direct provider SDK imports from business modules. Contract tests should run against fake adapters and at least one production-class adapter per provider type as implementation matures.

