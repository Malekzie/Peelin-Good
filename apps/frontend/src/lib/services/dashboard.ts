import { DASHBOARD_API } from '$lib/services/constants';

export async function getDashboardSummary(): Promise<Record<string, unknown>> {
	const res = await fetch(`${DASHBOARD_API}/summary`, {
		credentials: 'include'
	});
	if (!res.ok) throw new Error('Failed to fetch dashboard summary');
	return res.json();
}
