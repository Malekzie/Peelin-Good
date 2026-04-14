const API_BASE = '/api/v1';

function getToken(): string | null {
	if (typeof window === 'undefined') return null;

	const localToken = window.localStorage.getItem('token');
	if (localToken && localToken.trim().length > 0) {
		return localToken;
	}

	return null;
}

function authHeaders(): Record<string, string> {
	const token = getToken();
	return token ? { Authorization: `Bearer ${token}` } : {};
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
	const response = await fetch(`${API_BASE}${path}`, {
		method,
		credentials: 'include',
		headers: {
			'Content-Type': 'application/json',
			...authHeaders()
		},
		body: body !== undefined ? JSON.stringify(body) : undefined
	});

	if (response.status === 401) {
		if (typeof window !== 'undefined') {
			window.localStorage.removeItem('token');
			window.location.href = '/login';
		}
		throw new Error('Unauthorized');
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