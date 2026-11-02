# Platform CLI scripts (GitLab manual workflow)

Run from **`platform/`** with env loaded: `set -a && source .env && set +a`

| Script | Purpose |
|--------|---------|
| `preflight.sh` | Check `.env`, MySQL, optional registry/bridge/n8n |
| `gitlab-create-task.sh` | Start task from GitLab issue # |
| `start-country-crud-task.sh` | Same, with Country/State/City spec |
| `gitlab-show-task.sh` | Task + execution IDs |
| `gitlab-agent-completion.sh` | After each agent (DEV/REV/TEST) |

If **`scripts/` is missing**, from repo root:

```bash
bash platform/restore-scripts.sh
```
