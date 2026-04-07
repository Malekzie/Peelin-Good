import { clearAuth, setAuth, token } from '$lib/stores/authStore.js';
import * as Sentry from '@sentry/sveltekit';

const API_BASE = '/api/v1/auth';

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
			Sentry.withScope((scope) => {
				scope.setTag('action', 'LOGIN_FAILED');
				scope.setTag('reason', 'invalid_credentials');
				scope.setTag('status_code', String(res.status));
				Sentry.captureMessage('Login failed: invalid credentials', 'warning');
			});
			return { ok: false, message: 'Invalid email or password.' };
		}

		const data = await res.json();

		// saves to store + localStorage
		setAuth(data);
		return { ok: true };
	} catch {
		Sentry.withScope((scope) => {
			scope.setTag('action', 'LOGIN_FAILED');
			scope.setTag('reason', 'network_error');
			Sentry.captureMessage('Login failed: network error', 'error');
		});
		return { ok: false, message: 'Could not reach the server. Try again later.' };
	}
}

// logs out the current user, invalidating the token on the server
export async function logoutUser() {
	try {
		await fetch(`${API_BASE}/logout`, {
			method: 'POST',
			credentials: 'include'
		});
	} catch (e) {
		Sentry.captureMessage(`Logout failed: `, e);
	}
	clearAuth();
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
			Sentry.withScope((scope) => {
				scope.setTag('action', 'REGISTER_FAILED');
				scope.setTag('reason', res.status === 409 ? 'duplicate_account' : 'api_error');
				scope.setTag('status_code', String(res.status));
				Sentry.captureMessage(
					`Registration failed: HTTP ${res.status}`,
					res.status >= 500 ? 'error' : 'warning'
				);
			});
			return { ok: false, message: err.message ?? 'Registration failed.' };
		}

		const data = await res.json();
		setAuth(data);
		return { ok: true };
	} catch {
		Sentry.withScope((scope) => {
			scope.setTag('action', 'REGISTER_FAILED');
			scope.setTag('reason', 'network_error');
			Sentry.captureMessage('Registration failed: network error', 'error');
		});
		return { ok: false, message: 'Could not reach the server. Try again later.' };
	}
}
