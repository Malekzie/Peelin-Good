import { writable, derived } from 'svelte/store';
import { browser } from '$app/environment';

const storedUser = browser ? JSON.parse(localStorage.getItem('user') ?? 'null') : null;

export const user = writable(storedUser);

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
	}
}

export function clearAuth() {
	user.set(null);

	if (browser) {
		localStorage.removeItem('user');
	}
}
