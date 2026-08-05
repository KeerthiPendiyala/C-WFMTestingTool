# API Contract Conventions

This document governs `/api/v1` REST contracts for `AUTH-01` through `NFR-02`. `docs/api/openapi.yaml` is the executable contract; this document explains the conventions future endpoint slices must follow.

## Versioning

- Stable REST endpoints live under `/api/v1`.
- Breaking changes require a new base path such as `/api/v2`.
- Compatible additions may be added to v1.
- Deprecated operations or fields must be marked in OpenAPI and include a replacement note plus a `Deprecation` response header.

## Resource Naming

- Use lower-kebab-case plural resources: `/api/v1/projects/{projectId}/memberships`.
- Use command-style colon actions only when a state transition or generation command is clearer than CRUD: `/api/v1/requirements/{requirementId}:approve`.
- Never expose persistence entities directly. Controllers return DTOs only.

## Requests And Responses

- JSON APIs consume `application/json` and produce `application/json`.
- Error responses use `application/problem+json` and RFC 7807 fields.
- Timestamps are UTC ISO-8601 date-time values such as `2026-07-28T03:30:00Z`.
- Dates are ISO-8601 calendar dates such as `2026-07-28`.
- Enums serialize as exact documented strings. Keep `Inprogress` as the API enum until `CONF-004` changes.
- Every response includes `X-Correlation-Id`; callers may supply it or the server generates it.

## Pagination, Sorting And Filtering

- List endpoints use zero-based `page`, bounded `size`, and a `PageMeta` response.
- Sorting uses repeated `sort=field,asc` or `sort=field,desc`.
- Filters use `filter[field]=value` or `filter[field][op]=value`; allowed operators are `eq`, `contains`, `in`, `gte`, and `lte`.
- Unsupported filters or sort fields return a validation problem.

## Idempotency And Concurrency

- Bulk imports, exports, AI generation, predefined generation, and future execution scheduling require `Idempotency-Key`.
- Editable resources must expose `ETag`; update/delete operations must require `If-Match`.
- Optimistic conflicts return 409 problem details. Missing/failed preconditions return 412.

## Errors And Information Leakage

- 401 means missing, expired, or invalid authentication.
- 403 means the authenticated user is not authorized and must not reveal whether the resource exists.
- 404 may be used for public or already-authorized resource absence, but must still use the generic detail: `The requested resource is not available.`
- Validation, forbidden, hidden-not-found, conflict, upload-job, and download examples live in `docs/api/openapi.yaml`.

## Downloads

- Download APIs authorize every request and return streamed bytes.
- Responses include `Content-Disposition`, `Content-Type`, and `X-Correlation-Id`.
- APIs return provider-neutral file identifiers and never expose raw Azure Blob, SFTP, database, or local filesystem references.

## CORS And Portability

- Packaged Replit and enterprise same-origin deployments require no CORS.
- Split-origin development CORS is disabled unless `API_CORS_ALLOWED_ORIGINS` is explicitly set.
- Never use wildcard CORS with credentials.

## Generation

- Run `pnpm -C frontend generate:api` after editing `docs/api/openapi.yaml`.
- Run `pnpm -C frontend check:api` in CI and before finishing API work.
