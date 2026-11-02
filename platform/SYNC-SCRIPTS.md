# Missing `platform/scripts/`?

Your clone path (example):

```text
/home/rst/Documents/GitHub/agent-dev-platform/
```

## Option A — Pull from GitLab (recommended)

After the latest commit is on GitLab:

```bash
cd /home/rst/Documents/GitHub/agent-dev-platform
git pull origin main
cd platform
chmod +x restore-scripts.sh scripts/*.sh scripts/lib/*.sh 2>/dev/null || true
bash restore-scripts.sh
```

## Option B — Restore without full pull

If the repo has **`platform/restore-scripts.sh`** and **`platform/scripts.bundle.tar.gz`** but no `scripts/` folder:

```bash
cd /home/rst/Documents/GitHub/agent-dev-platform
bash platform/restore-scripts.sh
cd platform
set -a && source .env && set +a
./scripts/preflight.sh
```

## Option C — Only two files missing on GitLab

Upload to `platform/` in GitLab UI (or copy from a colleague):

- `restore-scripts.sh`
- `scripts.bundle.tar.gz`

Then run Option B.
