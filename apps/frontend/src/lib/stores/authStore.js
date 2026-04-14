import { writable, derived } from 'svelte/store';
import { browser } from '$app/environment';
import * as Sentry from '@sentry/sveltekit';

const storedUser = browser ? JSON.parse(localStorage.getItem('user') ?? 'null') : null;

export const user = writable(storedUser);

if (browser && storedUser) {
	Sentry.setUser({ id: storedUser.userId, username: storedUser.username });
	Sentry.setTag('role', (storedUser.role ?? '').toLowerCase());
}

// Reconcile localStorage with the backend's JWT identity so split-brain sessions
// (stale localStorage + different cookie) can't cause wrong-user bugs in the UI.
if (browser) {
	fetch('/api/v1/auth/whoami', { credentials: 'include' })
		.then((r) => (r.ok ? r.json() : { authenticated: false }))
		.then((d) => {
			if (d.authenticated) {
				const fresh = { userId: d.userId, username: d.username, role: d.role };
				const current = JSON.parse(localStorage.getItem('user') ?? 'null');
				if (!current || String(current.userId) !== String(fresh.userId)) {
					localStorage.setItem('user', JSON.stringify(fresh));
					user.set(fresh);
					Sentry.setUser({ id: fresh.userId, username: fresh.username });
					Sentry.setTag('role', (fresh.role ?? '').toLowerCase());
				}
			} else if (localStorage.getItem('user')) {
				localStorage.removeItem('user');
				user.set(null);
				Sentry.setUser(null);
				Sentry.setTag('role', '');
			}
		})
		.catch(() => {
			// Network error — leave stored state alone.
		});
}

export const isLoggedIn = derived(user, ($user) => !!$user);

export function setAuth(authResponse) {
	user.set({
		userId: authResponse.userId,
		username: authResponse.username,
		role: authResponse.role
	});

	if (browser) {
		localStorage.setItem(
			'user',
			JSON.stringify({
				userId: authResponse.userId,
				username: authResponse.username,
				role: authResponse.role
			})
		);
		Sentry.setUser({ id: authResponse.userId, username: authResponse.username });
		Sentry.setTag('role', (authResponse.role ?? '').toLowerCase());
	}
}

export function clearAuth() {
	user.set(null);

	if (browser) {
		localStorage.removeItem('user');
		Sentry.setUser(null);
		Sentry.setTag('role', '');
	}
}
