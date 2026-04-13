import { ADMIN_ANALYTICS_API } from '$lib/services/constants';
import type { AnalyticsPoint } from '$lib/services/types';

function dateStr(date: Date | string): string {
	return date instanceof Date ? date.toISOString().split('T')[0] : date;
}

function buildUrl(path: string, start: Date | string, end: Date | string, bakery?: string): string {
	const params = new URLSearchParams({ start: dateStr(start), end: dateStr(end) });
	if (bakery) params.set('bakerySelection', bakery);
	return `${ADMIN_ANALYTICS_API}${path}?${params}`;
}

async function get<T>(url: string): Promise<T> {
	const res = await fetch(url, { credentials: 'include' });
	if (!res.ok) throw new Error('Analytics request failed');
	return res.json() as Promise<T>;
}

export async function getBakeryNames(): Promise<string[]> {
	return get<string[]>(`${ADMIN_ANALYTICS_API}/meta/bakery-names`);
}

export async function getTotalRevenue(
	start: Date | string,
	end: Date | string,
	bakery?: string
): Promise<number> {
	return get<number>(buildUrl('/metrics/total-revenue', start, end, bakery));
}

export async function getAverageOrderValue(
	start: Date | string,
	end: Date | string,
	bakery?: string
): Promise<number> {
	return get<number>(buildUrl('/metrics/average-order-value', start, end, bakery));
}

export async function getCompletionRate(
	start: Date | string,
	end: Date | string,
	bakery?: string
): Promise<number> {
	return get<number>(buildUrl('/metrics/completion-rate', start, end, bakery));
}

export async function getRevenueOverTime(
	start: Date | string,
	end: Date | string,
	bakery?: string
): Promise<AnalyticsPoint[]> {
	return get<AnalyticsPoint[]>(buildUrl('/revenue-over-time', start, end, bakery));
}

export async function getRevenueByBakery(
	start: Date | string,
	end: Date | string
): Promise<AnalyticsPoint[]> {
	return get<AnalyticsPoint[]>(buildUrl('/revenue-by-bakery', start, end, undefined));
}

export async function getTopProducts(
	start: Date | string,
	end: Date | string,
	bakery?: string
): Promise<AnalyticsPoint[]> {
	return get<AnalyticsPoint[]>(buildUrl('/series/top-products', start, end, bakery));
}

export async function getSalesByEmployee(
	start: Date | string,
	end: Date | string,
	bakery?: string
): Promise<AnalyticsPoint[]> {
	return get<AnalyticsPoint[]>(buildUrl('/series/sales-by-employee', start, end, bakery));
}
