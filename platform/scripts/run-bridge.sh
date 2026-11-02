#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
set -a && source .env && set +a
exec mvn -pl cursor-bridge -am spring-boot:run
