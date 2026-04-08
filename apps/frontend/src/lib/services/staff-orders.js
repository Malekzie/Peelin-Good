const API = '/api/v1';

export async function updateOrderStatus(orderId, status) {
	const res = await fetch(`${API}/orders/${orderId}/status`, {
		method: 'PATCH',
		headers: { 'Content-Type': 'application/json' },
		credentials: 'include',
		body: JSON.stringify({ status })
	});
	if (!res.ok) throw new Error('Failed to update order status');
	return res.json();
}

export async function markDelivered(orderId) {
	const res = await fetch(`${API}/orders/${orderId}/delivered`, {
		method: 'PATCH',
		credentials: 'include'
	});
	if (!res.ok) throw new Error('Failed to mark order delivered');
	return res.json();
}
