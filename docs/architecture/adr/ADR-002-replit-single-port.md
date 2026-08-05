# ADR-002: Replit Single-Port Runtime

## Status

Accepted.

## Context

`DEPLOY-01` requires a production profile that serves the built React SPA and Spring API through one public port, binds to `0.0.0.0` and `PORT`, uses external PostgreSQL/object storage, reads secrets only from environment variables, and includes `.replit` and `replit.nix`. Replit must not require Docker Compose, RabbitMQ, Azure Key Vault, Azure Blob Storage, or Application Insights merely to start.

## Decision

The Replit profile will run one externally exposed Spring Boot web process that serves both the React SPA and REST API. It will use external PostgreSQL, environment variables for secrets, provider-neutral adapters that can run without Azure services, and in-process/database-backed asynchronous jobs for Phase 1/2.

Future browser automation runs in a separately deployable worker and is not required for Phase 1/2 startup.

## Consequences

- The server must bind to `0.0.0.0:${PORT}` in Replit.
- The built SPA must be served by the Spring Boot runtime in the Replit profile.
- Async jobs must be able to run without RabbitMQ or Azure Service Bus.
- Application Insights, Azure Blob Storage, and Azure Key Vault must be optional adapters, not startup prerequisites.
- Enterprise Docker deployment remains supported separately.

## Validation

Replit smoke tests should verify a cold start with only required environment variables and external PostgreSQL configured. Enterprise tests should verify Azure/broker adapters can be enabled without changing business code.

