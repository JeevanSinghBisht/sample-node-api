# First full run (GitLab) — platform + new product repo

Use this after cloning the **platform** repo locally, e.g.:

```text
/home/rst/Documents/GitHub/agent-dev-platform/
  platform/          ← run everything from here
  examples/          ← optional product starter to push to a second GitLab repo
```

You will use **two GitLab projects**:

| GitLab project | Purpose |
|----------------|---------|
| `agent-dev-platform` (this repo) | Registry, bridge, n8n, dashboard |
| **Your new product repo** | Java/React/Node app that agents build |

---

## Phase A — Configure the platform (once)

### A1. Environment file

```bash
cd /home/rst/Documents/GitHub/agent-dev-platform/platform
cp .env.example .env
nano .env   # or your editor
```

Set at minimum (replace `YOUR_GROUP` / `YOUR_PRODUCT`):

```bash
GITHUB_REPO_URL=https://gitlab.com/YOUR_GROUP/YOUR_PRODUCT
GITLAB_PROJECT_PATH=YOUR_GROUP/YOUR_PRODUCT
REPO_DEFAULT_REF=main

MYSQL_HOST=localhost
MYSQL_USER=root
MYSQL_PASSWORD=root

RABBITMQ_USER=guest
RABBITMQ_PASSWORD=guest

PLATFORM_INTERNAL_HMAC_SECRET=local-dev-secret
CURSOR_API_KEY=crsr_your_full_key
CURSOR_MOCK_ENABLED=false

N8N_TASK_STATE_WEBHOOK_URL=http://localhost:5678/webhook/platform-task-state
N8N_AGENT_COMPLETION_WEBHOOK_URL=http://localhost:5678/webhook/platform-agent-completion
N8N_REGISTRY_BASE_URL=http://host.docker.internal:8081
N8N_BRIDGE_BASE_URL=http://host.docker.internal:8082
```

Create MySQL database once:

```sql
CREATE DATABASE IF NOT EXISTS workflow_registry CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### A2. Preflight

```bash
cd /home/rst/Documents/GitHub/agent-dev-platform/platform
set -a && source .env && set +a
chmod +x scripts/*.sh scripts/lib/*.sh
./scripts/preflight.sh
```

Fix anything reported as FAIL before continuing.

### A3. n8n

```bash
docker compose up -d
```

1. Open http://localhost:5678 (create owner on first visit).
2. **Import** and **Activate**:
   - `n8n/platform-task-lifecycle.json`
   - `n8n/platform-agent-completion.json`

```bash
docker compose up -d --force-recreate n8n
```

### A4. Build and run Java services (three terminals)

```bash
cd /home/rst/Documents/GitHub/agent-dev-platform/platform
set -a && source .env && set +a
mvn install -DskipTests
```

| Terminal | Command |
|----------|---------|
| 1 | `mvn -pl workflow-registry -am spring-boot:run` |
| 2 | `mvn -pl cursor-bridge -am spring-boot:run` |
| 3 (optional) | `cd ops-dashboard && npm install && npm run dev` → http://localhost:5173 |

### A5. Cursor

1. [Cloud Agents](https://cursor.com/dashboard?tab=cloud-agents) → connect **GitLab**.
2. Grant access to **`YOUR_GROUP/YOUR_PRODUCT`** (the app repo, not necessarily the platform repo).
3. Verify: `curl -s -H "Authorization: Bearer $CURSOR_API_KEY" https://api.cursor.com/v1/me`

---

## Phase B — Create the **product** GitLab repo

### Option 1 — Push the included starter

```bash
cd /home/rst/Documents/GitHub/agent-dev-platform/examples/product-starter
git init
git add .
git commit -m "chore: initial product starter for agent workflow"
git remote add origin https://gitlab.com/YOUR_GROUP/YOUR_PRODUCT.git
git push -u origin main
```

### Option 2 — Empty GitLab project

Create an empty project on GitLab with default branch `main`, push any existing app, and set `GITHUB_REPO_URL` / `GITLAB_PROJECT_PATH` to match.

---

## Phase C — First workflow: Dev → Review → Test → Done

### C1. GitLab issue (in **product** repo)

Use the text in `examples/product-starter/docs/FIRST-ISSUE.md` (copy into GitLab **New issue**). Note the issue number (e.g. **1**).

### C2. Start the platform task

```bash
cd /home/rst/Documents/GitHub/agent-dev-platform/platform
set -a && source .env && set +a

./scripts/gitlab-create-task.sh 1 "Add product API per issue spec" "$(cat ../examples/product-starter/docs/FIRST-ISSUE.md)"
```

Save the **`id`** field from the JSON output (`taskId`).

Watch:

- n8n → **Executions** → `platform-task-lifecycle`
- https://cursor.com/agents → **DEVELOPER** run
- Dashboard or `curl -s http://localhost:8081/api/v1/tasks | python3 -m json.tool`

### C3. Developer done → completion

When the developer agent finishes (MR open):

```bash
TASK_ID=<paste-task-uuid>
./scripts/gitlab-show-task.sh "$TASK_ID"
```

Copy the **DEVELOPER** execution `id`, then:

```bash
./scripts/gitlab-agent-completion.sh DEVELOPER "$TASK_ID" <developer-exec-id> success \
  "https://gitlab.com/YOUR_GROUP/YOUR_PRODUCT/-/merge_requests/1"
```

State should move to **IN_REVIEW** and n8n spawns **REVIEWER**.

### C4. Reviewer done

```bash
./scripts/gitlab-agent-completion.sh REVIEWER "$TASK_ID" <reviewer-exec-id> approved \
  "Review passed; tests and scope OK"
```

→ **IN_QA** → **TESTER** agent runs.

### C5. Tester done

```bash
./scripts/gitlab-agent-completion.sh TESTER "$TASK_ID" <tester-exec-id> success \
  "QA passed"
```

→ **AWAITING_HUMAN**.

### C6. Human

1. Merge the MR in GitLab.
2. Dashboard → **Mark merged / done** (or approve via API).

Task state → **DONE**. One full cycle complete.

---

## Phase D — Daily use

1. New GitLab issue on the **product** repo.
2. `./scripts/gitlab-create-task.sh <n> "title" "body"`.
3. After each agent: `./scripts/gitlab-agent-completion.sh …`.
4. Merge + dashboard approve at the end.

See also [GITLAB-MANUAL.md](GITLAB-MANUAL.md) and [STARTUP.md](STARTUP.md).
