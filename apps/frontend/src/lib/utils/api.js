import { clearAuth, user } from '$lib/stores/authStore';
import { get } from 'svelte/store';

export async function apiFetch(url, options = {}) {
	const res = await fetch(url, {
		credentials: 'include',
		...options
	});

	if (res.status === 401) {
		const wasLoggedIn = !!get(user);
		clearAuth();
		if (wasLoggedIn) {
			try {
				await fetch('/api/v1/auth/logout', { method: 'POST', credentials: 'include' });
			} catch {}
			window.location.href = '/deactivated';
		} else {
			window.location.href = '/login';
		}
		return null;
	}

	return res;
}
