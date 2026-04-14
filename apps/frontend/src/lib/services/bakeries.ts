import { apiFetch } from '$lib/utils/api';
import { BAKERIES_API } from '$lib/services/constants';
import type { ApiId, ProductRecord, ReviewRecord, ErrorWithStatus } from '$lib/services/types';

export async function getBakeries(): Promise<unknown[]> {
	const res = await fetch(BAKERIES_API);
	if (!res.ok) throw new Error(`Failed to fetch bakeries: ${res.status}`);
	return res.json();
}

export async function getBakeryReviews(bakeryId: ApiId): Promise<ReviewRecord[]> {
	const res = await fetch(`${BAKERIES_API}/${bakeryId}/reviews`);
	if (!res.ok) throw new Error(`Failed to fetch bakery reviews: ${res.status}`);
	return res.json();
}

export async function getBakeryAverage(bakeryId: ApiId): Promise<unknown | null> {
	const res = await fetch(`${BAKERIES_API}/${bakeryId}/reviews/average`);
	if (!res.ok) return null;
	return res.json();
}

export async function createBakeryReview(
	bakeryId: ApiId,
	rating: number,
	comment: string
): Promise<ReviewRecord | undefined> {
	const res = await apiFetch(`${BAKERIES_API}/${bakeryId}/reviews`, {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify({ rating, comment })
	});
	if (!res) return;
	if (!res.ok) {
		const err = (await res.json().catch(() => ({}))) as { message?: string };
		const error = new Error(
			err.message ?? `Failed to submit review: ${res.status}`
		) as ErrorWithStatus;
		error.status = res.status;
		throw error;
	}
	return res.json();
}
