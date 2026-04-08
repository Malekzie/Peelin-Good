const API = '/api/v1/products';

export async function listProducts() {
	const res = await fetch(API, { credentials: 'include' });
	if (!res.ok) throw new Error('Failed to fetch products');
	return res.json();
}

export async function createProduct(data) {
	const res = await fetch(API, {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		credentials: 'include',
		body: JSON.stringify(data)
	});
	if (!res.ok) throw new Error('Failed to create product');
	return res.json();
}

export async function updateProduct(id, data) {
	const res = await fetch(`${API}/${id}`, {
		method: 'PUT',
		headers: { 'Content-Type': 'application/json' },
		credentials: 'include',
		body: JSON.stringify(data)
	});
	if (!res.ok) throw new Error('Failed to update product');
	return res.json();
}

export async function deleteProduct(id) {
	const res = await fetch(`${API}/${id}`, {
		method: 'DELETE',
		credentials: 'include'
	});
	if (!res.ok) throw new Error('Failed to delete product');
}
