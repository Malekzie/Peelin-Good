import { ORDERS_API } from '$lib/services/constants';
import type { ApiId, OrderRecord } from '$lib/services/types';

export async function updateOrderStatus(orderId: ApiId, status: string): Promise<OrderRecord> {
	const res = await fetch(`${ORDERS_API}/${orderId}/status`, {
		method: 'PATCH',
		headers: { 'Content-Type': 'application/json' },
		credentials: 'include',
		body: JSON.stringify({ status })
	});
	if (!res.ok) throw new Error('Failed to update order status');
	return res.json();
}

export async function markDelivered(orderId: ApiId): Promise<OrderRecord> {
	const res = await fetch(`${ORDERS_API}/${orderId}/delivered`, {
		method: 'PATCH',
		credentials: 'include'
	});
	if (!res.ok) throw new Error('Failed to mark order delivered');
	return res.json();
}
