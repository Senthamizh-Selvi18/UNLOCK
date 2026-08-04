const API_BASE = 'http://localhost:8080';

function getCsrfToken() {
    const match = document.cookie.match(/(?:^|; )XSRF-TOKEN=([^;]*)/);
    return match ? decodeURIComponent(match[1]) : null;
}

/** Same idea as the plain-JS apiFetch() from earlier weeks, now the single shared entry point for every API call. */
async function apiFetch(path, options = {}) {
    const method = (options.method || 'GET').toUpperCase();
    const headers = { ...(options.headers || {}) };

    if (method !== 'GET') {
        headers['X-XSRF-TOKEN'] = getCsrfToken();
    }

    const response = await fetch(`${API_BASE}${path}`, {
        ...options,
        method,
        headers,
        credentials: 'include'
    });

    return response;
}

async function apiJson(path, options = {}) {
    const response = await apiFetch(path, options);
    if (response.status === 204) return null; // "nothing due right now" etc - not an error
    if (!response.ok) {
        const err = new Error(`Request failed: ${response.status}`);
        err.status = response.status;
        throw err;
    }
    return response.json();
}

export const api = {
    // Auth / profile
    getMe: () => apiJson('/api/me'),
    loginUrl: `${API_BASE}/oauth2/authorization/github`,

    // Entries / Evidence Vault
    getEntries: () => apiJson('/api/entries'),
    syncGitHub: () => apiJson('/api/entries/sync', { method: 'POST' }),
    addManualEntry: (title, description) =>
        apiJson('/api/entries/manual', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ title, description })
        }),
    markComplete: (id, completed) =>
        apiJson(`/api/entries/${id}/complete`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ completed })
        }),

    // Auth: logout ends the Spring Security session and clears cookies server-side.
    logout: () => apiFetch('/logout', { method: 'POST' }),

    // Reflections
    getCurrentReflection: () => apiJson('/api/reflections/current'),
    getReflectionHistory: () => apiJson('/api/reflections'),
    answerReflection: (id, answer) =>
        apiJson(`/api/reflections/${id}/answer`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ answer })
        }),
    generateReflection: () => apiJson('/api/reflections/generate', { method: 'POST' }),

    // Patterns
    getPatterns: () => apiJson('/api/patterns'),
    scanPatterns: () => apiJson('/api/patterns/scan', { method: 'POST' }),
    confirmPattern: (id, confirmed) =>
        apiJson(`/api/patterns/${id}/confirm`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ confirmed })
        }),
    dismissSuggestion: (id) => apiJson(`/api/patterns/${id}/dismiss-suggestion`, { method: 'POST' }),

    // Growth Replay
    getReplay: () => apiJson('/api/replay'),

    // Data control
    exportData: () => apiFetch('/api/data/export'), // returns raw response - caller handles the file download
    deleteAllData: () => apiFetch('/api/data/delete-all', { method: 'DELETE' })
};
