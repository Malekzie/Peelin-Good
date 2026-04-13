import { PRODUCT_SPECIALS_API } from '$lib/services/constants';
import type { ProductRecord } from '$lib/services/types';

export async function getAllSpecials(): Promise<ProductRecord[]> {
	const res = await fetch(`${PRODUCT_SPECIALS_API}/all`);
	if (!res.ok) throw new Error(`Failed to fetch specials: ${res.status}`);
	return res.json();
}

export async function getTodaySpecial(date?: string): Promise<ProductRecord> {
	const url = date ? `${PRODUCT_SPECIALS_API}/today?date=${date}` : `${PRODUCT_SPECIALS_API}/today`;
	const res = await fetch(url);
	if (!res.ok) throw new Error(`Failed to fetch today's special: ${res.status}`);
	return res.json();
}
