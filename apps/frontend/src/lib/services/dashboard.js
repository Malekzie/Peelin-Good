const API = '/api/v1';

export async function getDashboardSummary() {
	const res = await fetch(`${API}/admin/dashboard/summary`, {
		credentials: 'include'
	});
	if (!res.ok) throw new Error('Failed to fetch dashboard summary');
	return res.json();
}
