import React, { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { getTask, humanApprove } from '../api/client.js';

export default function TaskDetail() {
  const { taskId } = useParams();
  const [detail, setDetail] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [approving, setApproving] = useState(false);

  function reload() {
    setLoading(true);
    setError('');
    getTask(taskId)
      .then(setDetail)
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }

  useEffect(() => {
    reload();
  }, [taskId]);

  async function onHumanApprove() {
    setApproving(true);
    try {
      await humanApprove(taskId, 'dashboard-operator');
      reload();
    } catch (err) {
      setError(err.message);
    } finally {
      setApproving(false);
    }
  }

  if (loading) {
    return <p className="muted">Loading task…</p>;
  }
  if (error && !detail) {
    return <p className="error">{error}</p>;
  }
  if (!detail) {
    return null;
  }

  const task = detail.task;

  return (
    <section className="detail">
      <p>
        <Link to="/">← Tasks</Link>
      </p>
      <h1>
        #{task.issueNumber} {task.title}
      </h1>
      <p className="muted">
        <a href={task.issueUrl} target="_blank" rel="noreferrer">
          GitHub issue
        </a>
        {' · '}
        <span className={`pill pill-${task.state.toLowerCase()}`}>{task.state}</span>
      </p>

      {task.prUrl && (
        <p>
          Pull request:{' '}
          <a href={task.prUrl} target="_blank" rel="noreferrer">
            #{task.prNumber}
          </a>
          {task.headSha && <span className="muted"> · {task.headSha.slice(0, 7)}</span>}
        </p>
      )}

      {task.state === 'AWAITING_HUMAN' && (
        <div className="callout">
          <p>Automated gates passed. Confirm merge on GitHub, then acknowledge here.</p>
          <button type="button" disabled={approving} onClick={onHumanApprove}>
            {approving ? 'Saving…' : 'Mark merged / done'}
          </button>
        </div>
      )}

      {error && <p className="error">{error}</p>}

      <h2>Timeline</h2>
      <ul className="timeline">
        {detail.transitions.map((t, index) => (
          <li key={`${t.createdAt}-${index}`}>
            <strong>{t.fromState || '—'}</strong> → <strong>{t.toState}</strong>
            <span className="muted"> · {t.reason}</span>
            <div className="muted">{new Date(t.createdAt).toLocaleString()}</div>
          </li>
        ))}
      </ul>

      <h2>Agent runs</h2>
      <ul className="timeline">
        {detail.executions.length === 0 && <li className="muted">No executions yet.</li>}
        {detail.executions.map((ex) => (
          <li key={ex.id}>
            <strong>{ex.role}</strong> · {ex.status}
            {ex.agentUrl && (
              <>
                {' '}
                ·{' '}
                <a href={ex.agentUrl} target="_blank" rel="noreferrer">
                  Open agent
                </a>
              </>
            )}
            {ex.branchName && <span className="muted"> · {ex.branchName}</span>}
          </li>
        ))}
      </ul>
    </section>
  );
}
