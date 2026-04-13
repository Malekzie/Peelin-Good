import type { PageServerLoad } from './$types';
import { ADMIN_ANALYTICS_API } from '$lib/services/constants';

function dateStr(date: Date | string): string {
	return date instanceof Date ? date.toISOString().split('T')[0] : date;
}

function buildUrl(path: string, start: Date | string, end: Date | string, bakery?: string): string {
	const params = new URLSearchParams({ start: dateStr(start), end: dateStr(end) });
	if (bakery) params.set('bakerySelection', bakery);
	return `${ADMIN_ANALYTICS_API}${path}?${params.toString()}`;
}

export const load: PageServerLoad = async ({ fetch }) => {
	const today = new Date().toISOString().split('T')[0];
	const thirtyDaysAgo = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0];

	const get = async <T>(url: string): Promise<T> => {
		const res = await fetch(url, { credentials: 'include' });
		if (!res.ok) throw new Error(`Analytics request failed: ${res.status}`);
		return res.json() as Promise<T>;
	};

	// Return bakery names immediately, and stream the metrics
	return {
		bakeryNames: get<string[]>(`${ADMIN_ANALYTICS_API}/meta/bakery-names`),
		initialData: {
			startDate: thirtyDaysAgo,
			endDate: today
		},
		// Stream these in parallel — page renders with skeletons while these load
		totalRevenue: get<number>(buildUrl('/metrics/total-revenue', thirtyDaysAgo, today)),
		aov: get<number>(buildUrl('/metrics/average-order-value', thirtyDaysAgo, today)),
		completionRate: get<number>(buildUrl('/metrics/completion-rate', thirtyDaysAgo, today)),
		revenueOverTime: get<{ label: string; value: number }[]>(
			buildUrl('/revenue-over-time', thirtyDaysAgo, today)
		),
		revenueByBakery: get<{ label: string; value: number }[]>(
			buildUrl('/revenue-by-bakery', thirtyDaysAgo, today)
		),
		topProducts: get<{ label: string; value: number }[]>(
			buildUrl('/series/top-products', thirtyDaysAgo, today)
		),
		salesByEmployee: get<{ label: string; value: number }[]>(
			buildUrl('/series/sales-by-employee', thirtyDaysAgo, today)
		)
	};
};
