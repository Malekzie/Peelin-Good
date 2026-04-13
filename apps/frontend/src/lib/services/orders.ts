import { apiFetch } from '$lib/utils/api';
import { ORDERS_API } from '$lib/services/constants';
import type { OrderRecord } from '$lib/services/types';

export async function getMyOrders(): Promise<OrderRecord[] | undefined> {
	const res = await apiFetch(ORDERS_API);

	if (!res) return;
	if (!res.ok) throw new Error(`Failed to fetch orders: ${res.status}`);
	return res.json();
}
