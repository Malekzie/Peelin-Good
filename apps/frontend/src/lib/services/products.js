let cache = null;

const API_BASE = '/api/v1/products';

export async function getProducts() {
	// return cached products if available
	if (cache) return cache;

	// fetch products from backend
	const res = await fetch(API_BASE, {
		credentials: 'include'
	});

	if (!res.ok) throw new Error('Failed to fetch products: ' + res.status);

	cache = await res.json();
	return cache;
}
