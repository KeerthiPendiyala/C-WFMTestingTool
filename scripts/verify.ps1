$ErrorActionPreference = "Stop"

pnpm -C frontend install --frozen-lockfile
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

pnpm -C frontend check:api
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

pnpm -C frontend lint
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

pnpm -C frontend typecheck
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

pnpm -C frontend test
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Push-Location backend
try {
    .\mvnw.cmd verify
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
} finally {
    Pop-Location
}
