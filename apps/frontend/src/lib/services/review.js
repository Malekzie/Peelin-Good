const API = '/api/v1';

export async function getTopReviews(limit = 3) {
	const res = await fetch(`${API}/reviews/top?limit=${limit}`);
	if (!res.ok) throw new Error('Failed to fetch reviews: ' + res.status);
	return res.json();
}
