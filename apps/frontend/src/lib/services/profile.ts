import { get } from 'svelte/store';
import { user } from '$lib/stores/authStore.js';
import { apiFetch } from '$lib/utils/api';
import { ACCOUNT_API, API_V1, PROFILE_API, EMPLOYEE_API } from '$lib/services/constants';
import type { CustomerRecord, ErrorWithStatus, ProductRecord } from '$lib/services/types';

type SessionUser = { role?: string } | null;

export async function getProfile(): Promise<CustomerRecord | undefined> {
	const currentUser = get(user) as SessionUser;
	const role = (currentUser?.role ?? '').toLowerCase();

	const url = role === 'employee' || role === 'admin' ? `${EMPLOYEE_API}/me` : PROFILE_API;

	const res = await apiFetch(url);

	if (!res) return;

	if (!res.ok) {
		const err = new Error(`Failed to fetch profile: ${res.status}`) as ErrorWithStatus;
		err.status = res.status;
		throw err;
	}

	const data = (await res.json()) as CustomerRecord;

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

export async function deleteAccount(): Promise<void> {
	const res = await apiFetch(PROFILE_API, {
		method: 'DELETE'
	});

	if (!res) return;
	if (!res.ok) throw new Error(`Failed to delete account: ${res.status}`);
}

export async function updateProfile(
	profileData: Record<string, unknown>
): Promise<CustomerRecord | undefined> {
	const res = await apiFetch(PROFILE_API, {
		method: 'PATCH',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify(profileData)
	});

	if (!res) return;
	if (!res.ok) throw new Error(`Failed to update profile: ${res.status}`);
	return res.json();
}

export async function bootstrapCustomerProfile(
	profileData: Record<string, unknown>
): Promise<CustomerRecord | undefined> {
	const res = await apiFetch(PROFILE_API, {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify(profileData)
	});
	if (!res) return;
	if (!res.ok) {
		const err = (await res.json().catch(() => ({}))) as { message?: string };
		const error = new Error(
			err.message ?? `Failed to create customer profile: ${res.status}`
		) as ErrorWithStatus;
		error.status = res.status;
		throw error;
	}
	return res.json();
}

export async function uploadProfilePhoto(file: File): Promise<CustomerRecord | undefined> {
	const formData = new FormData();
	formData.append('photo', file);
	const res = await apiFetch(`${ACCOUNT_API}/profile-photo`, {
		method: 'POST',
		body: formData
	});
	if (!res) return;
	if (!res.ok) throw new Error(`Failed to upload profile photo: ${res.status}`);
	return res.json();
}

export async function deactivateAccount(currentPassword: string): Promise<void> {
	const res = await apiFetch(`${ACCOUNT_API}/deactivate`, {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify({ currentPassword })
	});

	if (!res) return;
	if (!res.ok) throw new Error(`Failed to deactivate account: ${res.status}`);
}

export async function getRecommendations(): Promise<ProductRecord[]> {
	const res = await apiFetch(`${API_V1}/recommendations`);

	if (!res) return [];
	if (!res.ok) throw new Error(`Failed to fetch recommendations: ${res.status}`);
	return res.json();
}
