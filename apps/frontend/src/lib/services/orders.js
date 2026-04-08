import { apiFetch } from '$lib/utils/api';
const API = '/api/v1';

export async function getMyOrders() {
	const res = await apiFetch(`${API}/orders`);

	if (!res) return;
	if (!res.ok) throw new Error('Failed to fetch orders: ' + res.status);
	return res.json();
}
