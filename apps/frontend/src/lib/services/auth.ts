import { clearAuth, setAuth } from '$lib/stores/authStore.js';
import * as Sentry from '@sentry/sveltekit';
import { AUTH_API } from '$lib/services/constants';
import type { AuthResponse, LoginResult, RegisterResult } from '$lib/services/types';

type LoginOptions = {
	resolvedUsername?: string;
};

export async function loginUser(
	identifier: string,
	password: string,
	rememberMe = false,
	opts: LoginOptions = {}
): Promise<LoginResult> {
	const resolvedUsername = opts.resolvedUsername?.trim?.() || '';
	const body = resolvedUsername
		? { username: resolvedUsername, password, rememberMe }
		: { email: identifier, password, rememberMe };

	try {
		const res = await fetch(`${AUTH_API}/login`, {
			method: 'POST',
			headers: { 'Content-Type': 'application/json' },
			body: JSON.stringify(body),
			credentials: 'include'
		});

		if (res.status === 409) {
			const data = (await res.json().catch(() => ({}))) as {
				message?: string;
				choices?: unknown[];
			};
			const choices = Array.isArray(data.choices) ? data.choices : [];
			return {
				ok: false,
				roleChoiceRequired: true,
				message: typeof data.message === 'string' ? data.message : 'Choose how to sign in.',
				choices
			};
		}

		if (!res.ok) {
			const err = (await res.json().catch(() => ({}))) as { message?: string };
			const message = err.message?.toLowerCase().includes('disabled')
				? 'Your account has been deactivated.'
				: 'Invalid email or password.';

			Sentry.withScope((scope) => {
				scope.setTag('action', 'LOGIN_FAILED');
				scope.setTag('reason', 'invalid_credentials');
				scope.setTag('status_code', String(res.status));
				Sentry.captureMessage('Login failed: invalid credentials', 'warning');
			});
			return { ok: false, message };
		}

		const data = (await res.json()) as AuthResponse;
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

export async function logoutUser(): Promise<void> {
	try {
		await fetch(`${AUTH_API}/logout`, {
			method: 'POST',
			credentials: 'include'
		});
	} catch (error) {
		Sentry.captureException(error);
	} finally {
		// Ensure frontend-visible auth cookies are removed even if backend logout is rejected.
		await fetch('/auth/local-logout', {
			method: 'POST',
			credentials: 'include'
		}).catch(() => {
			/* no-op */
		});
	}
	clearAuth();
}

export type RegisterAvailabilityResult =
	| { ok: true; usernameAvailable: boolean; emailAvailable: boolean }
	| { ok: false; message: string };

/**
 * Pre-check before step 2 of registration (matches Android / backend case-insensitive rules).
 */
export async function fetchRegisterAvailability(
	username: string,
	email: string
): Promise<RegisterAvailabilityResult> {
	try {
		const params = new URLSearchParams();
		params.set('username', username.trim());
		params.set('email', email.trim().toLowerCase());
		const res = await fetch(`${AUTH_API}/register/availability?${params.toString()}`, {
			credentials: 'include'
		});
		if (!res.ok) {
			return { ok: false, message: 'Could not verify account details. Try again.' };
		}
		const data = (await res.json()) as {
			usernameAvailable?: boolean;
			emailAvailable?: boolean;
		};
		return {
			ok: true,
			usernameAvailable: data.usernameAvailable !== false,
			emailAvailable: data.emailAvailable !== false
		};
	} catch {
		return { ok: false, message: 'Could not reach the server. Try again later.' };
	}
}

export async function registerUser(payload: Record<string, unknown>): Promise<RegisterResult> {
	try {
		const res = await fetch(`${AUTH_API}/register`, {
			method: 'POST',
			headers: { 'Content-Type': 'application/json' },
			body: JSON.stringify(payload),
			credentials: 'include'
		});

		if (!res.ok) {
			const err = (await res.json().catch(() => ({}))) as { message?: string };
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

		const data = (await res.json()) as AuthResponse;
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

export async function forgotPassword(email: string): Promise<void> {
	try {
		await fetch(`${AUTH_API}/forgot-password`, {
			method: 'POST',
			headers: { 'Content-Type': 'application/json' },
			body: JSON.stringify({ email })
		});
	} catch {
		Sentry.captureMessage("Failed to reach endpoint for 'forgot-password'", 'warning');
	}
}
