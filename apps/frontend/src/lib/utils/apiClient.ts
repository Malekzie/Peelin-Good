const API_BASE = '/api/v1';

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
	const response = await fetch(`${API_BASE}${path}`, {
		method,
		credentials: 'include',
		headers: {
			'Content-Type': 'application/json'
		},
		body: body !== undefined ? JSON.stringify(body) : undefined
	});

	if (response.status === 401) {
		if (typeof window !== 'undefined') {
			window.location.href = '/login';
		}
		throw new Error('Unauthorized');
	}

	if (response.status === 403) {
		if (typeof window !== 'undefined') {
			const redirectTo = encodeURIComponent(window.location.pathname + window.location.search);
			window.location.href = `/login?redirectTo=${redirectTo}`;
		}
		throw new Error('Forbidden');
	}

	if (!response.ok) {
		throw new Error(`HTTP ${response.status}`);
	}

	const text = await response.text();
	return text ? (JSON.parse(text) as T) : (undefined as T);
}

export const api = {
	get: <T>(path: string) => request<T>('GET', path),
	post: <T>(path: string, body?: unknown) => request<T>('POST', path, body),
	put: <T>(path: string, body?: unknown) => request<T>('PUT', path, body),
	delete: <T>(path: string) => request<T>('DELETE', path)
};
