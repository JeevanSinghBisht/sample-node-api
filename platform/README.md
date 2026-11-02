# Agent dev platform (Spring Boot)

Maven multi-module skeleton for the event-driven SD platform. The **workflow-registry** service is the orchestration source of truth (MySQL), publishes domain events via a transactional outbox to RabbitMQ, and exposes internal APIs for n8n.

## Modules

| Module | Port | Description |
|--------|------|-------------|
| `workflow-registry` | 8081 | Tasks, transitions, executions, outbox relay |

Future: `cursor-bridge`, `github-ingest`.

## Quick start

```bash
cd platform
docker compose up -d
export PLATFORM_INTERNAL_HMAC_SECRET=local-dev-secret
mvn -pl workflow-registry spring-boot:run
```

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

## Architecture docs

See [../docs/agent-dev-platform/](../docs/agent-dev-platform/).
