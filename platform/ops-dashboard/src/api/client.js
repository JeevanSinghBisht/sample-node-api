const API_BASE = import.meta.env.VITE_REGISTRY_URL || '';

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
    ...options,
  });
  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || response.statusText);
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

export function humanApprove(taskId, mergedBy) {
  return request(`/api/v1/tasks/${taskId}/human-approve`, {
    method: 'POST',
    body: JSON.stringify({
      idempotencyKey: `human-${taskId}-${Date.now()}`,
      mergedBy: mergedBy || 'operator',
    }),
  });
}
