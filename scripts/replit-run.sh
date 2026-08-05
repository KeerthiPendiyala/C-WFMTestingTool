#!/usr/bin/env bash
set -euo pipefail
export SERVER_ADDRESS="${SERVER_ADDRESS:-0.0.0.0}"
export PORT="${PORT:-8080}"
exec java -jar backend/app/target/app-0.1.0-SNAPSHOT.jar

