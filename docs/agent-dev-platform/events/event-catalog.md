# Event catalog (RabbitMQ)

## Topology

- **Exchange:** `platform.events` (topic, durable)
- **Dead letter:** `platform.events.dlq` (direct)
- **Queue naming:** `n8n.{consumer}.{routing-key}` (durable, quorum queues recommended)

All messages share this envelope:

```json
{
  "eventId": "uuid",
  "eventType": "platform.task.state.changed",
  "occurredAt": "2026-11-02T06:30:00Z",
  "correlationId": "task-uuid",
  "idempotencyKey": "sha256(...)",
  "payload": { }
}
```

Headers (for tracing):

- `traceparent` – W3C trace context
- `x-platform-version` – schema version, e.g. `1`

---

## Event types

### `platform.issue.ready`

Published when GitHub issue receives `agent:ready` or project field moves to Ready.

**Routing key:** `platform.issue.ready`

```json
{
  "repoFullName": "org/service-a",
  "issueNumber": 42,
  "issueUrl": "https://github.com/org/service-a/issues/42",
  "title": "Add rate limiting",
  "body": "...",
  "labels": ["agent:ready"],
  "requestedBy": "github-login"
}
```

**Consumer:** registry (create task) → emits `platform.task.state.changed`.

---

### `platform.task.state.changed`

**Routing key:** `platform.task.state.{state}` (e.g. `platform.task.state.IN_REVIEW`)

```json
{
  "taskId": "uuid",
  "repoFullName": "org/service-a",
  "issueNumber": 42,
  "previousState": "DEVELOPING",
  "state": "IN_REVIEW",
  "attemptNo": 1,
  "prNumber": 108,
  "headSha": "abc123...",
  "metadata": {}
}
```

**Consumers:** n8n main workflow (primary), notification-service (optional).

---

### `platform.execution.requested`

Internal command event (optional pattern if n8n calls HTTP instead).

**Routing key:** `platform.execution.requested`

```json
{
  "taskId": "uuid",
  "executionId": "uuid",
  "role": "DEVELOPER",
  "repoUrl": "https://github.com/org/service-a",
  "baseRef": "master",
  "branchPrefix": "cursor/task-{taskId}-",
  "promptContext": {
    "issueBody": "...",
    "reviewFeedback": null,
    "qaFeedback": null
  }
}
```

**Consumer:** cursor-bridge.

---

### `platform.execution.started`

**Routing key:** `platform.execution.started`

```json
{
  "taskId": "uuid",
  "executionId": "uuid",
  "role": "REVIEWER",
  "bcId": "bc-...",
  "agentUrl": "https://cursor.com/agents/bc-...",
  "startedAt": "2026-11-02T06:35:00Z"
}
```

---

### `platform.execution.completed`

**Routing key:** `platform.execution.completed.{outcome}` where outcome ∈ `success|failed|rejected|approved`

```json
{
  "taskId": "uuid",
  "executionId": "uuid",
  "role": "DEVELOPER",
  "outcome": "success",
  "summary": "Implemented rate limiter with Redis sliding window.",
  "artifacts": {
    "prUrl": "https://github.com/org/service-a/pull/108",
    "prNumber": 108,
    "headSha": "abc123..."
  },
  "findings": [],
  "bcId": "bc-..."
}
```

Reviewer rejection example:

```json
{
  "taskId": "uuid",
  "executionId": "uuid",
  "role": "REVIEWER",
  "outcome": "rejected",
  "summary": "Missing integration test for 429 responses.",
  "findings": [
    { "severity": "major", "file": "src/.../RateLimitFilter.java", "line": 45, "message": "..." }
  ]
}
```

**Consumers:** n8n `agent-completion` workflow → registry transition.

---

### `platform.github.pull_request.opened`

Normalized from GitHub webhook (may be emitted by registry after ingest).

**Routing key:** `platform.github.pull_request.opened`

```json
{
  "repoFullName": "org/service-a",
  "prNumber": 108,
  "headSha": "abc123...",
  "baseRef": "master",
  "linkedIssueNumbers": [42],
  "taskId": "uuid"
}
```

---

### `platform.github.check_suite.completed`

**Routing key:** `platform.github.check_suite.completed`

```json
{
  "repoFullName": "org/service-a",
  "headSha": "abc123...",
  "conclusion": "success",
  "checkRuns": [
    { "name": "build_test", "conclusion": "success" }
  ],
  "taskId": "uuid"
}
```

Used in QA phase when n8n waits for CI terminal state.

---

### `platform.task.human_gate`

**Routing key:** `platform.task.human_gate`

```json
{
  "taskId": "uuid",
  "state": "AWAITING_HUMAN",
  "prUrl": "https://github.com/org/service-a/pull/108",
  "message": "All automated gates passed."
}
```

---

## Idempotency

- Publishers set `idempotencyKey = SHA-256(eventType + taskId + executionId + payloadHash)`.
- Consumers SET `idem:{idempotencyKey}` in Redis before processing (NX, EX 86400).
- Registry `outbox_event` table unique on `idempotency_key`.

---

## Versioning

- Breaking payload changes increment `x-platform-version`.
- Consumers support N and N-1 for rolling deploys.
