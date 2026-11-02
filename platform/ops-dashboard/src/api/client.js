const API_BASE = import.meta.env.VITE_REGISTRY_URL || '';

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
    ...options,
  });
  if (!response.ok) {
    let message = response.statusText;
    try {
      const body = await response.json();
      if (body?.error) {
        message = body.error;
      }
    } catch {
      const text = await response.text();
      if (text) {
        message = text;
      }
    }
    throw new Error(message);
  }
  if (response.status === 204) {
    return null;
  }
  return response.json();
}

export function listTasks(state) {
  const query = state ? `?state=${encodeURIComponent(state)}&size=50` : '?size=50';
  return request(`/api/v1/tasks${query}`);
}

export function getTask(taskId) {
  return request(`/api/v1/tasks/${taskId}`);
}

export function getTaskActions(taskId) {
  return request(`/api/v1/tasks/${taskId}/actions`);
}

export function runDeveloper(taskId) {
  return request(`/api/v1/tasks/${taskId}/actions/run-developer`, {
    method: 'POST',
    body: JSON.stringify({ operatorId: 'dashboard-operator' }),
  });
}

export function runReviewer(taskId) {
  return request(`/api/v1/tasks/${taskId}/actions/run-reviewer`, {
    method: 'POST',
    body: JSON.stringify({ operatorId: 'dashboard-operator' }),
  });
}

export function runTester(taskId) {
  return request(`/api/v1/tasks/${taskId}/actions/run-tester`, {
    method: 'POST',
    body: JSON.stringify({ operatorId: 'dashboard-operator' }),
  });
}

export function reportCompletion(taskId, executionId, payload) {
  return request(`/api/v1/tasks/${taskId}/actions/executions/${executionId}/complete`, {
    method: 'POST',
    body: JSON.stringify({
      idempotencyKey: `dashboard-${executionId}-${Date.now()}`,
      operatorId: 'dashboard-operator',
      ...payload,
    }),
  });
}

export function humanApprove(taskId, mergedBy) {
  return request(`/api/v1/tasks/${taskId}/human-approve`, {
    method: 'POST',
    body: JSON.stringify({
      idempotencyKey: `human-${taskId}-${Date.now()}`,
      mergedBy: mergedBy || 'operator',
    }),
  });
}
