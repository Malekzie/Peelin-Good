import { apiFetch } from '$lib/utils/api';

const API = '/api/v1';

export async function getTopReviews(limit = 3) {
	const res = await fetch(`${API}/reviews/top?limit=${limit}`);
	if (!res.ok) throw new Error('Failed to fetch reviews: ' + res.status);
	return res.json();
}

export async function createProductReview(productId, rating, comment) {
	const res = await apiFetch(`${API}/products/${productId}/reviews`, {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify({ rating, comment })
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

export async function createOrderReview(orderId, rating, comment) {
	const res = await apiFetch(`${API}/orders/${orderId}/reviews`, {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify({ rating, comment, orderId })
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

export async function getProductReviews(productId) {
	const res = await fetch(`${API}/products/${productId}/reviews`);

	if (!res.ok) throw new Error('Failed to fetch reviews: ' + res.status);
	return res.json();
}
