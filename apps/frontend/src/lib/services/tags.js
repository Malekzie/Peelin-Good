import { get } from 'svelte/store';

let cache = null;

const API_BASE = '/api/v1/tags';

export async function getTags() {
	if (cache) return cache;

	const res = await fetch(API_BASE, {
		credentials: 'include'
	});
	if (!res.ok) throw new Error('Failed to fetch tags: ' + res.status);

	cache = await res.json();
	return cache;
}
