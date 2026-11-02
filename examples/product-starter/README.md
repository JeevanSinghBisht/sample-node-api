# Product starter (agent target repo)

Push this folder to a **separate GitLab project**. The **agent-dev-platform** stack (registry + n8n + Cursor) will clone **this** repo when running Developer / Reviewer / Tester agents.

```bash
git init
git add .
git commit -m "chore: initial starter"
git branch -M main
git remote add origin https://gitlab.com/YOUR_GROUP/YOUR_PRODUCT.git
git push -u origin main
```

Then point `platform/.env` (GitLab only — no GitHub):

```bash
AGENT_REPO_URL=https://gitlab.com/YOUR_GROUP/YOUR_PRODUCT
GITLAB_PROJECT_PATH=YOUR_GROUP/YOUR_PRODUCT
```

Open **`docs/FIRST-ISSUE.md`** in GitLab as issue #1 and follow **`platform/FIRST-RUN-GITLAB.md`**.
