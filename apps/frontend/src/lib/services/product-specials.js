const API_BASE = '/api/v1/product-specials';

export async function getAllSpecials() {
	const res = await fetch(`${API_BASE}/all`);
	if (!res.ok) throw new Error(`Failed to fetch specials: ${res.status}`);
	return res.json();
}

export async function getTodaySpecial(date) {
	const url = date ? `${API_BASE}/today?date=${date}` : `${API_BASE}/today`;
	const res = await fetch(url);
	if (!res.ok) throw new Error(`Failed to fetch today's special: ${res.status}`);
	return res.json();
}
