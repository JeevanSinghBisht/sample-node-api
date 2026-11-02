import React, { useMemo, useState } from 'react';
function parseFindings(raw) {
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw);
  } catch {
    return raw;
  }
}

export default function ExecutionCard({ execution, onReportCompletion, reporting }) {
  const [expanded, setExpanded] = useState(false);
  const [outcome, setOutcome] = useState('success');
  const [summary, setSummary] = useState('');
  const [findingsJson, setFindingsJson] = useState('');
  const [prUrl, setPrUrl] = useState('');

  const findings = useMemo(() => parseFindings(execution.findingsJson), [execution.findingsJson]);
  const canReport =
    execution.status === 'RUNNING' ||
    execution.status === 'PENDING' ||
    (execution.status === 'FAILED' && !execution.outcome);

  async function submitReport(e) {
    e.preventDefault();
    await onReportCompletion(execution.id, {
      outcome,
      summary: summary || undefined,
      findingsJson: findingsJson || undefined,
      prUrl: prUrl || undefined,
    });
    setSummary('');
    setFindingsJson('');
    setPrUrl('');
  }

  return (
    <article className="execution-card">
      <header className="execution-header">
        <div>
          <strong>{execution.role}</strong>
          <span className={`exec-status exec-${execution.status.toLowerCase()}`}>{execution.status}</span>
          {execution.outcome && <span className="muted"> · outcome: {execution.outcome}</span>}
        </div>
        <button type="button" className="btn-ghost" onClick={() => setExpanded((v) => !v)}>
          {expanded ? 'Hide' : 'Details'}
        </button>
      </header>

      <div className="execution-meta muted">
        {execution.branchName && <span>Branch: {execution.branchName}</span>}
        {execution.headSha && <span> · SHA {execution.headSha.slice(0, 7)}</span>}
        {execution.startedAt && <span> · started {new Date(execution.startedAt).toLocaleString()}</span>}
      </div>

      {execution.agentUrl && (
        <p>
          <a href={execution.agentUrl} target="_blank" rel="noreferrer" className="btn-link">
            Open Cursor agent ↗
          </a>
        </p>
      )}

      {execution.summary && (
        <div className="finding-block">
          <h4>Summary</h4>
          <p>{execution.summary}</p>
        </div>
      )}

      {expanded && findings && (
        <div className="finding-block">
          <h4>Findings</h4>
          <pre className="json-block">{typeof findings === 'string' ? findings : JSON.stringify(findings, null, 2)}</pre>
        </div>
      )}

      {canReport && onReportCompletion && (
        <form className="report-form" onSubmit={submitReport}>
          <h4>Report completion (manual)</h4>
          <p className="muted small">
            Use when the agent finished outside the webhook path, or to advance the workflow from the dashboard.
          </p>
          <label>
            Outcome
            <select value={outcome} onChange={(e) => setOutcome(e.target.value)}>
              <option value="success">success</option>
              <option value="failed">failed</option>
              <option value="rejected">rejected (reviewer)</option>
            </select>
          </label>
          <label>
            Summary
            <textarea rows={2} value={summary} onChange={(e) => setSummary(e.target.value)} placeholder="Short summary" />
          </label>
          <label>
            Findings JSON
            <textarea
              rows={3}
              value={findingsJson}
              onChange={(e) => setFindingsJson(e.target.value)}
              placeholder='{"items":[{"severity":"high","message":"..."}]}'
            />
          </label>
          {execution.role === 'DEVELOPER' && (
            <label>
              Merge request URL
              <input type="url" value={prUrl} onChange={(e) => setPrUrl(e.target.value)} placeholder="https://gitlab.../merge_requests/1" />
            </label>
          )}
          <button type="submit" disabled={reporting}>
            {reporting ? 'Saving…' : 'Submit completion'}
          </button>
        </form>
      )}
    </article>
  );
}
