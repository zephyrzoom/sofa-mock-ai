const API_BASE = '/api';

const api = {
    async request(method, path, body) {
        const opts = {
            method,
            headers: { 'Content-Type': 'application/json' }
        };
        if (body !== undefined) {
            opts.body = typeof body === 'string' ? body : JSON.stringify(body);
        }
        const resp = await fetch(API_BASE + path, opts);
        if (!resp.ok) {
            const text = await resp.text();
            throw new Error(text || resp.statusText);
        }
        const contentType = resp.headers.get('content-type');
        if (contentType && contentType.includes('application/json')) {
            return resp.json();
        }
        return resp.text();
    },

    // Cases
    getCases(params) {
        const qs = params ? '?' + new URLSearchParams(params).toString() : '';
        return this.request('GET', '/cases' + qs);
    },
    getCase(id) {
        return this.request('GET', '/cases/' + id);
    },
    createCase(data) {
        return this.request('POST', '/cases', data);
    },
    updateCase(id, data) {
        return this.request('PUT', '/cases/' + id, data);
    },
    deleteCase(id) {
        return this.request('DELETE', '/cases/' + id);
    },
    exportCases() {
        return this.request('GET', '/cases/export');
    },
    uploadCases(file) {
        const formData = new FormData();
        formData.append('file', file);
        return fetch(API_BASE + '/cases/upload', {
            method: 'POST',
            body: formData
        }).then(r => r.json());
    },

    // Agents
    getAgents() {
        return this.request('GET', '/agents');
    },
    getOnlineAgents() {
        return this.request('GET', '/agents/online');
    },

    // Apps
    getApps() {
        return this.request('GET', '/apps');
    },

    // Stats (from agent management API)
    getAgentStats(agentUrl) {
        return fetch(agentUrl + '/mock/stats').then(r => r.json());
    },
    getAgentStatus(agentUrl) {
        return fetch(agentUrl + '/mock/status').then(r => r.json());
    }
};
