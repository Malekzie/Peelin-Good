const API_BASE = '/api/v1/customers/me';

export async function getProfile() {
	const res = await fetch(API_BASE, {
		credentials: 'include'
	});

	if (!res.ok) throw new Error('Failed to fetch profile: ' + res.status);
	return await res.json();
}
