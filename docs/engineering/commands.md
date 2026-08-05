# Engineering Commands

Use these commands from the repository root unless noted.

## Install

```powershell
pnpm -C frontend install --frozen-lockfile
```

The backend Maven wrapper downloads Maven 3.9.9 on first use. Java 21 must be installed and `JAVA_HOME` must point to that JDK.

## Format

```powershell
pnpm -C frontend format
powershell -NoProfile -File scripts/backend.ps1 format
```

## Lint And Type Check

```powershell
pnpm -C frontend lint
pnpm -C frontend typecheck
```

## Tests

```powershell
pnpm -C frontend test
pnpm -C frontend test:e2e
powershell -NoProfile -File scripts/backend.ps1 test
powershell -NoProfile -File scripts/backend.ps1 integration-test
```

`integration-test` includes the PostgreSQL migration verification. The Testcontainers migration test is disabled automatically when Docker is unavailable.

## Build

```powershell
powershell -NoProfile -File scripts/build.ps1
```

The production build compiles the SPA and packages it into the Spring Boot runtime so one public port is sufficient.

## Local Run

Start PostgreSQL through Docker Compose with environment values supplied outside the repository:

```powershell
docker compose --env-file .env.local -f infra/docker/docker-compose.yml up postgres
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/run-local.ps1
```

`run-local.ps1` loads `.env.local` without printing values and defaults the backend to the `dev` profile so the development seed data is available. To use the AUTH-03 local Administrator login, set these values in the uncommitted `.env.local`:

```dotenv
LOCAL_AUTH_ENABLED=true
LOCAL_ADMIN_USERNAME=avery.admin@example.test
LOCAL_ADMIN_PASSWORD=<choose-a-development-only-password>
LOCAL_ADMIN_TENANT_ID=dev-tenant
LOCAL_ADMIN_OBJECT_ID=local-admin
VITE_APP_ENV=development
VITE_LOCAL_AUTH_ENABLED=true
```

Prefer `LOCAL_ADMIN_PASSWORD_HASH` over `LOCAL_ADMIN_PASSWORD` when sharing a development environment. Never enable local authentication with `APP_SECURITY_PRODUCTION=true`; the startup guard intentionally rejects that configuration.

Start the Vite frontend in a second terminal:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/frontend.ps1
```

Then check:

```powershell
Invoke-RestMethod http://localhost:8080/api/v1/health
```

## Docker Run

```powershell
docker compose --env-file .env.local -f infra/docker/docker-compose.yml up --build app
```

RabbitMQ is optional and only starts when the `enterprise-adapters` profile is selected.

## Replit

```bash
bash scripts/replit-build.sh
bash scripts/replit-run.sh
```

The Replit runtime binds to `0.0.0.0` and reads `PORT`, defaulting to `8080`.
