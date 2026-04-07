const API = '/api/v1';

export async function getMyPreferences() {
	const res = await fetch(`${API}/customers/me/preferences`, {
		credentials: 'include'
	});
	if (!res.ok) throw new Error('Failed to fetch preferences: ' + res.status);
	return res.json();
}

export async function saveMyPreferences(preferences) {
	const res = await fetch(`${API}/customers/me/preferences`, {
		method: 'PUT',
		credentials: 'include',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify({ preferences })
	});
	if (!res.ok) throw new Error('Failed to save preferences: ' + res.status);
	return res.json();
}
