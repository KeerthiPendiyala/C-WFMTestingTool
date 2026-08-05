param(
    [ValidateSet("format", "test", "integration-test", "package", "run", "migration-test")]
    [string] $Task = "test"
)

$ErrorActionPreference = "Stop"
$backend = Resolve-Path "$PSScriptRoot\..\backend"
$mvnw = Join-Path $backend "mvnw.cmd"

switch ($Task) {
    "format" {
        & $mvnw -q "-DskipTests" validate
    }
    "test" {
        & $mvnw test
    }
    "integration-test" {
        & $mvnw verify
    }
    "migration-test" {
        & $mvnw -pl app "-DskipUnitTests=false" verify
    }
    "package" {
        & $mvnw "-DskipTests" package
    }
    "run" {
        & $mvnw -pl app -am "-DskipTests" install
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
        & $mvnw -pl app spring-boot:run
    }
}

if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
