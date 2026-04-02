import { get } from 'svelte/store';
import { token } from '$lib/stores/authStore.js';

let cache = null;

const API_BASE = 'http://localhost:8080/api/v1/products';

function authHeaders() {
	return {
		'Content-Type': 'application/json',
		Authorization: `Bearer ${get(token)}`
	};
}

export async function getProducts() {
	// return cached products if available
	if (cache) return cache;

	// fetch products from backend
	const res = await fetch(API_BASE, {
		headers: authHeaders()
	});

	if (!res.ok) throw new Error('Failed to fetch products: ' + res.status);

	cache = await res.json();
	return cache;
}
