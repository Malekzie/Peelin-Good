import { apiFetch } from '$lib/utils/api';
import { PREFERENCES_API } from '$lib/services/constants';

export async function getMyPreferences(): Promise<Record<string, unknown> | undefined> {
	const res = await apiFetch(PREFERENCES_API);

	if (!res) return;
	if (!res.ok) throw new Error(`Failed to fetch preferences: ${res.status}`);
	return res.json();
}

export async function saveMyPreferences(
	preferences: unknown
): Promise<Record<string, unknown> | undefined> {
	const res = await apiFetch(PREFERENCES_API, {
		method: 'PUT',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify({ preferences })
	});

	if (!res) return;
	if (!res.ok) throw new Error(`Failed to save preferences: ${res.status}`);
	return res.json();
}
