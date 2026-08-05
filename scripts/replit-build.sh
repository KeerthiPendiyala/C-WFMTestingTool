#!/usr/bin/env bash
set -euo pipefail
pnpm -C frontend install --frozen-lockfile
pnpm -C frontend build
(cd backend && bash ./mvnw -Dskip.frontend.resources=false -DskipTests package)
