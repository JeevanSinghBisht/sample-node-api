# GitLab-only setup (manual trigger — Option B)

Use this when your **application repo** lives on **GitLab** and you **do not** rely on GitHub webhooks. You start work by creating a task from a GitLab issue number; n8n + Cursor Cloud Agents handle the rest.

The platform code itself can live on GitLab or GitHub — that does not matter.

---

## 1. Prerequisites

| Item | Notes |
|------|--------|
| Java 21, Maven, Docker | Same as [STARTUP.md](STARTUP.md) |
| MySQL, Redis, RabbitMQ | Local or Docker |
| **Cursor Cloud Agents** | [Dashboard → Cloud Agents](https://cursor.com/dashboard?tab=cloud-agents): connect **GitLab** (or your provider) and grant access to the project you automate |
| **CURSOR_API_KEY** | User API key; `/v1/me` must succeed |

---

## 2. Configure `platform/.env`

```bash
cd platform
cp .env.example .env
```

**GitLab-focused values:**

```bash
# Agent target repo (name is legacy — value is the Git URL Cursor clones)
GITHUB_REPO_URL=https://gitlab.com/YOUR_GROUP/YOUR_PROJECT

# GitLab path used in API (group/project)
GITLAB_PROJECT_PATH=YOUR_GROUP/YOUR_PROJECT

# Default branch for new agent branches
REPO_DEFAULT_REF=main

PLATFORM_INTERNAL_HMAC_SECRET=local-dev-secret
REGISTRY_BASE_URL=http://localhost:8081
BRIDGE_BASE_URL=http://localhost:8082

CURSOR_API_KEY=crsr_...
CURSOR_MOCK_ENABLED=false

N8N_TASK_STATE_WEBHOOK_URL=http://localhost:5678/webhook/platform-task-state
N8N_AGENT_COMPLETION_WEBHOOK_URL=http://localhost:5678/webhook/platform-agent-completion

N8N_REGISTRY_BASE_URL=http://host.docker.internal:8081
N8N_BRIDGE_BASE_URL=http://host.docker.internal:8082

# GitHub webhook not used in Option B — leave empty
GITHUB_WEBHOOK_SECRET=
```

Reload n8n after `.env` changes:

```bash
docker compose up -d --force-recreate n8n
```

---

## 3. Start stack

Same as [STARTUP.md](STARTUP.md) §6:

1. `mvn install -DskipTests`
2. **workflow-registry** `:8081`
3. **cursor-bridge** `:8082`
4. Import + **activate** both workflows under `platform/n8n/` (re-import after updates)
5. Optional: `ops-dashboard` on `:5173`

---

## 4. Create a GitLab issue (human)

In your GitLab project:

1. **Issues → New issue** — write a clear title and description.
2. Note the issue number (e.g. **42**).

No special label is required for Option B.

---

## 5. Start a platform task (manual trigger)

```bash
cd platform
set -a && source .env && set +a
chmod +x scripts/*.sh scripts/lib/*.sh

./scripts/gitlab-create-task.sh 42 "Add health check endpoint" "Add GET /health returning JSON status."
```

This calls `POST /internal/v1/tasks/from-issue` (HMAC), creates the task, transitions to **READY**, and the registry notifies n8n.

**Verify:**

```bash
curl -s http://localhost:8081/api/v1/tasks | python3 -m json.tool
```

n8n → **Executions** → `platform-task-lifecycle` should run and spawn a **DEVELOPER** agent via cursor-bridge.

---

## 6. While the developer agent runs

- Open the agent from [cursor.com/agents](https://cursor.com/agents) or bridge logs.
- Agent works on `GITHUB_REPO_URL` (your GitLab project).
- Prompt includes your issue URL/title/body from the script.

Ask the agent to open an **MR** and put `Task-Id: <task-uuid>` in the MR description (copy `id` from the create-task JSON).

---

## 7. Signal completion (each role)

When an agent finishes, get IDs:

```bash
./scripts/gitlab-show-task.sh YOUR-TASK-UUID
```

Use the **DEVELOPER** execution `id`, then:

```bash
./scripts/gitlab-agent-completion.sh DEVELOPER YOUR-TASK-UUID EXEC-UUID success \
  "https://gitlab.com/YOUR_GROUP/YOUR_PROJECT/-/merge_requests/1"
```

That hits cursor-bridge → n8n **platform-agent-completion** → registry moves to **IN_REVIEW** (on success) and spawns the **REVIEWER**.

Repeat for **REVIEWER** (`approved` / `rejected`) and **TESTER** (`success` / `failed`):

```bash
./scripts/gitlab-agent-completion.sh REVIEWER YOUR-TASK-UUID EXEC-UUID approved
./scripts/gitlab-agent-completion.sh TESTER YOUR-TASK-UUID EXEC-UUID success
```

---

## 8. Human finish

When state is **AWAITING_HUMAN**:

1. Review and **merge** the MR in GitLab.
2. Ops dashboard → **Mark merged / done**, or use the registry human-approve API.

---

## 9. End-to-end checklist

```text
GitLab issue #N
    → ./scripts/gitlab-create-task.sh N "title" "body"
    → READY → n8n → DEVELOPER agent (Cursor)
    → ./scripts/gitlab-agent-completion.sh DEVELOPER … success [MR URL]
    → IN_REVIEW → REVIEWER agent
    → ./scripts/gitlab-agent-completion.sh REVIEWER … approved
    → IN_QA → TESTER agent
    → ./scripts/gitlab-agent-completion.sh TESTER … success
    → AWAITING_HUMAN → merge MR → dashboard Done
```

---

## 10. Troubleshooting

| Problem | Fix |
|---------|-----|
| n8n does not run | Workflows **active**? `N8N_TASK_STATE_WEBHOOK_URL` set? Restart registry after `.env` change |
| Agent wrong repo | `GITHUB_REPO_URL` must match GitLab project URL Cursor can access |
| Agent wrong branch | Set `REPO_DEFAULT_REF=main` (or `master`); recreate n8n container |
| Prompt shows wrong issue link | Re-import `platform-task-lifecycle.json`; create task with script (sends `issueUrl`) |
| Cursor clone fails | Connect GitLab in Cursor dashboard for that namespace/project |

---

## What is not included in Option B

- Automatic start from GitLab webhooks (use n8n + GitLab webhook → internal API later if you want).
- Automatic MR detection (you pass MR URL on developer completion, or extend the platform).

GitHub path remains available via [STARTUP.md](STARTUP.md) §8 Path A.
