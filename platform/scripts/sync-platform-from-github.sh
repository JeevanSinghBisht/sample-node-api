#!/usr/bin/env bash
# Sync platform/ from GitHub (cloud agent branch) into your GitLab-oriented local clone.
#
# Run from repo root on YOUR machine:
#   cd /home/rst/Documents/GitHub/agent-dev-platform
#   bash platform/scripts/sync-platform-from-github.sh
#
# Optional env:
#   GITHUB_REMOTE   default: https://github.com/JeevanSinghBisht/sample-node-api.git
#   GITHUB_BRANCH   default: cursor/agent-dev-platform-arch-ec4b
#   GITLAB_REMOTE   default: origin (your gitlab.nyggs.com remote)
#   GITLAB_BRANCH   default: current branch name
#   PUSH_GITLAB=1   commit + push to GitLab after sync
#   COMMIT_MESSAGE  custom commit message when PUSH_GITLAB=1
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"

GITHUB_REMOTE="${GITHUB_REMOTE:-https://github.com/JeevanSinghBisht/sample-node-api.git}"
GITHUB_BRANCH="${GITHUB_BRANCH:-cursor/agent-dev-platform-arch-ec4b}"
GITLAB_REMOTE="${GITLAB_REMOTE:-origin}"
SYNC_REF="sync/platform-from-github"

echo "=== Sync platform from GitHub → local (for GitLab) ==="
echo "GitHub: $GITHUB_REMOTE ($GITHUB_BRANCH)"
echo "Repo root: $ROOT"
echo ""

git remote add platform-github "$GITHUB_REMOTE" 2>/dev/null || git remote set-url platform-github "$GITHUB_REMOTE"
git fetch platform-github "$GITHUB_BRANCH:$SYNC_REF"

echo "Updating platform/ from $SYNC_REF ..."
git checkout "$SYNC_REF" -- platform/

echo ""
echo "Synced paths under platform/ (latest from GitHub branch)."
echo ""

if [[ "${PUSH_GITLAB:-0}" == "1" ]]; then
  GITLAB_BRANCH="${GITLAB_BRANCH:-$(git branch --show-current)}"
  MSG="${COMMIT_MESSAGE:-chore: sync platform from GitHub $GITHUB_BRANCH}"
  git add platform/
  if git diff --cached --quiet; then
    echo "No changes to commit after sync."
  else
    git commit -m "$MSG"
    git push "$GITLAB_REMOTE" "$GITLAB_BRANCH"
    echo "Pushed to $GITLAB_REMOTE $GITLAB_BRANCH"
  fi
else
  echo "Local working tree updated. To save on GitLab:"
  echo "  git add platform && git commit -m 'chore: sync platform from GitHub' && git push $GITLAB_REMOTE <branch>"
  echo ""
  echo "Or re-run with: PUSH_GITLAB=1 bash platform/scripts/sync-platform-from-github.sh"
fi

echo ""
echo "=== Rebuild & run (after sync) ==="
echo "  cd platform"
echo "  mvn -pl cursor-bridge,workflow-registry -am clean compile -DskipTests"
echo "  ./scripts/run-bridge.sh      # :8082"
echo "  ./scripts/run-registry.sh    # :8081"
echo "  cd ops-dashboard && npm run dev   # :5173"
