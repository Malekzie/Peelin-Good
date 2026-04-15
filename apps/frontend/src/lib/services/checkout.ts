import { FormValidationUtil } from '$lib/utils/formValidation';

// ── Types ────────────────────────────────────────────────────────────────

export interface BakeryAddress {
	line1: string;
	line2?: string;
	city: string;
	province: string;
	postalCode: string;
}

export interface Bakery {
	id: number;
	name: string;
	latitude: number | null;
	longitude: number | null;
	address: BakeryAddress | null;
}

export interface BakeryHour {
	dayOfWeek: number; // 1=Mon … 7=Sun
	openTime: string; // "HH:mm:ss"
	closeTime: string;
	closed: boolean;
}

export interface SavedAddress {
	id: number;
	line1: string;
	line2?: string;
	city: string;
	province: string;
	postalCode: string;
}

export interface CustomerProfile {
	id: string;
	firstName: string | null;
	lastName: string | null;
	email: string | null;
	phone: string | null;
	addressId: number | null;
	address: SavedAddress | null;
	employeeDiscountEligible?: boolean;
	rewardTierDiscountPercent?: number | null;
}

export type ErrorKey =
	| 'guestFirstName'
	| 'guestLastName'
	| 'guestEmail'
	| 'guestPhone'
	| 'deliveryLine1'
	| 'deliveryLine2'
	| 'deliveryCity'
	| 'deliveryProvince'
	| 'deliveryPostal';

// ── Validation ────────────────────────────────────────────────────────────

export function validateField(
	name: ErrorKey,
	values: {
		guestFirstName?: string;
		guestLastName?: string;
		guestEmail?: string;
		guestPhone?: string;
		deliveryLine1?: string;
		deliveryLine2?: string;
		deliveryCity?: string;
		deliveryProvince?: string;
		deliveryPostal?: string;
	}
): string {
	const val = ((): string => {
		switch (name) {
			case 'guestFirstName':
				return values.guestFirstName ?? '';
			case 'guestLastName':
				return values.guestLastName ?? '';
			case 'guestEmail':
				return values.guestEmail ?? '';
			case 'guestPhone':
				return values.guestPhone ?? '';
			case 'deliveryLine1':
				return values.deliveryLine1 ?? '';
			case 'deliveryLine2':
				return values.deliveryLine2 ?? '';
			case 'deliveryCity':
				return values.deliveryCity ?? '';
			case 'deliveryProvince':
				return values.deliveryProvince ?? '';
			case 'deliveryPostal':
				return values.deliveryPostal ?? '';
		}
	})();

	switch (name) {
		case 'guestFirstName':
		case 'guestLastName':
			if (!val.trim()) return 'This field is required.';
			if (val.trim().length < 2) return 'Must be at least 2 characters.';
			return '';
		case 'guestEmail':
			if (!val.trim()) return 'Email is required.';
			if (!FormValidationUtil.isValidEmail(val)) return 'Enter a valid email address.';
			return '';
		case 'guestPhone':
			if (val.trim() && !FormValidationUtil.isValidPhone(val)) return 'Enter a valid phone number.';
			return '';
		case 'deliveryLine1':
			if (!val.trim()) return 'Address is required.';
			return '';
		case 'deliveryLine2':
			return '';
		case 'deliveryCity':
			if (!val.trim()) return 'City is required.';
			return '';
		case 'deliveryProvince':
			if (!val) return 'Please select a province.';
			return '';
		case 'deliveryPostal':
			if (!val.trim()) return 'Postal code is required.';
			if (!FormValidationUtil.isValidCanadianPostalCode(val))
				return 'Enter a valid Canadian postal code (e.g. T2P 1J9).';
			return '';
	}
}

export function formatPhone(raw: string): string {
	return FormValidationUtil.formatPhone(raw);
}

// ── Geolocation & Distance ────────────────────────────────────────────────

