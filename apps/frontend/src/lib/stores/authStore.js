import { writable, derived } from 'svelte/store';
import { browser } from '$app/environment';

const storedToken = browser ? localStorage.getItem('token') : null;
const storedUser = browser ? JSON.parse(localStorage.getItem('user') ?? 'null') : null;

export const token = writable(storedToken);
export const user = writable(storedUser);

export const isLoggedIn = derived(token, ($token) => !!$token);

export function setAuth(authResponse) {
	token.set(authResponse.token);
	user.set({
		userId: authResponse.userId,
		username: authResponse.username,
		role: authResponse.role
	});

	if (browser) {
		localStorage.setItem('token', authResponse.token);
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
	token.set(null);
	user.set(null);

	if (browser) {
		localStorage.removeItem('token');
		localStorage.removeItem('user');
	}
}
