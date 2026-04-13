import { ADMIN_USERS_API } from '$lib/services/constants';
import type { ApiId, UserRecord } from '$lib/services/types';

export async function listUsers(): Promise<UserRecord[]> {
	const res = await fetch(ADMIN_USERS_API, { credentials: 'include' });
	if (!res.ok) throw new Error('Failed to fetch users');
	return res.json();
}

export async function setUserActive(id: ApiId, active: boolean): Promise<UserRecord> {
	const res = await fetch(`${ADMIN_USERS_API}/${id}/active`, {
		method: 'PATCH',
		headers: { 'Content-Type': 'application/json' },
		credentials: 'include',
		body: JSON.stringify({ active })
	});
	if (!res.ok) throw new Error('Failed to update user');
	return res.json();
}
