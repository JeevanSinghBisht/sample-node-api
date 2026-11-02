import React, { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import {
  getTask,
  getTaskActions,
  humanApprove,
  reportCompletion,
  runDeveloper,
  runReviewer,
  runTester,
} from '../api/client.js';
import ExecutionCard from './ExecutionCard.jsx';
import StateBadge from './StateBadge.jsx';

export default function TaskDetail() {
  const { taskId } = useParams();
  const [detail, setDetail] = useState(null);
  const [actions, setActions] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState('');
  const [reporting, setReporting] = useState(false);

  const reload = useCallback(() => {
    setLoading(true);
    setError('');
    Promise.all([getTask(taskId), getTaskActions(taskId)])
      .then(([taskDetail, actionList]) => {
        setDetail(taskDetail);
        setActions(actionList.availableActions || []);
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [taskId]);

  useEffect(() => {
    reload();
    const timer = setInterval(reload, 15000);
    return () => clearInterval(timer);
  }, [reload]);

  async function runAction(kind, fn) {
    setBusy(kind);
    setError('');
    try {
      await fn();
      reload();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy('');
    }
  }

  async function onReportCompletion(executionId, payload) {
    setReporting(true);
    setError('');
    try {
      await reportCompletion(taskId, executionId, payload);
      reload();
    } catch (err) {
      setError(err.message);
    } finally {
      setReporting(false);
    }
  }

  if (loading && !detail) {
    return <p className="muted">Loading task…</p>;
  }
  if (error && !detail) {
    return <p className="error">{error}</p>;
  }
  if (!detail) {
    return null;
  }

  const task = detail.task;
  const has = (name) => actions.includes(name);

  return (
    <section className="detail">
      <p className="breadcrumb">
        <Link to="/">← All tasks</Link>
      </p>

      <div className="detail-hero card">
        <div className="detail-hero-top">
          <div>
            <h1>
              #{task.issueNumber} {task.title}
            </h1>
            <p className="muted">
              <a href={task.issueUrl} target="_blank" rel="noreferrer">
                GitLab issue ↗
              </a>
              {' · '}
              <span className="mono">{task.repoFullName}</span>
            </p>
          </div>
          <StateBadge state={task.state} />
        </div>

        <div className="stat-row">
          <div className="stat">
            <span className="stat-label">Dev attempts</span>
            <span className="stat-value">{task.attemptDev}</span>
          </div>
          <div className="stat">
            <span className="stat-label">Review attempts</span>
            <span className="stat-value">{task.attemptReview}</span>
          </div>
          <div className="stat">
            <span className="stat-label">QA attempts</span>
            <span className="stat-value">{task.attemptQa}</span>
          </div>
          <div className="stat">
            <span className="stat-label">Updated</span>
            <span className="stat-value small">
              {task.updatedAt ? new Date(task.updatedAt).toLocaleString() : '—'}
            </span>
          </div>
        </div>

        {task.prUrl && (
          <p className="pr-line">
            Merge request:{' '}
            <a href={task.prUrl} target="_blank" rel="noreferrer">
              #{task.prNumber ?? 'link'}
            </a>
            {task.headSha && <span className="muted"> · {task.headSha.slice(0, 7)}</span>}
          </p>
        )}
      </div>

      <div className="card action-panel">
        <h2>Workflow actions</h2>
        <p className="muted small">
          Trigger agents from the dashboard (requires cursor-bridge on :8082 and AGENT_REPO_URL on workflow-registry).
        </p>
        <div className="action-grid">
          <button
            type="button"
            className="btn-primary"
            disabled={!has('RUN_DEVELOPER') || !!busy}
            onClick={() => runAction('dev', () => runDeveloper(taskId))}
          >
            {busy === 'dev' ? 'Starting…' : 'Run developer'}
          </button>
          <button
            type="button"
            className="btn-secondary"
            disabled={!has('RUN_REVIEWER') || !!busy}
            onClick={() => runAction('rev', runReviewer.bind(null, taskId))}
          >
            {busy === 'rev' ? 'Starting…' : 'Run reviewer'}
          </button>
          <button
            type="button"
            className="btn-secondary"
            disabled={!has('RUN_TESTER') || !!busy}
            onClick={() => runAction('qa', runTester.bind(null, taskId))}
          >
            {busy === 'qa' ? 'Starting…' : 'Run tester'}
          </button>
          {has('HUMAN_APPROVE') && (
            <button
              type="button"
              className="btn-success"
              disabled={!!busy}
              onClick={() => runAction('done', () => humanApprove(taskId))}
            >
              {busy === 'done' ? 'Saving…' : 'Mark merged / done'}
            </button>
          )}
        </div>
        {!has('RUN_DEVELOPER') && !has('RUN_REVIEWER') && !has('RUN_TESTER') && !has('HUMAN_APPROVE') && (
          <p className="muted">No manual actions for state {task.state}. Wait for an agent run or report completion below.</p>
        )}
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      <h2 className="section-title">Agent runs</h2>
      <div className="execution-list">
        {detail.executions.length === 0 && <p className="muted card">No executions yet. Use Run developer to start.</p>}
        {detail.executions.map((ex) => (
          <ExecutionCard
            key={ex.id}
            execution={ex}
            reporting={reporting}
            onReportCompletion={has('REPORT_COMPLETION') || ex.status === 'RUNNING' ? onReportCompletion : null}
          />
        ))}
      </div>

      <h2 className="section-title">State timeline</h2>
      <ul className="timeline">
        {detail.transitions.map((t, index) => (
          <li key={`${t.createdAt}-${index}`}>
            <div className="timeline-states">
              <StateBadge state={t.fromState || 'DRAFT'} />
              <span className="arrow">→</span>
              <StateBadge state={t.toState} />
            </div>
            <div className="muted">{t.reason}</div>
            <div className="muted small">{new Date(t.createdAt).toLocaleString()}</div>
          </li>
        ))}
      </ul>
    </section>
  );
}
