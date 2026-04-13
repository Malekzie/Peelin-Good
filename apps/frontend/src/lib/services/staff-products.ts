import { PRODUCTS_API } from '$lib/services/constants';
import type { ApiId, ProductRecord } from '$lib/services/types';

export async function listProducts(): Promise<ProductRecord[]> {
	const res = await fetch(PRODUCTS_API, { credentials: 'include' });
	if (!res.ok) throw new Error('Failed to fetch products');
	return res.json();
}

export async function createProduct(data: Record<string, unknown>): Promise<ProductRecord> {
	const res = await fetch(PRODUCTS_API, {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		credentials: 'include',
		body: JSON.stringify(data)
	});
	if (!res.ok) throw new Error('Failed to create product');
	return res.json();
}

export async function updateProduct(
	id: ApiId,
	data: Record<string, unknown>
): Promise<ProductRecord> {
	const res = await fetch(`${PRODUCTS_API}/${id}`, {
		method: 'PUT',
		headers: { 'Content-Type': 'application/json' },
		credentials: 'include',
		body: JSON.stringify(data)
	});
	if (!res.ok) throw new Error('Failed to update product');
	return res.json();
}

export async function deleteProduct(id: ApiId): Promise<void> {
	const res = await fetch(`${PRODUCTS_API}/${id}`, {
		method: 'DELETE',
		credentials: 'include'
	});
	if (!res.ok) throw new Error('Failed to delete product');
}

export async function uploadProductImage(id: ApiId, file: File): Promise<ProductRecord> {
	const formData = new FormData();
	formData.append('image', file);
	const res = await fetch(`${PRODUCTS_API}/${id}/image`, {
		method: 'POST',
		credentials: 'include',
		body: formData
	});
	if (!res.ok) throw new Error('Failed to upload image');
	return res.json();
}
