# First full run — GitLab only (no GitHub)

Example local clone (your path may differ):

```text
~/Documents/GitLab/agent-dev-platform/    # platform repo from GitLab
  platform/
  examples/product-starter/                 # push to second GitLab project
```

---

## Phase A — Platform `.env`

```bash
cd platform
cp .env.example .env
```

Set **`AGENT_REPO_URL`** to your **product** GitLab project (not the platform repo):

```bash
AGENT_REPO_URL=https://gitlab.com/YOUR_GROUP/YOUR_PRODUCT
GITLAB_PROJECT_PATH=YOUR_GROUP/YOUR_PRODUCT
REPO_DEFAULT_REF=main
# ... MySQL, RabbitMQ, CURSOR_API_KEY, n8n URLs — see .env.example
```

```bash
set -a && source .env && set +a
chmod +x scripts/*.sh scripts/lib/*.sh
./scripts/preflight.sh
docker compose up -d
# n8n: import + activate n8n/*.json
docker compose up -d --force-recreate n8n
mvn install -DskipTests
# Start registry :8081 and bridge :8082
```

Cursor: connect **GitLab** + allow **YOUR_PRODUCT** project.

---

## Phase B — Product repo on GitLab

```bash
cd examples/product-starter
git init && git add . && git commit -m "chore: starter"
git branch -M main
git remote add origin https://gitlab.com/YOUR_GROUP/YOUR_PRODUCT.git
git push -u origin main
```

Create GitLab **issue #1** using `docs/FIRST-ISSUE.md`.

---

## Phase C — Dev → Review → Test → Done

```bash
cd platform
set -a && source .env && set +a

./scripts/gitlab-create-task.sh 1 "Add product API" "$(cat ../examples/product-starter/docs/FIRST-ISSUE.md)"
```

Save `TASK_ID` from JSON. After each Cursor agent, use `gitlab-show-task.sh` and `gitlab-agent-completion.sh` (see [GITLAB-MANUAL.md](GITLAB-MANUAL.md)).

Merge MR on GitLab → dashboard approve at **AWAITING_HUMAN**.
