import { get } from 'svelte/store';
import { token } from '$lib/stores/authStore.js';

let cache = null;

const API_BASE = 'http://localhost:8080/api/v1/tags';

function authHeaders() {
	return {
		'Content-Type': 'application/json',
		Authorization: `Bearer ${get(token)}`
	};
}

export async function getTags() {
	if (cache) return cache;

	const res = await fetch(API_BASE, {
		headers: authHeaders()
	});
	if (!res.ok) throw new Error('Failed to fetch tags: ' + res.status);

	cache = await res.json();
	return cache;
}
