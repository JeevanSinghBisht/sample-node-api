# Agent dev platform (Spring Boot)

Maven multi-module skeleton for the event-driven SD platform. The **workflow-registry** service is the orchestration source of truth (MySQL), publishes domain events via a transactional outbox to RabbitMQ, and exposes internal APIs for n8n.

**Start here:** [STARTUP.md](STARTUP.md) — prerequisites, Cursor API keys, copy-paste commands, first task.

## Modules

| Module | Port | Description |
|--------|------|-------------|
| `platform-common` | — | Shared HMAC helpers |
| `workflow-registry` | 8081 | Tasks, transitions, executions, outbox relay |
| `cursor-bridge` | 8082 | Cursor Cloud Agents API + completion webhooks |
| `ops-dashboard` | 5173 | React ops UI (task list, timeline, human approve) |
| `n8n` (Docker) | 5678 | Workflow orchestration UI + webhooks |

GitHub webhooks are handled by **workflow-registry** at `POST /webhooks/v1/github` (no separate `github-ingest` module).

n8n import files: [`n8n/`](n8n/README.md).

## Quick start

```bash
cd platform
cp .env.example .env   # edit secrets / repo URL as needed
docker compose up -d   # n8n only (http://localhost:5678) — use existing local MySQL/Redis/RabbitMQ
# OR full stack in Docker if ports are free:
# docker compose --profile infra up -d

set -a && source .env && set +a
mvn -pl workflow-registry spring-boot:run
# separate terminal (same env):
mvn -pl cursor-bridge spring-boot:run

# Ops dashboard (proxies /api → registry :8081)
cd ops-dashboard && npm install && npm run dev
```

Environment reference: [`.env.example`](.env.example).

### GitHub App webhook

Point the GitHub App webhook to:

`https://<registry-host>/webhooks/v1/github`

Events: **Issues**, **Pull requests**. Set `GITHUB_WEBHOOK_SECRET` to match the App secret.

When an issue receives label **`agent:ready`**, the registry creates a task and emits `READY` (outbox → RabbitMQ + optional n8n webhook).

### n8n wiring

The **n8n** container starts with `docker compose` (port **5678**). Workflow Code nodes read `REGISTRY_BASE_URL`, `BRIDGE_BASE_URL`, `PLATFORM_HMAC_SECRET`, and `GITHUB_REPO_URL` from the container environment (see `.env.example`).

1. Open http://localhost:5678 and import `n8n/platform-task-lifecycle.json` and `n8n/platform-agent-completion.json`.
2. Activate both workflows.
3. Ensure `.env` on the host includes (defaults in `.env.example`):
   - `N8N_TASK_STATE_WEBHOOK_URL=http://localhost:5678/webhook/platform-task-state`
   - `N8N_AGENT_COMPLETION_WEBHOOK_URL=http://localhost:5678/webhook/platform-agent-completion`
4. Restart registry and bridge so they pick up the webhook URLs.

Run Spring Boot on the **host** while n8n runs in Docker; n8n reaches the host via `host.docker.internal` (`N8N_REGISTRY_BASE_URL` / `N8N_BRIDGE_BASE_URL` in `.env`).

Flyway applies `workflow-registry/src/main/resources/db/migration/V1__workflow.sql` on startup.

## Internal API (n8n)

All `/internal/v1/**` routes require HMAC headers when `platform.internal.hmac-enabled=true`:

- `X-Platform-Timestamp` — Unix epoch seconds
- `X-Platform-Signature` — `hmac-sha256=` + hex HMAC-SHA256 of `{timestamp}.{rawBody}`

| Method | Path |
|--------|------|
| `POST` | `/internal/v1/tasks/from-issue` |
| `GET` | `/internal/v1/tasks/{taskId}` |
| `POST` | `/internal/v1/tasks/{taskId}/transition` |
| `POST` | `/internal/v1/tasks/{taskId}/executions` |
| `PATCH` | `/internal/v1/executions/{executionId}` |

Public read API (no HMAC in scaffold): `GET /api/v1/tasks`, `GET /api/v1/tasks/{taskId}`.

## cursor-bridge (n8n + Cursor)

| Method | Path | Auth |
|--------|------|------|
| `POST` | `/internal/v1/agents` or `/agents` | HMAC (same as registry) |
| `POST` | `/webhooks/v1/agent-completions` | Optional `X-Platform-Completion-Secret` |

Set `CURSOR_API_KEY` for real agent launches (Cursor Cloud Agents API `POST /v1/agents`). Use `CURSOR_MOCK_ENABLED=true` for local integration without API calls.

## Architecture docs

See [../docs/agent-dev-platform/](../docs/agent-dev-platform/).
