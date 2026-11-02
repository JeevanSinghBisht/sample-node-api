# n8n orchestration specification

## Design rules

1. **Deterministic routing only** – IF/Switch on registry state and structured agent outcomes; no LLM nodes for control flow.
2. **Registry is authoritative** – every state change goes through `POST /internal/v1/tasks/{taskId}/transition`.
3. **One active execution per role** – Redis lock `lock:task:{taskId}` during spawn.
4. **Store execution mapping** – n8n execution ID → platform `executionId` in Redis and registry.

---

## Credentials (n8n)

| Credential | Usage |
|------------|--------|
| `registry_internal_hmac` | Sign transition requests |
| `cursor_bridge_api_key` | Spawn agents |
| `rabbitmq_platform` | Consume/publish (if using RabbitMQ trigger) |
| `redis_platform` | Idempotency + locks |

---

## Main workflow: `task-lifecycle`

### Trigger options

- **A (recommended):** Webhook `POST /n8n/task-state-changed` from registry outbox relay.
- **B:** RabbitMQ trigger on `platform.task.state.#`.

### Node graph (logical)

```
Trigger
  → Function: normalize envelope
  → Redis: GET idem:{idempotencyKey} — if exists, STOP
  → Redis: SET idem:{idempotencyKey}
  → Switch: payload.state
       READY:
         → HTTP registry: POST .../executions { role: DEVELOPER }
         → HTTP cursor-bridge: POST /agents
         → HTTP registry: PATCH execution { bcId, status: RUNNING }
       DEVELOPING:
         → Wait (webhook resume on execution.completed OR PR opened)
       IN_REVIEW:
         → HTTP spawn REVIEWER (new bcId)
       REVIEW_REJECTED:
         → HTTP transition → DEVELOPING (with feedback in metadata)
         → spawn DEVELOPER
       IN_QA:
         → HTTP spawn TESTER
         → Wait for execution.completed AND check_suite.success
       QA_FAILED:
         → transition DEVELOPING + spawn DEVELOPER
       AWAITING_HUMAN:
         → GitHub: add label agent:awaiting-human
         → Optional Slack notification
       DONE / CANCELLED:
         → GitHub: remove agent:* labels
```

### Wait nodes

Use **Wait + Webhook** (n8n) with correlation:

- Webhook path: `/hooks/agent-done/{executionId}`
- cursor-bridge calls this URL on completion (configured per environment).

Timeout branches:

| Phase | Timeout | Transition |
|-------|---------|------------|
| Developer | 4h | `DEV_FAILED` |
| Reviewer | 1h | retry once, then `REVIEW_REJECTED` with timeout finding |
| Tester | 2h | `QA_FAILED` |

---

## Sub-workflow: `apply-transition`

Reusable HTTP call:

```
Input: taskId, targetState, reason, idempotencyKey
HTTP POST /internal/v1/tasks/{taskId}/transition
Body: {
  "targetState": "...",
  "reason": "...",
  "idempotencyKey": "...",
  "metadata": { ... }
}
Error handling:
  409 → success (duplicate idempotency)
  422 → stop workflow, alert ops
  5xx → retry with exponential backoff (3x)
```

---

## Sub-workflow: `spawn-agent`

```
Input: taskId, role, promptContext
HTTP POST registry /executions → executionId
HTTP POST cursor-bridge /agents {
  taskId, executionId, role,
  repoUrl, baseRef,
  branchName: "cursor/task-{taskId}-{role}-{attemptNo}-ec4b",
  instructions: rendered template
}
Output: bcId, agentUrl
PATCH execution STARTED
```

### Branch naming

Align with Cloud Agent branch policy: `cursor/<descriptive>-ec4b` with task id embedded for traceability.

---

## Sub-workflow: `handle-developer-outcome`

```
Switch outcome:
  success + prUrl:
    → transition IN_REVIEW
    → GitHub comment on issue with PR link
  success + no pr:
    → transition DEV_FAILED (reason: no_pr)
  failed:
    → transition DEV_FAILED
```

---

## Sub-workflow: `handle-reviewer-outcome`

```
Switch outcome:
  approved:
    → transition IN_QA
  rejected:
    → store findings in registry metadata
    → transition REVIEW_REJECTED
    → call spawn-agent DEVELOPER with reviewFeedback
```

---

## Sub-workflow: `handle-qa-outcome`

```
IF agent outcome == failed → QA_FAILED → spawn developer
ELSE IF check_suite.conclusion != success → QA_FAILED
ELSE → transition AWAITING_HUMAN
```

---

## Retry scheduler workflow

**Cron:** `*/5 * * * *`

```
HTTP GET /internal/v1/tasks?state=DEV_FAILED&retryDue=true
For each task:
  IF attemptNo < maxAttempts:
    transition DEVELOPING
    spawn DEVELOPER
  ELSE:
    transition CANCELLED or BLOCKED
    notify human
```

---

## GitHub issue → READY detection

**Workflow:** `github-issue-ready`

Trigger: registry webhook `platform.issue.ready` OR GitHub webhook filtered in registry.

n8n optional path if registry delegates:

```
GitHub webhook → verify signature → if label agent:ready added
  → POST registry /tasks/from-issue
```

Prefer **registry owns GitHub parsing** so n8n never sees raw GitHub payloads.

---

## Observability in n8n

- Enable **execution logging** to external DB or OpenSearch.
- Prefix workflow names with `platform-`.
- Tag executions with `taskId` in n8n static data at start.

---

## Example transition request (n8n HTTP node)

```http
POST /internal/v1/tasks/550e8400-e29b-41d4-a716-446655440000/transition
X-Platform-Timestamp: 1730534400
X-Platform-Signature: hmac-sha256=...
Content-Type: application/json

{
  "targetState": "IN_REVIEW",
  "reason": "pull_request_opened",
  "idempotencyKey": "pr-108-opened",
  "metadata": {
    "prNumber": 108,
    "headSha": "abc123",
    "prUrl": "https://github.com/org/service-a/pull/108"
  }
}
```
