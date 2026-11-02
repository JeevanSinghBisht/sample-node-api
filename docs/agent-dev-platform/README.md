# Event-Driven Multi-Agent SD Platform

This folder defines the architecture for a software development platform where **n8n orchestrates lifecycle and state**, and **Cursor Cloud Agents** perform Developer, Reviewer, and Tester work against **GitHub**.

## Documents

| Document | Purpose |
|----------|---------|
| [ARCHITECTURE.md](./ARCHITECTURE.md) | HLD + LLD, services, integrations, security, observability |
| [events/event-catalog.md](./events/event-catalog.md) | RabbitMQ exchanges, routing keys, payload contracts |
| [n8n/state-machine.md](./n8n/state-machine.md) | Task states, n8n nodes, idempotency, retries |
| [schema/V001__workflow.sql](./schema/V001__workflow.sql) | MySQL schema (source of truth for orchestration metadata) |

## Core principle

**n8n is the orchestrator, not an AI agent.** It owns task state transitions, execution IDs, retry budgets, commit SHAs, and handoffs. Agents receive bounded prompts and report structured outcomes via webhooks; they do not decide the next workflow step.

## Happy-path flow

1. Human opens a GitHub Issue and marks it ready (label or project field).
2. n8n ingests the signal and creates a `workflow_task` in `READY`.
3. n8n starts a **Developer** Cloud Agent with issue context and environment.
4. Developer opens a PR; n8n records `pr_number`, `head_sha`, moves to `IN_REVIEW`.
5. n8n starts an independent **Reviewer** agent on the PR diff.
6. On approval → **Tester** agent runs QA (CI + scripted checks).
7. On success → PR marked ready for human merge; task → `AWAITING_HUMAN`.

## Code scaffold

Spring Boot multi-module skeleton lives in [`platform/`](../../platform/README.md) (`workflow-registry` service).

## Implementation status

| Step | Location |
|------|----------|
| workflow-registry + outbox | `platform/workflow-registry` |
| GitHub webhooks (`agent:ready`, PR opened) | `POST /webhooks/v1/github` |
| cursor-bridge | `platform/cursor-bridge` |
| n8n import workflows | `platform/n8n/*.json` |
| React ops dashboard | `platform/ops-dashboard` |
