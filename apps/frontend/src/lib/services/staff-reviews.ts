import { REVIEWS_API } from '$lib/services/constants';
import type { ApiId, ReviewRecord } from '$lib/services/types';

export async function getPendingReviews(): Promise<ReviewRecord[]> {
	const res = await fetch(`${REVIEWS_API}/pending`, { credentials: 'include' });
	if (res.status === 403) {
		const err = new Error('Forbidden') as Error & { status?: number };
		err.status = 403;
		throw err;
	}
	if (!res.ok) throw new Error('Failed to fetch pending reviews');
	return res.json();
}

export async function updateReviewStatus(
	reviewId: ApiId,
	status: string
): Promise<ReviewRecord | null> {
	const res = await fetch(`${REVIEWS_API}/${reviewId}/status`, {
		method: 'PATCH',
		headers: { 'Content-Type': 'application/json' },
		credentials: 'include',
		body: JSON.stringify({ status })
	});
	if (res.status === 403) {
		const err = new Error('Forbidden') as Error & { status?: number };
		err.status = 403;
		throw err;
	}
	if (!res.ok) throw new Error('Failed to update review status');
	if (res.status === 204) return null;
	return res.json();
}
