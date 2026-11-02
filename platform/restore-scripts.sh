#!/usr/bin/env bash
# Restore platform/scripts/ when missing (GitLab clone without scripts folder).
# Run from repository root:
#   cd /home/rst/Documents/GitHub/agent-dev-platform
#   bash platform/restore-scripts.sh
#
# Or from platform/:
#   cd platform && bash restore-scripts.sh

set -euo pipefail

if [[ -f scripts/preflight.sh && -f scripts/gitlab-create-task.sh ]]; then
  chmod +x scripts/*.sh scripts/lib/*.sh 2>/dev/null || true
  echo "OK: platform/scripts already present."
  ls -la scripts scripts/lib
  exit 0
fi

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT/platform"

if [[ ! -f scripts.bundle.tar.gz ]]; then
  echo "ERROR: missing platform/scripts.bundle.tar.gz and platform/scripts/" >&2
  echo "Ask for a full platform sync from the latest agent-dev-platform commit." >&2
  exit 1
fi

echo "Extracting platform/scripts from scripts.bundle.tar.gz ..."
tar xzf scripts.bundle.tar.gz
chmod +x scripts/*.sh scripts/lib/*.sh
echo "Done. Scripts installed:"
ls -la scripts scripts/lib
