import { setAuth } from '$lib/stores/authStore.js';

const API_BASE = 'http://localhost:8080/api/v1/auth';

// Log in with email and password, returns {ok: boolean, message?: string}
export async function loginUser(email, password) {
	try {
		const res = await fetch(`${API_BASE}/login`, {
			method: 'POST',
			headers: { 'Content-Type': 'application/json' },
			body: JSON.stringify({ email, password })
		});

		if (!res.ok) {
			// Spring returns 401 for bad credentials
			return { ok: false, message: 'Invalid email or password.' };
		}

		const data = await res.json();

		// saves to store + localStorage
		setAuth(data);
		return { ok: true };
	} catch {
		return { ok: false, message: 'Could not reach the server. Try again later.' };
	}
}

// registers a new user
export async function registerUser(payload) {
	try {
		const res = await fetch(`${API_BASE}/register`, {
			method: 'POST',
			headers: { 'Content-Type': 'application/json' },
			body: JSON.stringify(payload)
		});

		if (!res.ok) {
			const err = await res.json().catch(() => ({}));
			return { ok: false, message: err.message ?? 'Registration failed.' };
		}

		const data = await res.json();
		setAuth(data);
		return { ok: true };
	} catch {
		return { ok: false, message: 'Could not reach the server. Try again later.' };
	}
}
