import React from 'react';

const STATE_CLASS = {
  READY: 'state-ready',
  DEVELOPING: 'state-developing',
  DEV_FAILED: 'state-failed',
  IN_REVIEW: 'state-review',
  REVIEW_REJECTED: 'state-failed',
  IN_QA: 'state-qa',
  QA_FAILED: 'state-failed',
  AWAITING_HUMAN: 'state-human',
  DONE: 'state-done',
  CANCELLED: 'state-muted',
  DRAFT: 'state-muted',
};

export default function StateBadge({ state }) {
  const cls = STATE_CLASS[state] || 'state-muted';
  return <span className={`state-badge ${cls}`}>{state.replace(/_/g, ' ')}</span>;
}
