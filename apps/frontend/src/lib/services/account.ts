import { apiFetch } from '$lib/utils/api';
import { ACCOUNT_API } from '$lib/services/constants';

export async function changePassword(currentPassword: string, newPassword: string): Promise<void> {
	const res = await apiFetch(`${ACCOUNT_API}/password`, {
		method: 'PUT',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify({ currentPassword, newPassword })
	});

	if (!res) return;
	if (!res.ok) {
		const err = (await res.json().catch(() => ({}))) as { message?: string };
		throw new Error(err.message ?? 'Failed to change password.');
	}
}
