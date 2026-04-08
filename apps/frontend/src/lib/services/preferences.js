import { apiFetch } from '$lib/utils/api';
const API = '/api/v1';

export async function getMyPreferences() {
	const res = await apiFetch(`${API}/customers/me/preferences`);

	if (!res) return;
	if (!res.ok) throw new Error('Failed to fetch preferences: ' + res.status);
	return res.json();
}

export async function saveMyPreferences(preferences) {
	const res = await apiFetch(`${API}/customers/me/preferences`, {
		method: 'PUT',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify({ preferences })
	});

	if (!res) return;
	if (!res.ok) throw new Error('Failed to save preferences: ' + res.status);
	return res.json();
}
