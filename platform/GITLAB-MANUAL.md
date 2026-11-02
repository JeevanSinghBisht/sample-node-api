# GitLab-only workflow (manual trigger)

**No GitHub account, webhooks, or `github.com` URLs are required.** Everything runs against **GitLab** issues/MRs and your local platform stack.

Platform code can live in GitLab (recommended). Your **product** code lives in a **separate GitLab project** (`AGENT_REPO_URL`).

**First full cycle:** [FIRST-RUN-GITLAB.md](FIRST-RUN-GITLAB.md)

---

## 1. Prerequisites

| Item | Notes |
|------|--------|
| Java 21, Maven, Docker | For n8n |
| MySQL, Redis, RabbitMQ | Local services |
| **Cursor Cloud Agents** | [Dashboard → Cloud Agents](https://cursor.com/dashboard?tab=cloud-agents): connect **GitLab**, grant access to **product** project |
| **CURSOR_API_KEY** | User key; `curl -H "Authorization: Bearer $CURSOR_API_KEY" https://api.cursor.com/v1/me` |

---

## 2. Configure `platform/.env`

```bash
cd platform
cp .env.example .env
```

```bash
AGENT_REPO_URL=https://gitlab.com/YOUR_GROUP/YOUR_PRODUCT
GITLAB_PROJECT_PATH=YOUR_GROUP/YOUR_PRODUCT
REPO_DEFAULT_REF=main

PLATFORM_INTERNAL_HMAC_SECRET=local-dev-secret
CURSOR_API_KEY=crsr_...
CURSOR_MOCK_ENABLED=false

N8N_TASK_STATE_WEBHOOK_URL=http://localhost:5678/webhook/platform-task-state
N8N_AGENT_COMPLETION_WEBHOOK_URL=http://localhost:5678/webhook/platform-agent-completion
N8N_REGISTRY_BASE_URL=http://host.docker.internal:8081
N8N_BRIDGE_BASE_URL=http://host.docker.internal:8082
```

Do **not** set GitHub webhook secrets unless you intentionally enable optional GitHub integration in the registry.

After edits:

```bash
docker compose up -d --force-recreate n8n
```

---

## 3. Start stack

1. `mvn install -DskipTests`
2. **workflow-registry** `:8081`
3. **cursor-bridge** `:8082`
4. Import + **activate** `n8n/platform-task-lifecycle.json` and `n8n/platform-agent-completion.json`
5. Optional: `ops-dashboard` on `:5173`

---

## 4. GitLab issue → task

1. Create an issue in the **product** GitLab project.
2. Run:

```bash
set -a && source .env && set +a
./scripts/gitlab-create-task.sh <issue#> "Title" "Description markdown"
```

---

## 5. Completions (dev / review / test)

```bash
./scripts/gitlab-show-task.sh <task-uuid>
./scripts/gitlab-agent-completion.sh DEVELOPER <task> <exec> success "https://gitlab.com/.../-/merge_requests/N"
./scripts/gitlab-agent-completion.sh REVIEWER <task> <exec> approved
./scripts/gitlab-agent-completion.sh TESTER <task> <exec> success
```

Merge MR in GitLab → dashboard **Mark merged / done** at **AWAITING_HUMAN**.

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| Wrong repo cloned | `AGENT_REPO_URL` must match GitLab project; Cursor must have GitLab access |
| n8n silent | Workflows active; registry has `N8N_TASK_STATE_WEBHOOK_URL`; re-import n8n JSON after updates |
| Old env name | Replace `GITHUB_REPO_URL` with **`AGENT_REPO_URL`** in `.env` |
