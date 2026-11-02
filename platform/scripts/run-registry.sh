#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
set -a && source .env && set +a
exec mvn -pl workflow-registry -am spring-boot:run
