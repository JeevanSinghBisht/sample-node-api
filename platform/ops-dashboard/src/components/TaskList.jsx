import React, { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { listTasks } from '../api/client.js';
import StateBadge from './StateBadge.jsx';

const STATES = [
  '',
  'READY',
  'DEVELOPING',
  'DEV_FAILED',
  'IN_REVIEW',
  'REVIEW_REJECTED',
  'IN_QA',
  'QA_FAILED',
  'AWAITING_HUMAN',
  'DONE',
  'CANCELLED',
];

export default function TaskList() {
  const [stateFilter, setStateFilter] = useState('');
  const [page, setPage] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  const load = useCallback(() => {
    setLoading(true);
    setError('');
    listTasks(stateFilter || undefined)
      .then(setPage)
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [stateFilter]);

  useEffect(() => {
    load();
    const timer = setInterval(load, 20000);
    return () => clearInterval(timer);
  }, [load]);

  const tasks = page?.content || [];

  return (
    <section>
      <div className="toolbar card">
        <label>
          Filter by state
          <select value={stateFilter} onChange={(e) => setStateFilter(e.target.value)}>
            <option value="">All states</option>
            {STATES.filter(Boolean).map((s) => (
              <option key={s} value={s}>
                {s.replace(/_/g, ' ')}
              </option>
            ))}
          </select>
        </label>
        <button type="button" className="btn-secondary" onClick={load} disabled={loading}>
          {loading ? 'Refreshing…' : 'Refresh'}
        </button>
        {page && (
          <span className="muted small">
            {page.totalElements} task{page.totalElements === 1 ? '' : 's'}
          </span>
        )}
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      {!loading && !error && (
        <div className="table-wrap card">
          <table className="table">
            <thead>
              <tr>
                <th>Issue</th>
                <th>Title</th>
                <th>State</th>
                <th>MR</th>
                <th>Attempts</th>
                <th>Updated</th>
              </tr>
            </thead>
            <tbody>
              {tasks.length === 0 && (
                <tr>
                  <td colSpan={6} className="muted">
                    No tasks. Create one with <code>./scripts/gitlab-create-task.sh</code>.
                  </td>
                </tr>
              )}
              {tasks.map((task) => (
                <tr key={task.id}>
                  <td>
                    <Link to={`/tasks/${task.id}`}>#{task.issueNumber}</Link>
                  </td>
                  <td className="title-cell">{task.title}</td>
                  <td>
                    <StateBadge state={task.state} />
                  </td>
                  <td>
                    {task.prUrl ? (
                      <a href={task.prUrl} target="_blank" rel="noreferrer">
                        #{task.prNumber ?? 'MR'}
                      </a>
                    ) : (
                      '—'
                    )}
                  </td>
                  <td className="muted small">
                    D{task.attemptDev} / R{task.attemptReview} / Q{task.attemptQa}
                  </td>
                  <td className="muted small">
                    {task.updatedAt ? new Date(task.updatedAt).toLocaleString() : '—'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
