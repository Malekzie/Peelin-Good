import { get } from 'svelte/store';
import { user } from '$lib/stores/authStore.js';
import { apiFetch } from '$lib/utils/api';

const API = '/api/v1';

export async function getProfile() {
	const currentUser = get(user);
	const role = (currentUser?.role ?? '').toLowerCase();

	const url =
		role === 'employee' || role === 'admin' ? `${API}/employee/me` : `${API}/customers/me`;

	const res = await apiFetch(url);

	if (!res) return;

	if (!res.ok) {
		const err = new Error('Failed to fetch profile: ' + res.status);
		err.status = res.status;
		throw err;
	}

	const data = await res.json();

	if (role === 'employee' || role === 'admin') {
		return {
			...data,
			email: data.workEmail ?? null,
			loyaltyTier: null,
			rewardBalance: null
		};
	}

	return {
		...data,
		loyaltyTier: data.rewardTierName ?? null
	};
}

export async function deleteAccount() {
	const res = await apiFetch(`${API}/customers/me`, {
		method: 'DELETE'
	});

	if (!res) return;
	if (!res.ok) throw new Error('Failed to delete account: ' + res.status);
}

export async function updateProfile(profileData) {
	const res = await apiFetch(`${API}/customers/me`, {
		method: 'PATCH',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify(profileData)
	});

	if (!res) return;
	if (!res.ok) throw new Error('Failed to update profile: ' + res.status);
	return res.json();
}

export async function bootstrapCustomerProfile(profileData) {
	const res = await apiFetch(`${API}/customers/me`, {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify(profileData)
	});
	if (!res) return;
	if (!res.ok) {
		const err = await res.json().catch(() => ({}));
		const error = new Error(err.message ?? `Failed to create customer profile: ${res.status}`);
		error.status = res.status;
		throw error;
	}
	return res.json();
}

export async function uploadProfilePhoto(file) {
	const formData = new FormData();
	formData.append('photo', file);
	const res = await apiFetch(`${API}/account/profile-photo`, {
		method: 'POST',
		body: formData
	});
	if (!res) return;
	if (!res.ok) throw new Error('Failed to upload profile photo: ' + res.status);
	return res.json();
}

export async function deactivateAccount() {
	const res = await apiFetch(`${API}/customers/me/deactivate`, {
		method: 'PATCH'
	});

	if (!res) return;
	if (!res.ok) throw new Error('Failed to deactivate account: ' + res.status);
}

export async function getRecommendations() {
	const res = await apiFetch(`${API}/recommendations`);

	if (!res) return [];
	if (!res.ok) throw new Error('Failed to fetch recommendations: ' + res.status);
	return res.json();
}
