import { get } from 'svelte/store';
import { token, user } from '$lib/stores/authStore.js';

const API = 'http://localhost:8080/api/v1';

function authHeaders() {
	const t = get(token);
	const headers = { 'Content-Type': 'application/json' };
	if (t) headers['Authorization'] = `Bearer ${t}`;
	return headers;
}

export async function getProfile() {
	const currentUser = get(user);
	const role = (currentUser?.role ?? '').toLowerCase();

	const url =
		role === 'employee' || role === 'admin'
			? `${API}/employee/me`
			: `${API}/customers/me`;

	const res = await fetch(url, { headers: authHeaders() });
	if (!res.ok) throw new Error('Failed to fetch profile: ' + res.status);

	const data = await res.json();

	if (role === 'employee' || role === 'admin') {
		return {
			...data,
			email: data.workEmail ?? null,
			loyaltyTier: null,
			rewardBalance: null
		};
	}

	return data;
}
