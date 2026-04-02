import { get } from 'svelte/store';
import { token } from '$lib/stores/authStore.js';

const API_BASE = 'http://localhost:8080/api/v1/customers/me';

// TODO change this to use HttpCookies from backend instead of storing token in localStorage for better security
function authHeaders() {
	const t = get(token);
	const headers = {
		'Content-Type': 'application/json'
	};

	if (t) headers['Authorization'] = `Bearer ${t}`;

	return headers;
}

export async function getProfile() {
	const res = await fetch(API_BASE, {
		headers: authHeaders()
	});

	if (!res.ok) throw new Error('Failed to fetch profile: ' + res.status);
	return await res.json();
}
