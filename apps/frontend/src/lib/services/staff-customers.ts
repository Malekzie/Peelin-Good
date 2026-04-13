import { ADMIN_CUSTOMERS_API } from '$lib/services/constants';
import type { ApiId, CustomerRecord } from '$lib/services/types';

export async function listCustomers(search = ''): Promise<CustomerRecord[]> {
	const url = search
		? `${ADMIN_CUSTOMERS_API}?search=${encodeURIComponent(search)}`
		: ADMIN_CUSTOMERS_API;
	const res = await fetch(url, { credentials: 'include' });
	if (!res.ok) throw new Error('Failed to fetch customers');
	return res.json();
}

export async function getCustomer(id: ApiId): Promise<CustomerRecord> {
	const res = await fetch(`${ADMIN_CUSTOMERS_API}/${id}`, { credentials: 'include' });
	if (!res.ok) throw new Error('Customer not found');
	return res.json();
}

export async function patchCustomer(
	id: ApiId,
	data: Record<string, unknown>
): Promise<CustomerRecord> {
	const res = await fetch(`${ADMIN_CUSTOMERS_API}/${id}`, {
		method: 'PATCH',
		headers: { 'Content-Type': 'application/json' },
		credentials: 'include',
		body: JSON.stringify(data)
	});
	if (!res.ok) throw new Error('Failed to update customer');
	return res.json();
}

export async function getPendingPhotos(): Promise<CustomerRecord[]> {
	const res = await fetch(`${ADMIN_CUSTOMERS_API}/pending-photos`, { credentials: 'include' });
	if (!res.ok) throw new Error('Failed to fetch pending photos');
	return res.json();
}

export async function approvePhoto(id: ApiId): Promise<void> {
	const res = await fetch(`${ADMIN_CUSTOMERS_API}/${id}/approve-photo`, {
		method: 'POST',
		credentials: 'include'
	});
	if (!res.ok) throw new Error('Failed to approve photo');
}

export async function rejectPhoto(id: ApiId): Promise<void> {
	const res = await fetch(`${ADMIN_CUSTOMERS_API}/${id}/reject-photo`, {
		method: 'POST',
		credentials: 'include'
	});
	if (!res.ok) throw new Error('Failed to reject photo');
}
