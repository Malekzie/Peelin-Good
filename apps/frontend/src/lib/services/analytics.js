const API = '/api/v1/admin/analytics';

function dateStr(d) {
	return d instanceof Date ? d.toISOString().split('T')[0] : d;
}

function buildUrl(path, start, end, bakery) {
	const params = new URLSearchParams({ start: dateStr(start), end: dateStr(end) });
	if (bakery) params.set('bakerySelection', bakery);
	return `${API}${path}?${params}`;
}

async function get(url) {
	const res = await fetch(url, { credentials: 'include' });
	if (!res.ok) throw new Error('Analytics request failed');
	return res.json();
}

export async function getBakeryNames() {
	return get(`${API}/meta/bakery-names`);
}

export async function getTotalRevenue(start, end, bakery) {
	return get(buildUrl('/metrics/total-revenue', start, end, bakery));
}

export async function getAverageOrderValue(start, end, bakery) {
	return get(buildUrl('/metrics/average-order-value', start, end, bakery));
}

export async function getCompletionRate(start, end, bakery) {
	return get(buildUrl('/metrics/completion-rate', start, end, bakery));
}

export async function getRevenueOverTime(start, end, bakery) {
	return get(buildUrl('/revenue-over-time', start, end, bakery));
}

export async function getRevenueByBakery(start, end) {
	return get(buildUrl('/revenue-by-bakery', start, end, null));
}

export async function getTopProducts(start, end, bakery) {
	return get(buildUrl('/series/top-products', start, end, bakery));
}

export async function getSalesByEmployee(start, end, bakery) {
	return get(buildUrl('/series/sales-by-employee', start, end, bakery));
}
