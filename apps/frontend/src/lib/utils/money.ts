/** Display a Canadian dollar amount with the literal CAD suffix (e.g. `$12.34 CAD`). */
export function formatPriceCad(amount: number | string | null | undefined): string {
	const n = Number(amount);
	if (!Number.isFinite(n)) return '$0.00 CAD';
	return `$${n.toFixed(2)} CAD`;
}

/** Discount line: minus sign, amount, CAD (e.g. `−$5.00 CAD`). */
export function formatDiscountCad(amount: number | string | null | undefined): string {
	const n = Math.abs(Number(amount));
	if (!Number.isFinite(n)) return '−$0.00 CAD';
	return `−$${n.toFixed(2)} CAD`;
}
