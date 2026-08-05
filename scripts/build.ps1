$ErrorActionPreference = "Stop"

pnpm -C frontend install --frozen-lockfile
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

pnpm -C frontend build
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Push-Location backend
try {
    .\mvnw.cmd "-Dskip.frontend.resources=false" package
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
} finally {
    Pop-Location
}
