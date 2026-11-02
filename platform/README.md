# Agent dev platform (Spring Boot)

Event-driven orchestration for **GitLab** + **Cursor Cloud Agents** (Developer → Reviewer → Tester). **No GitHub required.**

| Doc | Use when |
|-----|----------|
| **[FIRST-RUN-GITLAB.md](FIRST-RUN-GITLAB.md)** | First setup on your machine + first full workflow |
| **[GITLAB-MANUAL.md](GITLAB-MANUAL.md)** | Day-to-day GitLab manual trigger reference |
| [STARTUP.md](STARTUP.md) | Optional: generic install details (includes legacy GitHub webhook notes — skip those for GitLab-only) |

## Two repos

1. **This repo** — platform (registry, bridge, n8n, dashboard).
2. **Product repo on GitLab** — app the agents build (`AGENT_REPO_URL` in `.env`).

Starter for product repo: [`../examples/product-starter/`](../examples/product-starter/).

## Quick start (GitLab)

```bash
cd platform
cp .env.example .env   # set AGENT_REPO_URL, CURSOR_API_KEY, MySQL/RabbitMQ
set -a && source .env && set +a
./scripts/preflight.sh
docker compose up -d
# Import + activate n8n/*.json → http://localhost:5678
mvn install -DskipTests
mvn -pl workflow-registry -am spring-boot:run   # :8081
mvn -pl cursor-bridge -am spring-boot:run       # :8082
./scripts/gitlab-create-task.sh <issue#> "title" "body"
```

## Modules

| Module | Port | Description |
|--------|------|-------------|
| `platform-common` | — | Shared HMAC helpers |
| `workflow-registry` | 8081 | Tasks, transitions, executions, outbox |
| `cursor-bridge` | 8082 | Cursor Cloud Agents API + completions |
| `ops-dashboard` | 5173 | Task list, human approve |
| `n8n` (Docker) | 5678 | Lifecycle orchestration |

Environment: [`.env.example`](.env.example) — use **`AGENT_REPO_URL`** for your GitLab product project.

Architecture: [../docs/agent-dev-platform/](../docs/agent-dev-platform/).
