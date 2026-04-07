const API = '/api/v1/admin/users';

export async function listUsers() {
	const res = await fetch(API, { credentials: 'include' });
	if (!res.ok) throw new Error('Failed to fetch users');
	return res.json();
}

export async function setUserActive(id, active) {
	const res = await fetch(`${API}/${id}/active`, {
		method: 'PATCH',
		headers: { 'Content-Type': 'application/json' },
		credentials: 'include',
		body: JSON.stringify({ active })
	});
	if (!res.ok) throw new Error('Failed to update user');
	return res.json();
}
