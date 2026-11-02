#!/usr/bin/env bash
# Run on YOUR machine from the agent-dev-platform repo root (GitLab clone).
# Example: cd /home/rst/Documents/GitHub/agent-dev-platform && bash platform/scripts/pull-task-api-fix-into-clone.sh
#
# Fetches fixed Java sources from the public fix branch (one-time read; no GitHub workflow required).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"

FIX_REMOTE="${FIX_REMOTE:-https://github.com/JeevanSinghBisht/sample-node-api.git}"
FIX_BRANCH="${FIX_BRANCH:-cursor/agent-dev-platform-arch-ec4b}"
REF="fix/task-api-lazy-init"

echo "Fetching $FIX_BRANCH from fix remote..."
git remote add platform-fix "$FIX_REMOTE" 2>/dev/null || git remote set-url platform-fix "$FIX_REMOTE"
git fetch platform-fix "$FIX_BRANCH:$REF"

PATHS=(
  platform/workflow-registry/src/main/java/com/platform/workflow/registry/api/TaskController.java
  platform/workflow-registry/src/main/java/com/platform/workflow/registry/api/internal/InternalTaskController.java
  platform/workflow-registry/src/main/java/com/platform/workflow/registry/domain/WorkflowTask.java
  platform/workflow-registry/src/main/java/com/platform/workflow/registry/repository/WorkflowTaskRepository.java
  platform/workflow-registry/src/main/java/com/platform/workflow/registry/service/TaskQueryService.java
  platform/scripts/apply-fix-task-api-lazy-init.sh
  platform/scripts/patches/fix-task-api-lazy-init.patch
)

git checkout "$REF" -- "${PATHS[@]}"

echo "Updated:"
for p in "${PATHS[@]}"; do
  echo "  $p"
done

if grep -q listTaskSummaries platform/workflow-registry/src/main/java/com/platform/workflow/registry/api/TaskController.java \
  && grep -q FetchType.EAGER platform/workflow-registry/src/main/java/com/platform/workflow/registry/domain/WorkflowTask.java; then
  echo "OK: task API lazy-init fix is in your working tree."
else
  echo "WARN: fix markers not found; check paths match your repo layout." >&2
  exit 1
fi

echo ""
echo "Next:"
echo "  cd platform && mvn -pl workflow-registry -am clean compile spring-boot:run"
echo "Optional: git add -A && git commit -m 'fix: task list API LazyInitializationException' && git push origin <your-branch>"
