# Agent dev platform (Spring Boot)

Maven multi-module skeleton for the event-driven SD platform. The **workflow-registry** service is the orchestration source of truth (MySQL), publishes domain events via a transactional outbox to RabbitMQ, and exposes internal APIs for n8n.

## Modules

| Module | Port | Description |
|--------|------|-------------|
| `platform-common` | — | Shared HMAC helpers |
| `workflow-registry` | 8081 | Tasks, transitions, executions, outbox relay |
| `cursor-bridge` | 8082 | Cursor Cloud Agents API + completion webhooks |

| `ops-dashboard` | 5173 | React ops UI (task list, timeline, human approve) |

GitHub webhooks are handled by **workflow-registry** at `POST /webhooks/v1/github` (no separate `github-ingest` module).

n8n import files: [`n8n/`](n8n/README.md).

## Quick start

```bash
cd platform
docker compose up -d
export PLATFORM_INTERNAL_HMAC_SECRET=local-dev-secret
export CURSOR_MOCK_ENABLED=true   # local dev without Cursor API key
mvn -pl workflow-registry spring-boot:run
# separate terminal:
mvn -pl cursor-bridge spring-boot:run

# Ops dashboard (proxies /api → registry :8081)
cd ops-dashboard && npm install && npm run dev
```

### GitHub App webhook

Point the GitHub App webhook to:

`https://<registry-host>/webhooks/v1/github`

Events: **Issues**, **Pull requests**. Set `GITHUB_WEBHOOK_SECRET` to match the App secret.

When an issue receives label **`agent:ready`**, the registry creates a task and emits `READY` (outbox → RabbitMQ + optional n8n webhook).

### n8n wiring

1. Import `n8n/platform-task-lifecycle.json` and `n8n/platform-agent-completion.json`.
2. Set n8n variables (`REGISTRY_BASE_URL`, `BRIDGE_BASE_URL`, `PLATFORM_HMAC_SECRET`, `GITHUB_REPO_URL`).
3. Activate workflows and copy webhook URLs into:
   - `N8N_TASK_STATE_WEBHOOK_URL` (registry)
   - `N8N_AGENT_COMPLETION_WEBHOOK_URL` (cursor-bridge)

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
