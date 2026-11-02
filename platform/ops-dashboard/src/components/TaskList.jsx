import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { listTasks } from '../api/client.js';

const STATES = ['', 'READY', 'DEVELOPING', 'IN_REVIEW', 'IN_QA', 'AWAITING_HUMAN', 'DONE'];

export default function TaskList() {
  const [stateFilter, setStateFilter] = useState('');
  const [page, setPage] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    setError('');
    listTasks(stateFilter || undefined)
      .then(setPage)
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [stateFilter]);

  const tasks = page?.content || [];

  return (
    <section>
      <div className="toolbar">
        <label>
          State{' '}
          <select value={stateFilter} onChange={(e) => setStateFilter(e.target.value)}>
            <option value="">All</option>
            {STATES.filter(Boolean).map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
        </label>
      </div>

      {loading && <p className="muted">Loading tasks…</p>}
      {error && <p className="error">{error}</p>}

      {!loading && !error && (
        <table className="table">
          <thead>
            <tr>
              <th>Issue</th>
              <th>Title</th>
              <th>State</th>
              <th>PR</th>
              <th>Updated</th>
            </tr>
          </thead>
          <tbody>
            {tasks.map((task) => (
              <tr key={task.id}>
                <td>
                  <Link to={`/tasks/${task.id}`}>#{task.issueNumber}</Link>
                </td>
                <td>{task.title}</td>
                <td>
                  <span className={`pill pill-${task.state.toLowerCase()}`}>{task.state}</span>
                </td>
                <td>
                  {task.prUrl ? (
                    <a href={task.prUrl} target="_blank" rel="noreferrer">
                      #{task.prNumber}
                    </a>
                  ) : (
                    '—'
                  )}
                </td>
                <td className="muted">{task.updatedAt ? new Date(task.updatedAt).toLocaleString() : '—'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}
