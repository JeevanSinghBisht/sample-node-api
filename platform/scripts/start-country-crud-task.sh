#!/usr/bin/env bash
# Start Developer agent for Country/State/City Spring Boot CRUD (GitLab issue).
# Usage:
#   cd platform && set -a && source .env && set +a
#   ./scripts/start-country-crud-task.sh <gitlab-issue-number>

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ISSUE_N="${1:?Usage: $0 <gitlab-issue-number>}"
SPEC="$SCRIPT_DIR/issue-specs/country-state-city-crud.md"

if [[ ! -f "$SPEC" ]]; then
  echo "Missing $SPEC — run platform/restore-scripts.sh" >&2
  exit 1
fi

exec "$SCRIPT_DIR/gitlab-create-task.sh" "$ISSUE_N" \
  "Country / State / City CRUD API (Spring Boot + MySQL)" \
  "$(cat "$SPEC")"
