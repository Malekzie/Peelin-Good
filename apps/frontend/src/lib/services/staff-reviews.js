const API = '/api/v1';

export async function getPendingReviews() {
	const res = await fetch(`${API}/reviews/pending`, { credentials: 'include' });
	if (!res.ok) throw new Error('Failed to fetch pending reviews');
	return res.json();
}

export async function updateReviewStatus(reviewId, status) {
	const res = await fetch(`${API}/reviews/${reviewId}/status`, {
		method: 'PATCH',
		headers: { 'Content-Type': 'application/json' },
		credentials: 'include',
		body: JSON.stringify({ status })
	});
	if (!res.ok) throw new Error('Failed to update review status');
	return res.json();
}
