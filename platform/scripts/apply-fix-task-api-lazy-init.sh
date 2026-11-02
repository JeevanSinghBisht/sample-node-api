#!/usr/bin/env bash
# Apply fix for GET /api/v1/tasks LazyInitializationException (RepoConfig).
# Run from repo root: ./platform/scripts/apply-fix-task-api-lazy-init.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
PATCH="$ROOT/platform/scripts/patches/fix-task-api-lazy-init.patch"

if [[ ! -f "$PATCH" ]]; then
  echo "Patch not found: $PATCH" >&2
  exit 1
fi

cd "$ROOT"
if git apply --check "$PATCH" 2>/dev/null; then
  git apply "$PATCH"
  echo "Applied patch. Rebuild workflow-registry:"
  echo "  cd platform && mvn -pl workflow-registry -am clean compile spring-boot:run"
  exit 0
fi

echo "Patch did not apply cleanly (maybe already applied). Checking source..." >&2
if grep -q 'listTaskSummaries' platform/workflow-registry/src/main/java/com/platform/workflow/registry/api/TaskController.java 2>/dev/null \
  && grep -q 'FetchType.EAGER' platform/workflow-registry/src/main/java/com/platform/workflow/registry/domain/WorkflowTask.java 2>/dev/null; then
  echo "Fix already present in source. Run: cd platform && mvn -pl workflow-registry -am clean compile spring-boot:run"
  exit 0
fi

echo "Manual merge required. See platform/scripts/patches/fix-task-api-lazy-init.patch" >&2
exit 1
