#!/usr/bin/env bash
# Create a workflow task from a GitLab issue (manual trigger — Option B).
# Usage:
#   cd platform && set -a && source .env && set +a
#   ./scripts/gitlab-create-task.sh 42 "Add login API" "Implement JWT login..."
#
# Env (required):
#   PLATFORM_INTERNAL_HMAC_SECRET, REGISTRY_BASE_URL (default http://localhost:8081)
#   AGENT_REPO_URL       GitLab URL Cursor clones, e.g. https://gitlab.com/my-group/my-app
#   GITLAB_PROJECT_PATH  group/project (optional if derivable from AGENT_REPO_URL)

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib/platform-hmac.sh
source "$SCRIPT_DIR/lib/platform-hmac.sh"
# shellcheck source=lib/repo-env.sh
source "$SCRIPT_DIR/lib/repo-env.sh"

ISSUE_NUMBER="${1:?Usage: $0 ISSUE_NUMBER \"Title\" [issue body markdown]}"
TITLE="${2:?Provide issue title as second argument}"
ISSUE_BODY="${3:-}"

REGISTRY_BASE_URL="${REGISTRY_BASE_URL:-http://localhost:8081}"

AGENT_REPO_URL="$(repo_env_agent_url)"
if [[ -z "$AGENT_REPO_URL" ]]; then
  echo "Set AGENT_REPO_URL in .env (GitLab project URL)" >&2
  exit 1
fi

GITLAB_PROJECT_PATH="$(repo_env_project_path)"
if [[ -z "$GITLAB_PROJECT_PATH" ]]; then
  echo "Set GITLAB_PROJECT_PATH or AGENT_REPO_URL in .env" >&2
  exit 1
fi

REPO_HOST="$(echo "$AGENT_REPO_URL" | sed -E 's#^(https?://[^/]+)/.*#\1#')"
ISSUE_URL="${REPO_HOST}/${GITLAB_PROJECT_PATH}/-/issues/${ISSUE_NUMBER}"

IDEM_KEY="${IDEMPOTENCY_KEY:-gitlab-manual-${GITLAB_PROJECT_PATH//\//-}-${ISSUE_NUMBER}}"

export GITLAB_PROJECT_PATH ISSUE_NUMBER ISSUE_URL TITLE ISSUE_BODY IDEM_KEY

BODY="$(python3 - <<'PY'
import json, os
print(json.dumps({
  "repoFullName": os.environ["GITLAB_PROJECT_PATH"],
  "githubRepoId": 0,
  "issueNumber": int(os.environ["ISSUE_NUMBER"]),
  "issueUrl": os.environ["ISSUE_URL"],
  "title": os.environ["TITLE"],
  "issueBody": os.environ.get("ISSUE_BODY") or None,
  "idempotencyKey": os.environ["IDEM_KEY"],
  "actorType": "N8N",
  "actorId": "gitlab-manual"
}, separators=(",", ":")))
PY
)"

platform_hmac_post "${REGISTRY_BASE_URL}/internal/v1/tasks/from-issue" "$BODY" | python3 -m json.tool

echo ""
echo "Task created → READY. Watch n8n Executions and cursor-bridge logs for agent spawn."