export function haversineKm(lat1: number, lon1: number, lat2: number, lon2: number): number {
	const R = 6371;
	const dLat = ((lat2 - lat1) * Math.PI) / 180;
	const dLon = ((lon2 - lon1) * Math.PI) / 180;
	const a =
		Math.sin(dLat / 2) ** 2 +
		Math.cos((lat1 * Math.PI) / 180) * Math.cos((lat2 * Math.PI) / 180) * Math.sin(dLon / 2) ** 2;
	return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

export function distanceLabel(
	bakery: Bakery,
	userLat: number | null,
	userLng: number | null
): string {
	if (userLat == null || userLng == null || !bakery.latitude || !bakery.longitude) return '';
	const km = haversineKm(userLat, userLng, bakery.latitude, bakery.longitude);
	return km < 1 ? `${Math.round(km * 1000)} m away` : `${km.toFixed(1)} km away`;
}

export function formatBakeryOption(
	bakery: Bakery,
	userLat: number | null,
	userLng: number | null
): string {
	const city = bakery.address?.city ?? '';
	const dist = distanceLabel(bakery, userLat, userLng);
	let label = bakery.name;
	if (city) label += `, ${city}`;
	if (dist) label += ` (${dist})`;
	return label;
}

export function sortedBakeries(
	bakeries: Bakery[],
	userLat: number | null,
	userLng: number | null
): Bakery[] {
	if (userLat == null || userLng == null) return bakeries;
	return [...bakeries].sort((a, b) => {
		const da =
			a.latitude && a.longitude ? haversineKm(userLat, userLng, a.latitude, a.longitude) : Infinity;
		const db =
			b.latitude && b.longitude ? haversineKm(userLat, userLng, b.latitude, b.longitude) : Infinity;
		return da - db;
	});
}

// ── Schedule Helpers ──────────────────────────────────────────────────────

export function formatTimeHM(h: number, m: number): string {
	const period = h >= 12 ? 'PM' : 'AM';
	const display = h % 12 === 0 ? 12 : h % 12;
	return `${display}:${String(m).padStart(2, '0')} ${period}`;
}

export function asapScheduleDate(): string {
	const now = new Date();
	let total = now.getHours() * 60 + now.getMinutes() + 120;
	const rem = total % 30;
	if (rem !== 0) total += 30 - rem;
	const d = new Date(now);
	d.setHours(0, 0, 0, 0);
	d.setMinutes(total);
	return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

export function getAsapEstimateLabel(): string {
	const now = new Date();
	let total = now.getHours() * 60 + now.getMinutes() + 120;
	const rem = total % 30;
	if (rem !== 0) total += 30 - rem;
	const overflows = total >= 24 * 60;
	const h = Math.floor((total % (24 * 60)) / 60);
	const m = total % 60;
	return overflows ? `tomorrow at ${formatTimeHM(h, m)}` : formatTimeHM(h, m);
}

export function minScheduleDate(): string {
	return new Date().toISOString().split('T')[0];
}

export function maxScheduleDate(): string {
	const d = new Date();
	d.setDate(d.getDate() + 30);
	return d.toISOString().split('T')[0];
}

export function isOpenNow(bakeryHours: BakeryHour[]): boolean {
	if (!bakeryHours.length) return false;
	const now = new Date();
	const dtoDay = now.getDay() === 0 ? 7 : now.getDay();
	const entry = bakeryHours.find((h) => h.dayOfWeek === dtoDay);
	if (!entry || entry.closed) return false;
	const [oh, om] = entry.openTime.split(':').map(Number);
	const [ch, cm] = entry.closeTime.split(':').map(Number);
	const nowMin = now.getHours() * 60 + now.getMinutes();
	return nowMin >= oh * 60 + om && nowMin < ch * 60 + cm;
}

export function nextOpenStr(bakeryHours: BakeryHour[]): string | null {
	if (!bakeryHours.length) return null;
	const now = new Date();
	const nowMin = now.getHours() * 60 + now.getMinutes();
	for (let offset = 0; offset <= 7; offset++) {
		const d = new Date(now);
		d.setDate(d.getDate() + offset);
		const dtoDay = d.getDay() === 0 ? 7 : d.getDay();
		const entry = bakeryHours.find((h) => h.dayOfWeek === dtoDay);
		if (!entry || entry.closed) continue;
		const [oh, om] = entry.openTime.split(':').map(Number);
		if (offset === 0 && oh * 60 + om <= nowMin) continue;
		const timeStr = formatTimeHM(oh, om);
		if (offset === 0) return `today at ${timeStr}`;
		if (offset === 1) return `tomorrow at ${timeStr}`;
		return `${d.toLocaleDateString('en-CA', { weekday: 'long' })} at ${timeStr}`;
	}
	return null;
}

export function scheduledDayClosedNotice(
	scheduleDate: string,
	bakeryHours: BakeryHour[],
	availableTimeSlots: string[]
): string | null {
	if (!scheduleDate || availableTimeSlots.length > 0) return null;
	if (!bakeryHours.length) return null;
	for (let offset = 1; offset <= 7; offset++) {
		const d = new Date(scheduleDate + 'T12:00:00');
		d.setDate(d.getDate() + offset);
		const dtoDay = d.getDay() === 0 ? 7 : d.getDay();
		const entry = bakeryHours.find((h) => h.dayOfWeek === dtoDay);
		if (!entry || entry.closed) continue;
		const [oh, om] = entry.openTime.split(':').map(Number);
		const timeStr = formatTimeHM(oh, om);
		const dayLabel = offset === 1 ? 'tomorrow' : d.toLocaleDateString('en-CA', { weekday: 'long' });
		return `This location is closed on the selected day. It will open ${dayLabel} at ${timeStr}.`;
	}
	return 'This location appears to be closed for the near future.';
}

export function generateTimeSlots(scheduleDate: string, bakeryHours: BakeryHour[]): string[] {
	if (!scheduleDate || bakeryHours.length === 0) {
		return [];
	}

	const selected = new Date(scheduleDate + 'T00:00:00');
	const today = new Date();
	today.setHours(0, 0, 0, 0);
	const isToday = selected.getTime() === today.getTime();

	let minTotalMinutes = 0;
	if (isToday) {
		const now = new Date();
		let earliest = now.getHours() * 60 + now.getMinutes() + 120;
		const rem = earliest % 30;
		if (rem !== 0) earliest += 30 - rem;
		minTotalMinutes = earliest;
	}

	const jsDay = selected.getDay();
	const dtoDay = jsDay === 0 ? 7 : jsDay;
	const hourEntry = bakeryHours.find((h) => h.dayOfWeek === dtoDay);
	if (!hourEntry || hourEntry.closed) {
		return [];
	}

	const [openH, openM] = hourEntry.openTime.split(':').map(Number);
	const [closeH, closeM] = hourEntry.closeTime.split(':').map(Number);

	let startMinutes = openH * 60 + openM;
	const rem = startMinutes % 30;
	if (rem !== 0) startMinutes += 30 - rem;

	const closeMinutes = closeH * 60 + closeM;
	const slots: string[] = [];

	for (let t = startMinutes; t < closeMinutes; t += 30) {
		if (isToday && t < minTotalMinutes) continue;
		const h = Math.floor(t / 60);
		const m = t % 60;
		slots.push(`${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`);
	}

	return slots;
}
