const API = '/api/v1';

export async function getMyOrders() {
	const res = await fetch(`${API}/orders`, {
		credentials: 'include'
	});

	if (!res.ok) throw new Error('Failed to fetch orders: ' + res.status);
	return res.json();
}
