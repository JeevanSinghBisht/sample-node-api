# Platform startup guide

This guide walks you from zero to a running **agent dev platform**: Docker (MySQL, Redis, RabbitMQ, n8n), **workflow-registry**, **cursor-bridge**, optional **ops dashboard**, and real **Cursor Cloud Agents**.

All paths assume you are in the repository root unless noted.

---

## 0. Checkout the repository

Platform code lives on branch **`cursor/agent-dev-platform-arch-ec4b`** in [JeevanSinghBisht/sample-node-api](https://github.com/JeevanSinghBisht/sample-node-api).

### Fresh clone

```bash
git clone https://github.com/JeevanSinghBisht/sample-node-api.git
cd sample-node-api
git fetch origin cursor/agent-dev-platform-arch-ec4b
git checkout cursor/agent-dev-platform-arch-ec4b
```

Confirm the `platform/` folder exists:

```bash
ls platform/README.md platform/STARTUP.md
```

### Already cloned (switch to platform branch)

```bash
cd sample-node-api
git fetch origin
git checkout cursor/agent-dev-platform-arch-ec4b
git pull origin cursor/agent-dev-platform-arch-ec4b
```

### One-shot: checkout + configure for real Cursor Cloud Agents

Run from any directory (creates `sample-node-api` if needed). **Replace** `crsr_…` and the GitHub repo URL with yours:

```bash
git clone https://github.com/JeevanSinghBisht/sample-node-api.git
cd sample-node-api
git checkout cursor/agent-dev-platform-arch-ec4b

cd platform
cp .env.example .env

# Edit .env — at minimum set these three (use your editor or sed):
#   CURSOR_API_KEY=crsr_your_key_here
#   CURSOR_MOCK_ENABLED=false
#   GITHUB_REPO_URL=https://github.com/YOUR_ORG/YOUR_REPO

set -a && source .env && set +a
docker compose up -d
```

Then continue with **§5** (n8n import) and **§6** (start Java services), or use the full sequence in **§ Quick run checklist** below.

---

## Quick run checklist (with Cursor Cloud key)

```bash
# 1) Checkout (see §0)
git clone https://github.com/JeevanSinghBisht/sample-node-api.git
cd sample-node-api && git checkout cursor/agent-dev-platform-arch-ec4b

# 2) Env — add CURSOR_API_KEY from Cursor Dashboard → API Keys
cd platform
cp .env.example .env
# nano .env   →  CURSOR_API_KEY=crsr_...  CURSOR_MOCK_ENABLED=false  GITHUB_REPO_URL=https://github.com/org/repo

# 3) Infrastructure
set -a && source .env && set +a
docker compose up -d

# 4) n8n — browser: http://localhost:5678 → import n8n/*.json → activate both workflows

# 5) Registry (terminal 1)
cd platform && set -a && source .env && set +a
mvn -pl workflow-registry spring-boot:run

# 6) Bridge (terminal 2)
cd platform && set -a && source .env && set +a
mvn -pl cursor-bridge spring-boot:run

# 7) Dashboard (terminal 3, optional)
cd platform/ops-dashboard && npm install && npm run dev
```

Verify Cursor key before starting the bridge:

```bash
set -a && source platform/.env && set +a
curl -s -u "$CURSOR_API_KEY:" https://api.cursor.com/v1/me
```

---

## Prerequisites

Install on your machine:

| Tool | Version | Check |
|------|---------|--------|
| Java | 21+ | `java -version` |
| Maven | 3.8+ | `mvn -version` |
| Docker + Compose | current | `docker compose version` |
| Node.js | 18+ (dashboard only) | `node -version` |

Clone the repo and use the branch that contains `platform/` (for example `cursor/agent-dev-platform-arch-ec4b`).

---

## 1. Get keys from Cursor

The **cursor-bridge** service calls the [Cloud Agents API](https://cursor.com/docs/cloud-agent/api/endpoints) (`POST https://api.cursor.com/v1/agents`). You need a **Cloud Agents–capable API key** and GitHub connected to Cursor.

### 1.1 Connect GitHub to Cursor

1. Open [Cursor Dashboard → Cloud Agents](https://cursor.com/dashboard?tab=cloud-agents) (or **Settings → Cloud Agents** in the IDE).
2. Connect **GitHub** (or your Git provider) and grant access to the repository you will automate (for example `your-org/your-repo`).
3. Confirm you can start a cloud agent manually on that repo from [cursor.com/agents](https://cursor.com/agents) before using the API.

Without this step, API agent creates fail even with a valid key.

### 1.2 Create a Cursor API key

Cursor documents keys under **Dashboard → API Keys**. UI labels may also appear under **Integrations** depending on your account.

1. Go to [https://cursor.com/dashboard](https://cursor.com/dashboard).
2. Open **API Keys** (or **Integrations** → API keys / Cloud Agents keys).
3. Click **New API key** (or equivalent). Name it e.g. `platform-cursor-bridge`.
4. Copy the key **immediately** — it is shown only once.

Key format is often like `crsr_…` (see [Cursor API overview](https://cursor.com/docs/api)).

**Where to put it:** `CURSOR_API_KEY` in `platform/.env` (see below).

**Auth used by this project:** Basic auth — username = API key, password empty — which matches Cursor’s docs:

```bash
curl -s -u "$CURSOR_API_KEY:" https://api.cursor.com/v1/me
```

If that returns your identity JSON, the key works for the Cloud Agents API.

### 1.3 (Optional) Cloud environment name

If you use a **named Cursor cloud environment** (prebuilt install for Java/Maven/Node), note its **environment name** from the dashboard (Cloud Agents → Environments). You can pass it when spawning agents as `environmentId` in n8n/bridge payloads (`env.name` in the Cursor API).

For a first run you can omit it and let Cursor use default cloud VMs with `repos[].url` only.

### 1.4 (Optional) Verify agent create

Replace repo URL and key:

```bash
export CURSOR_API_KEY='crsr_your_key_here'

curl -s -X POST https://api.cursor.com/v1/agents \
  -u "$CURSOR_API_KEY:" \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": { "text": "Reply with OK only." },
    "repos": [{ "url": "https://github.com/your-org/your-repo", "startingRef": "master" }]
  }'
```

You should get JSON with `agent.id` (bc-…) and `agent.url`.

---

## 2. GitHub webhook secret (optional for local mock)

For **GitHub → registry** automation (issue label `agent:ready`, PR opened):

1. GitHub → your repo → **Settings → Webhooks → Add webhook** (or use a **GitHub App**).
2. **Payload URL:** `https://<public-host>/webhooks/v1/github`  
   - Local dev: expose `:8081` with [ngrok](https://ngrok.com/) or Cloudflare Tunnel, e.g.  
     `ngrok http 8081` → `https://abc123.ngrok-free.app/webhooks/v1/github`
3. **Content type:** `application/json`
4. **Secret:** generate a random string (e.g. `openssl rand -hex 32`).
5. **Events:** Issues, Pull requests

Put the same secret in `.env` as `GITHUB_WEBHOOK_SECRET`.

Leave empty only for local testing without GitHub (create tasks via internal API instead).

---

## 3. Configure environment file

```bash
cd platform
cp .env.example .env
```

Edit `.env`. Minimum for **real Cursor agents**:

```bash
PLATFORM_INTERNAL_HMAC_SECRET=local-dev-secret   # change in production
CURSOR_API_KEY=crsr_your_key_here
CURSOR_MOCK_ENABLED=false
GITHUB_REPO_URL=https://github.com/your-org/your-repo
```

Minimum for **dry run** (no Cursor API, fake bcIds):

```bash
CURSOR_MOCK_ENABLED=true
# CURSOR_API_KEY can stay empty
```

Load env in every shell that runs Java:

```bash
cd platform
set -a && source .env && set +a
```

On Windows PowerShell:

```powershell
cd platform
Get-Content .env | ForEach-Object {
  if ($_ -match '^\s*([^#][^=]+)=(.*)$') { Set-Item -Path "env:$($matches[1].Trim())" -Value $matches[2].Trim() }
}
```

---

## 4. Start infrastructure (Docker)

```bash
cd platform
docker compose up -d
```

| Service | URL / port |
|---------|------------|
| MySQL | `localhost:3306` (db `workflow_registry`, user/pass `platform`) |
| Redis | `localhost:6379` |
| RabbitMQ | AMQP `localhost:5672`, UI http://localhost:15672 (`platform` / `platform`) |
| n8n | http://localhost:5678 |

Check:

```bash
docker compose ps
```

First-time n8n: open http://localhost:5678 and create an owner account.

---

## 5. Import and activate n8n workflows

1. In n8n: **Workflows → Import from file**.
2. Import:
   - `platform/n8n/platform-task-lifecycle.json`
   - `platform/n8n/platform-agent-completion.json`
3. **Activate** both workflows (toggle top-right).
4. Open each **Webhook** node and note the **Production URL** (should match):
   - `http://localhost:5678/webhook/platform-task-state`
   - `http://localhost:5678/webhook/platform-agent-completion`
5. Confirm `.env` contains:

```bash
N8N_TASK_STATE_WEBHOOK_URL=http://localhost:5678/webhook/platform-task-state
N8N_AGENT_COMPLETION_WEBHOOK_URL=http://localhost:5678/webhook/platform-agent-completion
```

n8n in Docker already receives `REGISTRY_BASE_URL`, `BRIDGE_BASE_URL`, `PLATFORM_HMAC_SECRET`, and `GITHUB_REPO_URL` from `docker-compose.yml` + `.env`. Update `GITHUB_REPO_URL` to your repo.

If you change webhook URLs or `.env`, restart the Java services (step 6).

---

## 6. Start Java services

Use **two terminals**, both with env loaded (`set -a && source .env && set +a` from `platform/`).

**Terminal A — workflow-registry (port 8081):**

```bash
cd platform
set -a && source .env && set +a
mvn -pl workflow-registry spring-boot:run
```

Wait until Flyway migrations finish and the app listens on **8081**.

**Terminal B — cursor-bridge (port 8082):**

```bash
cd platform
set -a && source .env && set +a
mvn -pl cursor-bridge spring-boot:run
```

Health checks:

```bash
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8081/actuator/health
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8082/actuator/health
```

Expect `200`.

---

## 7. Start ops dashboard (optional)

**Terminal C:**

```bash
cd platform/ops-dashboard
npm install
npm run dev
```

Open http://localhost:5173 (proxies `/api` → registry `:8081`).

---

## 8. Run your first task (end-to-end)

### Path A — With GitHub (recommended)

1. Ensure webhook points to registry and `GITHUB_WEBHOOK_SECRET` is set in `.env`.
2. Create an issue in `GITHUB_REPO_URL` with a clear description.
3. Add label **`agent:ready`**.
4. Watch:
   - Registry logs: task created, state `READY`
   - n8n **Executions**: task-lifecycle workflow runs
   - Bridge logs: agent spawn (or mock bcId)
   - Dashboard: task appears, state progresses
5. When the developer agent opens a PR with `Fixes #<issue>` in the body, registry moves to **`IN_REVIEW`** and n8n spawns the reviewer.
6. Wire agent completion to n8n (bridge `N8N_AGENT_COMPLETION_WEBHOOK_URL`) or POST completions manually (Path B) until Cursor webhooks are configured.
7. When state is **`AWAITING_HUMAN`**, merge on GitHub, then in the dashboard click **Mark merged / done**.

### Path B — Manual task (no GitHub)

Create a task via internal API (requires HMAC). Example using the same secret as `.env`:

```bash
cd platform
set -a && source .env && set +a
export BODY='{"repoFullName":"your-org/your-repo","githubRepoId":0,"issueNumber":99,"issueUrl":"https://github.com/your-org/your-repo/issues/99","title":"Test task","idempotencyKey":"manual-99"}'
export TS=$(date +%s)
export SIG=$(python3 -c "import hmac,hashlib,os; s=os.environ['PLATFORM_INTERNAL_HMAC_SECRET']; p=f\"{os.environ['TS']}.{os.environ['BODY']}\"; print('hmac-sha256='+hmac.new(s.encode(),p.encode(),hashlib.sha256).hexdigest())")
curl -s -X POST http://localhost:8081/internal/v1/tasks/from-issue \
  -H "Content-Type: application/json" \
  -H "X-Platform-Timestamp: $TS" \
  -H "X-Platform-Signature: $SIG" \
  -d "$BODY"
```

Then open the dashboard or `GET http://localhost:8081/api/v1/tasks`.

---

## 9. Environment variable reference

| Variable | Used by | Purpose |
|----------|---------|---------|
| `PLATFORM_INTERNAL_HMAC_SECRET` | registry, bridge, n8n | Signs `/internal/*` requests |
| `CURSOR_API_KEY` | cursor-bridge | Cursor Cloud Agents API |
| `CURSOR_MOCK_ENABLED` | cursor-bridge | `true` = fake agents, no API key |
| `GITHUB_WEBHOOK_SECRET` | registry | Validates GitHub webhooks |
| `GITHUB_REPO_URL` | n8n | Repo URL in spawn payloads |
| `N8N_TASK_STATE_WEBHOOK_URL` | registry | Outbox notifies n8n on state change |
| `N8N_AGENT_COMPLETION_WEBHOOK_URL` | cursor-bridge | Forwards completions to n8n |
| `N8N_REGISTRY_BASE_URL` | n8n container | Reach registry on host (`host.docker.internal:8081`) |
| `N8N_BRIDGE_BASE_URL` | n8n container | Reach bridge on host |
| `REGISTRY_BASE_URL` | cursor-bridge | Registry base URL |
| `DASHBOARD_ALLOWED_ORIGINS` | registry CORS | e.g. `http://localhost:5173` |

Full template: [`.env.example`](.env.example).

---

## 10. Troubleshooting

| Symptom | What to check |
|---------|----------------|
| Registry won’t start | MySQL up? `docker compose ps`; ports 3306 free |
| n8n workflow doesn’t run | Workflows **activated**? `N8N_TASK_STATE_WEBHOOK_URL` set and registry restarted |
| n8n can’t reach registry | Spring on **host**; `.env` has `N8N_REGISTRY_BASE_URL=http://host.docker.internal:8081` (Linux: compose includes `host-gateway`) |
| Cursor 401/403 | `CURSOR_API_KEY` valid? `curl -u "$CURSOR_API_KEY:" https://api.cursor.com/v1/me` |
| Cursor agent fails on repo | GitHub connected in Cursor dashboard; repo URL matches `GITHUB_REPO_URL` |
| GitHub webhook 401 | `GITHUB_WEBHOOK_SECRET` matches GitHub webhook secret |
| Dashboard CORS error | `DASHBOARD_ALLOWED_ORIGINS` includes `http://localhost:5173` |

---

## 11. Stop everything

```bash
cd platform
# Ctrl+C on Maven and Vite terminals, then:
docker compose down
# Remove n8n data: docker compose down -v   # deletes n8n_data volume
```

---

## Related docs

- [README.md](README.md) — module overview  
- [n8n/README.md](n8n/README.md) — workflow details  
- [../docs/agent-dev-platform/ARCHITECTURE.md](../docs/agent-dev-platform/ARCHITECTURE.md) — full architecture  
- [Cursor Cloud Agents API](https://cursor.com/docs/cloud-agent/api/endpoints)  
- [Cursor API keys](https://cursor.com/docs/api)
