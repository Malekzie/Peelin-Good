import { get } from 'svelte/store';
import { user } from '$lib/stores/authStore.js';

const API = '/api/v1';

export async function getProfile() {
	const currentUser = get(user);
	const role = (currentUser?.role ?? '').toLowerCase();

	const url =
		role === 'employee' || role === 'admin' ? `${API}/employee/me` : `${API}/customers/me`;

	const res = await fetch(url, { credentials: 'include' });
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

export async function updateProfile(profileData) {
	const res = await fetch(`${API}/customers/me`, {
		method: 'PATCH',
		credentials: 'include',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify(profileData)
	});

	if (!res.ok) throw new Error('Failed to update profile: ' + res.status);
	return res.json();
}

export async function deleteAccount() {
	const res = await fetch(`${API}/customers/me`, {
		method: 'DELETE',
		credentials: 'include'
	});

	if (!res.ok) throw new Error('Failed to delete account: ' + res.status);
}
