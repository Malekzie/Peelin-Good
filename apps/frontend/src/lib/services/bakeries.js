import { apiFetch } from '$lib/utils/api';

const API = '/api/v1';

export async function getBakeries() {
	const res = await fetch(`${API}/bakeries`);

	if (!res.ok) throw new Error('Failed to fetch bakeries: ' + res.status);
	return res.json();
}

export async function getBakeryReviews(bakeryId) {
	const res = await fetch(`${API}/bakeries/${bakeryId}/reviews`);
	if (!res.ok) throw new Error('Failed to fetch bakery reviews: ' + res.status);
	return res.json();
}

export async function getBakeryAverage(bakeryId) {
	const res = await fetch(`${API}/bakeries/${bakeryId}/reviews/average`);
	if (!res.ok) return null;
	return res.json();
}

export async function createBakeryReview(bakeryId, rating, comment, guestName = null) {
	const res = await apiFetch(`${API}/bakeries/${bakeryId}/reviews`, {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify({ rating, comment, ...(guestName ? { guestName } : {}) })
	});
	if (!res) return;
	if (!res.ok) {
		const err = await res.json().catch(() => ({}));
		const error = new Error(err.message ?? `Failed to submit review: ${res.status}`);
		error.status = res.status;
		throw error;
	}
	return res.json();
}
