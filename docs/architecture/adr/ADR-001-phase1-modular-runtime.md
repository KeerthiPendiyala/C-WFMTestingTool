# ADR-001: Phase 1/2 Modular Runtime

## Status

Accepted.

## Context

The baseline names service responsibilities for Authentication, Project Management, Requirement, Test Management, Execution, Connector, Validation, Evidence, Reporting, and AI Assistant services. It also requires Replit portability, strict module boundaries, OpenAPI-first REST APIs, PostgreSQL, audit events, OpenTelemetry, provider-neutral adapters, and future extraction readiness. Relevant requirements: `ARCH-01`, `DEPLOY-01`, `NFR-01`, and `NFR-02`.

The team must choose whether Phase 1/2 starts as fully distributed microservices or one modular runtime.

## Decision

Phase 1/2 will use one Spring Boot runtime containing strict bounded modules that map one-to-one to the documented services. The codebase must preserve the target microservice architecture through module APIs, domain events, transactional outbox, owned persistence, provider interfaces, and OpenAPI contracts.

Direct cross-module repository access is prohibited.

## Options Considered

| Option | Pros | Cons |
| --- | --- | --- |
| Fully distributed microservices | Runtime isolation, independent scaling, independently deployable services. | More infrastructure, more secrets, service-to-service security, distributed transactions, broker dependency, harder Replit startup, slower Phase 1/2 delivery. |
| Modular monolith with strict boundaries | Faster delivery, simpler transactions, simpler Replit profile, fewer runtime failure points, still extraction-ready. | One process is a broader failure domain; requires discipline and architecture tests to prevent boundary erosion. |

## Consequences

- Java package/module boundaries must align to the module map.
- Database ownership and migration namespaces must be module-aware.
- Cross-module calls must use public services, events, or provider interfaces.
- Outbox events must be introduced with mutating workflows so later extraction does not rewrite business logic.
- Separately deployable workers remain available for enterprise and future execution needs.

## Validation

Architecture tests should fail direct repository imports across module boundaries once code exists. OpenAPI and event contracts should be reviewed before implementing each vertical slice.

