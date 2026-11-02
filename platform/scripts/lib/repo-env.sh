#!/usr/bin/env bash
# Resolve the Git repo URL Cursor agents clone (GitLab-only setup; no GitHub required).

repo_env_agent_url() {
  if [[ -n "${AGENT_REPO_URL:-}" ]]; then
    echo "$AGENT_REPO_URL"
  elif [[ -n "${GITLAB_REPO_URL:-}" ]]; then
    echo "$GITLAB_REPO_URL"
  elif [[ -n "${GITHUB_REPO_URL:-}" ]]; then
    echo "$GITHUB_REPO_URL"
  else
    echo ""
  fi
}

repo_env_project_path() {
  if [[ -n "${GITLAB_PROJECT_PATH:-}" ]]; then
    echo "$GITLAB_PROJECT_PATH"
    return
  fi
  local url
  url="$(repo_env_agent_url)"
  if [[ -z "$url" ]]; then
    echo ""
    return
  fi
  echo "$url" | sed -E 's#^https?://[^/]+/##; s#\.git$##; s#/$##'
}
