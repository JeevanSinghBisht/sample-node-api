# n8n workflows

Import these workflows into n8n (**Workflows → Import from file**).

With **`docker compose`** in `platform/`, n8n receives variables from [`.env`](../.env.example) (see `n8n` service in `docker-compose.yml`). Code nodes use `$env.REGISTRY_BASE_URL`, etc.

| Variable | Docker compose default |
|----------|-------------------------|
| `REGISTRY_BASE_URL` | `http://host.docker.internal:8081` |
| `BRIDGE_BASE_URL` | `http://host.docker.internal:8082` |
| `PLATFORM_HMAC_SECRET` | same as `PLATFORM_INTERNAL_HMAC_SECRET` |
| `AGENT_REPO_URL` | GitLab product repo URL (from `.env`) |

If n8n is not in Docker, set the same names under **Settings → Variables** or export them in the shell before starting n8n.

## Workflows

| File | Trigger | Purpose |
|------|---------|---------|
| `platform-task-lifecycle.json` | Webhook `POST /platform-task-state` | Spawn agents on `READY`, `IN_REVIEW`, `IN_QA` |
| `platform-agent-completion.json` | Webhook `POST /platform-agent-completion` | Apply registry transitions after agent/QA outcomes |

Copy the **Production URL** from each Webhook node into:

- Registry: `N8N_TASK_STATE_WEBHOOK_URL`
- Bridge: `N8N_AGENT_COMPLETION_WEBHOOK_URL`

HMAC signing uses a Code node (`platform-hmac.js` logic inlined in each workflow).
