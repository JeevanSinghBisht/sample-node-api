# First agent task — Product API

## Goal

Turn this starter into a small **Node.js** HTTP API that we can extend later (Java/React services can follow the same workflow in their own repos).

## Requirements

1. Add **`express`** (or native `node:http`) and a **`package.json`** with `"start": "node src/server.js"`.
2. Implement **`GET /health`** returning JSON: `{ "status": "UP", "service": "product-api" }`.
3. Implement **`GET /api/v1/items`** returning a JSON array with at least two sample items `{ "id", "name" }`.
4. Add **`README.md`** section **Run locally** with `npm install` and `npm start` (port **3000**).
5. Add a minimal test or smoke script if reasonable (optional but preferred).
6. Open a **merge request** to `main`; do **not** merge.

## Constraints

- Keep dependencies minimal.
- Include in the MR description: `Closes #1` and `Task-Id: <task-uuid>` (the platform will give you the task UUID in the agent prompt).

## Acceptance (for Reviewer / Tester)

- Server starts without errors.
- `/health` and `/api/v1/items` return 200 and valid JSON.
- README instructions work on a clean clone.
