#!/usr/bin/env bash
# Quick checks before starting the GitLab manual workflow.
# Note: no "set -u" — .env may omit optional vars; health checks default code=000.

RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'
ok() { echo -e "${GREEN}OK${NC}  $1"; }
fail() { echo -e "${RED}FAIL${NC} $1"; FAILED=1; }
warn() { echo -e "WARN  $1"; }

FAILED=0
REGISTRY="${REGISTRY_BASE_URL:-http://localhost:8081}"
BRIDGE="${BRIDGE_BASE_URL:-http://localhost:8082}"

echo "=== Platform preflight ==="

command -v java >/dev/null 2>&1 && ok "java installed" || fail "java not found (need Java 21+)"
command -v mvn >/dev/null 2>&1 && ok "mvn installed" || fail "mvn not found"
command -v docker >/dev/null 2>&1 && ok "docker installed" || warn "docker not found (needed for n8n)"
command -v python3 >/dev/null 2>&1 && ok "python3 installed" || fail "python3 required for HMAC scripts"

if [[ -f .env ]]; then ok ".env exists"; else fail "missing platform/.env (cp .env.example .env)"; fi

if [[ -n "${CURSOR_API_KEY:-}" ]]; then
  if [[ ${#CURSOR_API_KEY} -lt 20 ]]; then
    fail "CURSOR_API_KEY looks truncated (length ${#CURSOR_API_KEY})"
  else
    ok "CURSOR_API_KEY set (length ${#CURSOR_API_KEY})"
  fi
else
  warn "CURSOR_API_KEY unset (use CURSOR_MOCK_ENABLED=true for dry run)"
fi

if [[ -n "${AGENT_REPO_URL:-}${GITLAB_REPO_URL:-}" ]]; then
  ok "AGENT_REPO_URL=${AGENT_REPO_URL:-$GITLAB_REPO_URL}"
elif [[ -n "${GITHUB_REPO_URL:-}" ]]; then
  warn "Using deprecated GITHUB_REPO_URL — prefer AGENT_REPO_URL for GitLab"
else
  fail "set AGENT_REPO_URL to your GitLab product project URL"
fi
[[ -n "${GITLAB_PROJECT_PATH:-}" ]] && ok "GITLAB_PROJECT_PATH=$GITLAB_PROJECT_PATH" || warn "GITLAB_PROJECT_PATH unset (derived from AGENT_REPO_URL in scripts)"

if command -v mysql >/dev/null 2>&1 && [[ -n "${MYSQL_HOST:-}" ]]; then
  if mysql -h"${MYSQL_HOST}" -P"${MYSQL_PORT:-3306}" -u"${MYSQL_USER}" -p"${MYSQL_PASSWORD}" -e "SELECT 1" >/dev/null 2>&1; then
    ok "MySQL reachable"
  else
    fail "MySQL not reachable with .env credentials"
  fi
else
  warn "mysql client not installed or MYSQL_* unset — skip DB ping"
fi

code="$(curl -s -o /dev/null -w '%{http_code}' "$REGISTRY/actuator/health" 2>/dev/null || echo 000)"
if [[ "$code" == "200" ]]; then ok "registry $REGISTRY"; else warn "registry not up yet ($REGISTRY → HTTP $code)"; fi

code="$(curl -s -o /dev/null -w '%{http_code}' "$BRIDGE/actuator/health" 2>/dev/null || echo 000)"
if [[ "$code" == "200" ]]; then ok "bridge $BRIDGE"; else warn "bridge not up yet ($BRIDGE → HTTP $code)"; fi

code="$(curl -s -o /dev/null -w '%{http_code}' "http://localhost:5678" 2>/dev/null || echo 000)"
if [[ "$code" =~ ^(200|301|302)$ ]]; then ok "n8n http://localhost:5678"; else warn "n8n not reachable on :5678"; fi

echo "=== Done ==="
if [[ "$FAILED" -eq 1 ]]; then
  echo "Fix FAIL items above, then re-run."
  exit 1
fi
exit 0
