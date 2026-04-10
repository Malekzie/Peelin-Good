/**
 * Canadian postal code display/input: A1A 1A1 — max 6 alphanumeric + space (7 chars total).
 * Strips non-alphanumeric input; uppercases letters.
 */
export function formatCanadianPostalInput(raw: string): string {
	const alnum = raw.toUpperCase().replace(/[^A-Z0-9]/g, '');
	const six = alnum.slice(0, 6);
	if (six.length <= 3) return six;
	return `${six.slice(0, 3)} ${six.slice(3)}`;
}
