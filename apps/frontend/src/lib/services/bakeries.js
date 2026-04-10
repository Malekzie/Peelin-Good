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
