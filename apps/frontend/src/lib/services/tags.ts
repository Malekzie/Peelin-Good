import { TAGS_API } from '$lib/services/constants';
import type { TagRecord } from '$lib/services/types';

let cache: TagRecord[] | null = null;

export async function getTags(): Promise<TagRecord[]> {
	if (cache) return cache;

	const res = await fetch(TAGS_API, {
		credentials: 'include'
	});
	if (!res.ok) throw new Error(`Failed to fetch tags: ${res.status}`);

	cache = await res.json();
	return cache ?? [];
}
