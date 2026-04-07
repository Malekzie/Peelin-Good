const API = '/api/v1/admin/customers';

export async function listCustomers(search = '') {
	const url = search ? `${API}?search=${encodeURIComponent(search)}` : API;
	const res = await fetch(url, { credentials: 'include' });
	if (!res.ok) throw new Error('Failed to fetch customers');
	return res.json();
}

export async function getCustomer(id) {
	const res = await fetch(`${API}/${id}`, { credentials: 'include' });
	if (!res.ok) throw new Error('Customer not found');
	return res.json();
}

export async function patchCustomer(id, data) {
	const res = await fetch(`${API}/${id}`, {
		method: 'PATCH',
		headers: { 'Content-Type': 'application/json' },
		credentials: 'include',
		body: JSON.stringify(data)
	});
	if (!res.ok) throw new Error('Failed to update customer');
	return res.json();
}

export async function getPendingPhotos() {
	const res = await fetch(`${API}/pending-photos`, { credentials: 'include' });
	if (!res.ok) throw new Error('Failed to fetch pending photos');
	return res.json();
}

export async function approvePhoto(id) {
	const res = await fetch(`${API}/${id}/approve-photo`, {
		method: 'POST',
		credentials: 'include'
	});
	if (!res.ok) throw new Error('Failed to approve photo');
}

export async function rejectPhoto(id) {
	const res = await fetch(`${API}/${id}/reject-photo`, {
		method: 'POST',
		credentials: 'include'
	});
	if (!res.ok) throw new Error('Failed to reject photo');
}
