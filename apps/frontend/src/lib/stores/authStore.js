import { writable, derived } from 'svelte/store';
import { browser } from '$app/environment';
import * as Sentry from '@sentry/sveltekit';

const storedUser = browser ? JSON.parse(localStorage.getItem('user') ?? 'null') : null;

export const user = writable(storedUser);

if (browser && storedUser) {
	Sentry.setUser({ id: storedUser.userId, username: storedUser.username });
	Sentry.setTag('role', (storedUser.role ?? '').toLowerCase());
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
