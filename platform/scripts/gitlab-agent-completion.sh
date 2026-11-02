#!/usr/bin/env bash
# Notify cursor-bridge that an agent finished (triggers n8n completion workflow).
# Usage:
#   ./scripts/gitlab-agent-completion.sh DEVELOPER TASK_UUID EXEC_UUID success \
#     "https://gitlab.com/group/project/-/merge_requests/1"
#
# Roles: DEVELOPER | REVIEWER | TESTER
# Outcomes: success | failed | approved | rejected

set -euo pipefail

ROLE="${1:?role (DEVELOPER|REVIEWER|TESTER)}"
TASK_ID="${2:?taskId UUID}"
EXECUTION_ID="${3:?executionId UUID}"
OUTCOME="${4:?outcome (success|failed|approved|rejected)}"
PR_URL="${5:-}"
SUMMARY="${6:-Completed via gitlab-agent-completion.sh}"

BRIDGE_BASE_URL="${BRIDGE_BASE_URL:-http://localhost:8082}"
IDEM_KEY="${IDEMPOTENCY_KEY:-completion-${EXECUTION_ID}}"

BODY="$(python3 - <<PY
import json, os
print(json.dumps({
  "taskId": os.environ["TASK_ID"],
  "executionId": os.environ["EXECUTION_ID"],
  "role": os.environ["ROLE"],
  "outcome": os.environ["OUTCOME"],
  "idempotencyKey": os.environ["IDEM_KEY"],
  "prUrl": os.environ.get("PR_URL") or None,
  "summary": os.environ.get("SUMMARY") or None,
}, separators=(",", ":")))
PY
)"

export ROLE TASK_ID EXECUTION_ID OUTCOME PR_URL SUMMARY IDEM_KEY
curl -sS -X POST "${BRIDGE_BASE_URL}/webhooks/v1/agent-completions" \
  -H "Content-Type: application/json" \
  ${PLATFORM_COMPLETION_WEBHOOK_SECRET:+-H "X-Platform-Completion-Secret: $PLATFORM_COMPLETION_WEBHOOK_SECRET"} \
  -d "$BODY"

echo ""
echo "Completion accepted. Check n8n (platform-agent-completion) and registry task state."
