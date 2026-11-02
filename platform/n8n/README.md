# n8n workflows

Import these workflows into n8n (**Workflows → Import from file**). Set environment variables in n8n (Settings → Variables) before activating.

| Variable | Example |
|----------|---------|
| `REGISTRY_BASE_URL` | `http://workflow-registry:8081` |
| `BRIDGE_BASE_URL` | `http://cursor-bridge:8082` |
| `PLATFORM_HMAC_SECRET` | same as `PLATFORM_INTERNAL_HMAC_SECRET` |
| `GITHUB_REPO_URL` | `https://github.com/org/repo` |

## Workflows

| File | Trigger | Purpose |
|------|---------|---------|
| `platform-task-lifecycle.json` | Webhook `POST /platform-task-state` | Spawn agents on `READY`, `IN_REVIEW`, `IN_QA` |
| `platform-agent-completion.json` | Webhook `POST /platform-agent-completion` | Apply registry transitions after agent/QA outcomes |

Copy the **Production URL** from each Webhook node into:

- Registry: `N8N_TASK_STATE_WEBHOOK_URL`
- Bridge: `N8N_AGENT_COMPLETION_WEBHOOK_URL`

HMAC signing uses a Code node (`platform-hmac.js` logic inlined in each workflow).
