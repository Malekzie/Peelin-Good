import { apiFetch } from '$lib/utils/api';
import { ORDERS_API } from '$lib/services/constants';
import type { OrderRecord } from '$lib/services/types';

export async function getMyOrders(): Promise<OrderRecord[] | undefined> {
	const res = await apiFetch(ORDERS_API);

	if (!res) return;
	if (!res.ok) throw new Error(`Failed to fetch orders: ${res.status}`);
	return res.json();
}

export async function resumeStripePayment(orderId: string): Promise<{
	orderId: string;
	orderNumber: string;
	clientSecret: string;
	paymentIntentId: string;
	orderPaid: boolean;
}> {
	const res = await apiFetch(`${ORDERS_API}/${orderId}/resume-stripe-payment`, {
		method: 'POST'
	});
	if (!res) throw new Error('No response');
	if (!res.ok) throw new Error(`Failed to resume payment: ${res.status}`);
	return res.json();
}

export async function getStripePublishableKey(): Promise<string> {
	const res = await fetch('/api/v1/stripe/config');
	if (!res.ok) throw new Error('Failed to fetch Stripe config');
	const data = await res.json();
	return data.publishableKey;
}
