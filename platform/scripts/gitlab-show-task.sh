#!/usr/bin/env bash
# Show task detail including execution IDs (for completion script).
set -euo pipefail
TASK_ID="${1:?task UUID}"
REGISTRY_BASE_URL="${REGISTRY_BASE_URL:-http://localhost:8081}"
curl -sS "${REGISTRY_BASE_URL}/api/v1/tasks/${TASK_ID}" | python3 -m json.tool
