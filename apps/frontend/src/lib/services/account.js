import { apiFetch } from '$lib/utils/api';

const API = '/api/v1/account';

export async function changePassword(currentPassword, newPassword) {
	const res = await apiFetch(`${API}/password`, {
		method: 'PUT',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify({ currentPassword, newPassword })
	});

	if (!res) return;
	if (!res.ok) {
		const err = await res.json().catch(() => ({}));
		throw new Error(err.message ?? 'Failed to change password.');
	}
}
