import { clearAuth, setAuth } from '$lib/stores/authStore.js';
import * as Sentry from '@sentry/sveltekit';

const API_BASE = '/api/v1/auth';

/**
 * @param {string} identifier - email or username (first step)
 * @param {object} [opts]
 * @param {string} [opts.resolvedUsername] - after 409 linked-account prompt; sends username + password only
 */
export async function loginUser(identifier, password, rememberMe = false, opts = {}) {
	const resolvedUsername = opts.resolvedUsername?.trim?.() || '';
	const body = resolvedUsername
		? { username: resolvedUsername, password, rememberMe }
		: { email: identifier, password, rememberMe };

	try {
		const res = await fetch(`${API_BASE}/login`, {
			method: 'POST',
			headers: { 'Content-Type': 'application/json' },
			body: JSON.stringify(body),
			credentials: 'include'
		});

		if (res.status === 409) {
			const data = await res.json().catch(() => ({}));
			const choices = Array.isArray(data.choices) ? data.choices : [];
			return {
				ok: false,
				roleChoiceRequired: true,
				message: typeof data.message === 'string' ? data.message : 'Choose how to sign in.',
				choices
			};
		}

		if (!res.ok) {
			const err = await res.json().catch(() => ({}));
			const message = err.message?.toLowerCase().includes('disabled')
				? 'Your account has been deactivated.'
				: 'Invalid email or password.';

			// Spring returns 401 for bad credentials
			Sentry.withScope((scope) => {
				scope.setTag('action', 'LOGIN_FAILED');
				scope.setTag('reason', 'invalid_credentials');
				scope.setTag('status_code', String(res.status));
				Sentry.captureMessage('Login failed: invalid credentials', 'warning');
			});
			return { ok: false, message };
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
		Sentry.captureException(e);
	}
	clearAuth();
}

// registers a new user
export async function registerUser(payload) {
	try {
		const res = await fetch(`${API_BASE}/register`, {
			method: 'POST',
			headers: { 'Content-Type': 'application/json' },
			body: JSON.stringify(payload),
			credentials: 'include'
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
		return {
			ok: true,
			employeeDiscountLinkEstablished: data.employeeDiscountLinkEstablished === true,
			employeeDiscountLinkMessage:
				typeof data.employeeDiscountLinkMessage === 'string'
					? data.employeeDiscountLinkMessage
					: null
		};
	} catch {
		Sentry.withScope((scope) => {
			scope.setTag('action', 'REGISTER_FAILED');
			scope.setTag('reason', 'network_error');
			Sentry.captureMessage('Registration failed: network error', 'error');
		});
		return { ok: false, message: 'Could not reach the server. Try again later.' };
	}
}

export async function forgotPassword(email) {
	try {
		await fetch(`${API_BASE}/forgot-password`, {
			method: 'POST',
			headers: { 'Content-Type': 'application/json' },
			body: JSON.stringify({ email })
		});
	} catch {
		Sentry.captureMessage("Failed to reach endpoint for 'forgot-password'", 'warning');
	}
}
