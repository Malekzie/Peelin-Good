<script lang="ts">
	import { resolve } from '$app/paths';
	import { goto } from '$app/navigation';
	import { cart } from '$lib/stores/cart';
	import { onMount } from 'svelte';
	import { get } from 'svelte/store';
	import { isLoggedIn } from '$lib/stores/authStore';
	import { api } from '$lib/api';
	import { formatCanadianPostalInput } from '$lib/canadianPostalCode';

	// ── Types ────────────────────────────────────────────────────────────────────

	interface BakeryAddress {
		line1: string;
		line2?: string;
		city: string;
		province: string;
		postalCode: string;
	}

	interface Bakery {
		id: number;
		name: string;
		latitude: number | null;
		longitude: number | null;
		address: BakeryAddress | null;
	}

	interface BakeryHour {
		dayOfWeek: number; // 1=Mon … 7=Sun
		openTime: string; // "HH:mm:ss"
		closeTime: string;
		closed: boolean;
	}

	interface SavedAddress {
		id: number;
		line1: string;
		line2?: string;
		city: string;
		province: string;
		postalCode: string;
	}

	interface CustomerProfile {
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

	// ── Session ──────────────────────────────────────────────────────────────────

	const isGuest = !get(isLoggedIn);

	// ── Page state ───────────────────────────────────────────────────────────────

	let pageLoading = $state(true);
	let pageError = $state('');

	// Guest contact
	let guestFirstName = $state('');
	let guestLastName = $state('');
	let guestEmail = $state('');
	let guestPhone = $state('');

	// Delivery address (for guest or logged-in customer without saved address)
	let deliveryLine1 = $state('');
	let deliveryLine2 = $state('');
	let deliveryCity = $state('');
	let deliveryProvince = $state('AB');
	let deliveryPostal = $state('');

	// ── Validation ───────────────────────────────────────────────────────────────

	type ErrorKey =
		| 'guestFirstName' | 'guestLastName' | 'guestEmail' | 'guestPhone'
		| 'deliveryLine1' | 'deliveryCity' | 'deliveryProvince' | 'deliveryPostal';

	let errors = $state<Record<ErrorKey, string>>({
		guestFirstName: '', guestLastName: '', guestEmail: '', guestPhone: '',
		deliveryLine1: '', deliveryCity: '', deliveryProvince: '', deliveryPostal: ''
	});

	let touched = $state<Record<ErrorKey, boolean>>({
		guestFirstName: false, guestLastName: false, guestEmail: false, guestPhone: false,
		deliveryLine1: false, deliveryCity: false, deliveryProvince: false, deliveryPostal: false
	});

	function validateField(name: ErrorKey): string {
		const val = ((): string => {
			switch (name) {
				case 'guestFirstName': return guestFirstName;
				case 'guestLastName':  return guestLastName;
				case 'guestEmail':     return guestEmail;
				case 'guestPhone':     return guestPhone;
				case 'deliveryLine1':  return deliveryLine1;
				case 'deliveryCity':   return deliveryCity;
				case 'deliveryProvince': return deliveryProvince;
				case 'deliveryPostal': return deliveryPostal;
			}
		})();

		switch (name) {
			case 'guestFirstName':
			case 'guestLastName':
				if (val.trim() && val.trim().length < 2) return 'Must be at least 2 characters.';
				return '';
			case 'guestEmail':
				if (!guestEmail.trim()) return 'Email is required.';
				if (!/^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(val.trim()))
					return 'Enter a valid email address.';
				return '';
			case 'guestPhone':
				if (val.trim() && !/^\+?[\d\s\-().]{7,15}$/.test(val.trim()))
					return 'Enter a valid phone number.';
				return '';
			case 'deliveryLine1':
				if (!val.trim()) return 'Address is required.';
				return '';
			case 'deliveryCity':
				if (!val.trim()) return 'City is required.';
				return '';
			case 'deliveryProvince':
				if (!val) return 'Please select a province.';
				return '';
			case 'deliveryPostal':
				if (!val.trim()) return 'Postal code is required.';
				if (!/^[A-Za-z]\d[A-Za-z][ -]?\d[A-Za-z]\d$/.test(val.trim()))
					return 'Enter a valid Canadian postal code (e.g. T2P 1J9).';
				return '';
		}
	}

	function handleBlur(name: ErrorKey) {
		touched[name] = true;
		errors[name] = validateField(name);
	}

	function handleInput(name: ErrorKey) {
		if (touched[name]) errors[name] = validateField(name);
	}

	function formatPhone(raw: string): string {
		const digits = raw.replace(/\D/g, '').substring(0, 10);
		const parts: string[] = [];
		if (digits.length > 0) parts.push('(' + digits.substring(0, 3));
		if (digits.length >= 4) parts.push(') ' + digits.substring(3, 6));
		if (digits.length >= 7) parts.push('-' + digits.substring(6, 10));
		return parts.join('');
	}

	function validateContactFields(): boolean {
		if (!isGuest) return true;
		const fields: ErrorKey[] = ['guestFirstName', 'guestLastName', 'guestEmail', 'guestPhone'];
		fields.forEach((f) => { touched[f] = true; errors[f] = validateField(f); });
		return !fields.some((f) => errors[f]);
	}

	function validateDeliveryFields(): boolean {
		if (!needsDeliveryForm) return true;
		const fields: ErrorKey[] = ['deliveryLine1', 'deliveryCity', 'deliveryProvince', 'deliveryPostal'];
		fields.forEach((f) => { touched[f] = true; errors[f] = validateField(f); });
		return !fields.some((f) => errors[f]);
	}

	// Loaded data
	let customer = $state<CustomerProfile | null>(null);
	let bakeries = $state<Bakery[]>([]);
	let selectedBakeryId = $state<number | null>(null);
	let stripePublishableKey = $state('');

	// User geolocation
	let userLat = $state<number | null>(null);
	let userLng = $state<number | null>(null);

	// Fulfillment
	let orderMethod = $state<'pickup' | 'delivery'>('pickup');

	// Schedule
	let scheduleEnabled = $state(false);
	let scheduleDate = $state('');
	let scheduleTime = $state('');
	let bakeryHours = $state<BakeryHour[]>([]);
	let bakeryHoursLoading = $state(false);
	let availableTimeSlots = $state<string[]>([]);

	// Custom delivery address override (logged-in customers)
	let useCustomAddress = $state(false);

	// Misc
	let orderComment = $state('');

	// Submit
	let submitting = $state(false);
	let submitError = $state('');

	// Payment phase
	let phase = $state<'form' | 'payment'>('form');
	let pendingOrderId = $state('');
	let pendingOrderNumber = $state('');
	let pendingClientSecret = $state('');
	let pendingPaymentIntentId = $state('');
	let pendingSubtotal = $state<number | null>(null);
	let pendingDiscount = $state<number | null>(null);
	let pendingDeliveryFee = $state<number | null>(null);
	let pendingTaxAmount = $state<number | null>(null);
	let pendingGrandTotal = $state<number | null>(null);
	let paymentContainer = $state<HTMLDivElement | undefined>(undefined);
	let stripePaymentLoading = $state(false);
	let paymentError = $state('');

	// ── Derived ──────────────────────────────────────────────────────────────────

	const DELIVERY_FEE = 7;
	const DELIVERY_FREE_THRESHOLD = 50;

	const selectedBakery = $derived(bakeries.find((b) => b.id === selectedBakeryId) ?? null);

	const deliveryFee = $derived(
		orderMethod === 'delivery' && $cart.subtotal < DELIVERY_FREE_THRESHOLD ? DELIVERY_FEE : 0
	);

	const needsDeliveryForm = $derived(
		orderMethod === 'delivery' && (isGuest || !customer?.addressId || useCustomAddress)
	);

	const hasScheduledAt = $derived(
		scheduleEnabled && !!scheduleDate && !!scheduleTime
	);

	const scheduledAtIso = $derived(
		hasScheduledAt ? `${scheduleDate}T${scheduleTime}:00Z` : null
	);

	// ── Helpers ──────────────────────────────────────────────────────────────────

	function haversineKm(lat1: number, lon1: number, lat2: number, lon2: number): number {
		const R = 6371;
		const dLat = ((lat2 - lat1) * Math.PI) / 180;
		const dLon = ((lon2 - lon1) * Math.PI) / 180;
		const a =
			Math.sin(dLat / 2) ** 2 +
			Math.cos((lat1 * Math.PI) / 180) *
				Math.cos((lat2 * Math.PI) / 180) *
				Math.sin(dLon / 2) ** 2;
		return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
	}

	function distanceLabel(b: Bakery): string {
		if (userLat == null || userLng == null || !b.latitude || !b.longitude) return '';
		const km = haversineKm(userLat, userLng, b.latitude, b.longitude);
		return km < 1 ? `${Math.round(km * 1000)} m away` : `${km.toFixed(1)} km away`;
	}

	function formatBakeryOption(b: Bakery): string {
		const city = b.address?.city ?? '';
		const dist = distanceLabel(b);
		let label = b.name;
		if (city) label += `, ${city}`;
		if (dist) label += ` (${dist})`;
		return label;
	}

	function sortedBakeries(): Bakery[] {
		if (userLat == null || userLng == null) return bakeries;
		return [...bakeries].sort((a, b) => {
			const da =
				a.latitude && a.longitude
					? haversineKm(userLat!, userLng!, a.latitude, a.longitude)
					: Infinity;
			const db =
				b.latitude && b.longitude
					? haversineKm(userLat!, userLng!, b.latitude, b.longitude)
					: Infinity;
			return da - db;
		});
	}

	function minScheduleDate(): string {
		return new Date().toISOString().split('T')[0];
	}

	function maxScheduleDate(): string {
		const d = new Date();
		d.setDate(d.getDate() + 30);
		return d.toISOString().split('T')[0];
	}

	// ── Schedule helpers ─────────────────────────────────────────────────────────

	function formatTimeHM(h: number, m: number): string {
		const period = h >= 12 ? 'PM' : 'AM';
		const display = h % 12 === 0 ? 12 : h % 12;
		return `${display}:${String(m).padStart(2, '0')} ${period}`;
	}

	// Returns today's date (or tomorrow's if now+2h crosses midnight) as YYYY-MM-DD
	function asapScheduleDate(): string {
		const now = new Date();
		let total = now.getHours() * 60 + now.getMinutes() + 120;
		const rem = total % 30;
		if (rem !== 0) total += 30 - rem;
		const d = new Date(now);
		d.setHours(0, 0, 0, 0);
		d.setMinutes(total); // JS setMinutes handles midnight overflow automatically
		return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
	}

	// ASAP estimated ready time: now + 2h rounded up to next 30-min boundary
	function getAsapEstimateLabel(): string {
		const now = new Date();
		let total = now.getHours() * 60 + now.getMinutes() + 120;
		const rem = total % 30;
		if (rem !== 0) total += 30 - rem;
		const overflows = total >= 24 * 60;
		const h = Math.floor((total % (24 * 60)) / 60);
		const m = total % 60;
		return overflows ? `tomorrow at ${formatTimeHM(h, m)}` : formatTimeHM(h, m);
	}

	// Is the selected bakery open right now?
	const isOpenNow = $derived.by((): boolean => {
		if (!bakeryHours.length) return false;
		const now = new Date();
		const dtoDay = now.getDay() === 0 ? 7 : now.getDay();
		const entry = bakeryHours.find((h) => h.dayOfWeek === dtoDay);
		if (!entry || entry.closed) return false;
		const [oh, om] = entry.openTime.split(':').map(Number);
		const [ch, cm] = entry.closeTime.split(':').map(Number);
		const nowMin = now.getHours() * 60 + now.getMinutes();
		return nowMin >= oh * 60 + om && nowMin < ch * 60 + cm;
	});

	// Next time the bakery will open — used in closed-location notices
	const nextOpenStr = $derived.by((): string | null => {
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
			// On the current day only use it if the open time hasn't passed yet
			if (offset === 0 && oh * 60 + om <= nowMin) continue;
			const timeStr = formatTimeHM(oh, om);
			if (offset === 0) return `today at ${timeStr}`;
			if (offset === 1) return `tomorrow at ${timeStr}`;
			return `${d.toLocaleDateString('en-CA', { weekday: 'long' })} at ${timeStr}`;
		}
		return null;
	});

	// Notice shown when the selected scheduled date has no available time slots
	const scheduledDayClosedNotice = $derived.by((): string | null => {
		if (!scheduleEnabled || !scheduleDate || availableTimeSlots.length > 0) return null;
		if (!bakeryHours.length) return null;
		for (let offset = 1; offset <= 7; offset++) {
			const d = new Date(scheduleDate + 'T12:00:00'); // noon avoids DST edge cases
			d.setDate(d.getDate() + offset);
			const dtoDay = d.getDay() === 0 ? 7 : d.getDay();
			const entry = bakeryHours.find((h) => h.dayOfWeek === dtoDay);
			if (!entry || entry.closed) continue;
			const [oh, om] = entry.openTime.split(':').map(Number);
			const timeStr = formatTimeHM(oh, om);
			const dayLabel =
				offset === 1
					? 'tomorrow'
					: d.toLocaleDateString('en-CA', { weekday: 'long' });
			return `This location is closed on the selected day. It will open ${dayLabel} at ${timeStr}.`;
		}
		return 'This location appears to be closed for the near future.';
	});

	// ── Lifecycle ────────────────────────────────────────────────────────────────

	onMount(async () => {
		if ($cart.items.length === 0) {
			goto(resolve('/'));
			return;
		}

		// Request geolocation — silently ignore if denied
		if (typeof navigator !== 'undefined' && navigator.geolocation) {
			navigator.geolocation.getCurrentPosition(
				(pos) => {
					userLat = pos.coords.latitude;
					userLng = pos.coords.longitude;
					// Re-sort and auto-select nearest bakery
					if (bakeries.length > 0) {
						const sorted = sortedBakeries();
						bakeries = sorted;
						selectedBakeryId = sorted[0]?.id ?? null;
					}
				},
				() => {}
			);
		}

		try {
			const [bakeriesData, stripeConfig] = await Promise.all([
				api.get<Bakery[]>('/bakeries'),
				api
					.get<{ publishableKey: string }>('/stripe/config')
					.catch(() => ({ publishableKey: '' }))
			]);
			stripePublishableKey = stripeConfig.publishableKey ?? '';
			bakeries = bakeriesData;
			if (bakeries.length > 0) selectedBakeryId = bakeries[0].id;

			if (!isGuest) {
				customer = await api.get<CustomerProfile>('/customers/me');
			}
		} catch (err) {
			pageError = err instanceof Error ? err.message : 'Failed to load checkout data.';
		} finally {
			pageLoading = false;
		}
	});

	// Fetch bakery hours whenever the selected bakery changes
	$effect(() => {
		if (!selectedBakeryId) return;
		const id = selectedBakeryId;
		bakeryHours = [];
		bakeryHoursLoading = true;
		api
			.get<BakeryHour[]>(`/bakeries/${id}/hours`)
			.then((h) => {
				bakeryHours = h;
			})
			.catch(() => {
				bakeryHours = [];
			})
			.finally(() => {
				bakeryHoursLoading = false;
			});
	});

	// Rebuild time slots when date or hours change (30-minute intervals)
	$effect(() => {
		if (!scheduleDate || bakeryHours.length === 0) {
			availableTimeSlots = [];
			return;
		}

		const selected = new Date(scheduleDate + 'T00:00:00');
		const today = new Date();
		today.setHours(0, 0, 0, 0);
		const isToday = selected.getTime() === today.getTime();

		// Earliest bookable time today: now + 2 hours, rounded up to next 30-min boundary
		let minTotalMinutes = 0;
		if (isToday) {
			const now = new Date();
			let earliest = now.getHours() * 60 + now.getMinutes() + 120; // +2 hours
			const rem = earliest % 30;
			if (rem !== 0) earliest += 30 - rem; // round up to next :00 or :30
			minTotalMinutes = earliest;
		}

		const jsDay = selected.getDay(); // 0=Sun
		const dtoDay = jsDay === 0 ? 7 : jsDay; // 1=Mon..7=Sun (backend convention)
		const hourEntry = bakeryHours.find((h) => h.dayOfWeek === dtoDay);
		if (!hourEntry || hourEntry.closed) {
			availableTimeSlots = [];
			scheduleTime = '';
			return;
		}

		const [openH, openM] = hourEntry.openTime.split(':').map(Number);
		const [closeH, closeM] = hourEntry.closeTime.split(':').map(Number);

		// Round open time up to the nearest 30-minute boundary
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

		availableTimeSlots = slots;
		if (slots.length > 0 && (!scheduleTime || !slots.includes(scheduleTime))) {
			scheduleTime = slots[0];
		} else if (slots.length === 0) {
			scheduleTime = '';
		}
	});

	// Auto-fill date when "Schedule for later" is enabled; reset when disabled
	$effect(() => {
		if (!scheduleEnabled) {
			scheduleDate = '';
			scheduleTime = '';
			return;
		}
		// Wait until bakery hours have loaded before setting the default date so
		// the slot-building effect can immediately validate it
		if (!bakeryHours.length) return;
		// Only set the default if the user hasn't already picked a date
		if (!scheduleDate) {
			scheduleDate = asapScheduleDate();
		}
	});

	// ── Submit ───────────────────────────────────────────────────────────────────

	async function confirmOrder() {
		submitError = '';

		if ($cart.items.length === 0) {
			submitError = 'Your cart is empty.';
			return;
		}
		if (!selectedBakeryId) {
			submitError = 'Please select a bakery location.';
			return;
		}

		const contactOk = validateContactFields();
		const deliveryOk = validateDeliveryFields();
		if (!contactOk || !deliveryOk) return;

		submitting = true;
		try {
			type InlineAddress = {
				line1: string;
				line2?: string;
				city: string;
				province: string;
				postalCode: string;
			};
			type CheckoutBody = {
				bakeryId: number;
				orderMethod: string;
				paymentMethod: string;
				comment?: string;
				scheduledAt?: string;
				addressId?: number;
				deliveryAddress?: InlineAddress;
				items: { productId: number; quantity: number }[];
				guest?: {
					firstName?: string;
					lastName?: string;
					email?: string;
					phone?: string;
					addressLine1?: string;
					addressLine2?: string;
					city?: string;
					province?: string;
					postalCode?: string;
				};
			};

			const body: CheckoutBody = {
				bakeryId: selectedBakeryId,
				orderMethod,
				paymentMethod: 'online',
				comment: orderComment || undefined,
				scheduledAt: scheduledAtIso ?? undefined,
				items: $cart.items.map((i) => ({ productId: i.productId, quantity: i.quantity }))
			};

			if (orderMethod === 'delivery') {
				if (isGuest) {
					// Guest: address goes in the guest object
				} else if (useCustomAddress) {
					body.deliveryAddress = {
						line1: deliveryLine1,
						line2: deliveryLine2 || undefined,
						city: deliveryCity,
						province: deliveryProvince,
						postalCode: deliveryPostal
					};
				} else if (customer?.addressId) {
					body.addressId = customer.addressId;
				}
			}

			if (isGuest) {
				body.guest = {
					firstName: guestFirstName || undefined,
					lastName: guestLastName || undefined,
					email: guestEmail || undefined,
					phone: guestPhone || undefined,
					...(orderMethod === 'delivery'
						? {
								addressLine1: deliveryLine1,
								addressLine2: deliveryLine2 || undefined,
								city: deliveryCity,
								province: deliveryProvince,
								postalCode: deliveryPostal
							}
						: {})
				};
			}

			const session = await api.post<{
				orderId: string;
				orderNumber: string;
				clientSecret: string;
				paymentIntentId: string;
				subtotal?: number;
				discount?: number;
				deliveryFee?: number;
				taxAmount?: number;
				grandTotal?: number;
			}>('/orders', body);

			pendingOrderId = session.orderId;
			pendingOrderNumber = session.orderNumber;
			pendingClientSecret = session.clientSecret;
			pendingPaymentIntentId = session.paymentIntentId;
			pendingSubtotal = session.subtotal ?? null;
			pendingDiscount = session.discount ?? null;
			pendingDeliveryFee = session.deliveryFee ?? null;
			pendingTaxAmount = session.taxAmount ?? null;
			pendingGrandTotal = session.grandTotal ?? null;

			// Dev mode: no real Stripe configured — skip straight to confirmation
			if (!stripePublishableKey || pendingClientSecret.startsWith('dev_pi_')) {
				cart.clear();
				goto(resolve(`/orders/${pendingOrderNumber}/confirmation`));
				return;
			}

			phase = 'payment';
			// Give Svelte a tick to render the payment container before mounting
			setTimeout(mountStripeElement, 50);
		} catch (err) {
			submitError = err instanceof Error ? err.message : 'Unexpected error. Please try again.';
		} finally {
			submitting = false;
		}
	}

	// ── Stripe ───────────────────────────────────────────────────────────────────

	// eslint-disable-next-line @typescript-eslint/no-explicit-any
	let stripeInstance: any = null;
	// eslint-disable-next-line @typescript-eslint/no-explicit-any
	let stripeElements: any = null;

	async function mountStripeElement() {
		if (!paymentContainer || !stripePublishableKey || !pendingClientSecret) return;
		stripePaymentLoading = true;

		// Dynamically load Stripe.js if not already present
		// eslint-disable-next-line @typescript-eslint/no-explicit-any
		if (!(window as any).Stripe) {
			await new Promise<void>((res, rej) => {
				const s = document.createElement('script');
				s.src = 'https://js.stripe.com/v3/';
				s.onload = () => res();
				s.onerror = () => rej(new Error('Failed to load Stripe.js'));
				document.head.appendChild(s);
			});
		}

		// eslint-disable-next-line @typescript-eslint/no-explicit-any
		stripeInstance = (window as any).Stripe(stripePublishableKey);
		stripeElements = stripeInstance.elements({ clientSecret: pendingClientSecret });
		const paymentElement = stripeElements.create('payment');
		paymentElement.mount(paymentContainer);
		stripePaymentLoading = false;
	}

	async function pay() {
		if (!stripeInstance || !stripeElements) return;
		stripePaymentLoading = true;
		paymentError = '';

		const returnUrl = `${window.location.origin}/orders/${pendingOrderNumber}/confirmation`;

		const { error } = await stripeInstance.confirmPayment({
			elements: stripeElements,
			redirect: 'if_required',
			confirmParams: { return_url: returnUrl }
		});

		if (error) {
			paymentError = error.message ?? 'Payment failed. Please try again.';
			stripePaymentLoading = false;
			return;
		}

		// Notify the backend the payment succeeded (webhook may also do this, but belt-and-suspenders)
		try {
			await api.post(`/orders/${pendingOrderId}/confirm-stripe-payment`, {
				paymentIntentId: pendingPaymentIntentId
			});
		} catch (err) {
			// Surface fulfillment errors — webhook will also handle this if the request fails
			paymentError = err instanceof Error ? err.message : 'Could not confirm payment. Please contact support.';
			stripePaymentLoading = false;
			return;
		}

		cart.clear();
		goto(resolve(`/orders/${pendingOrderNumber}/confirmation`));
	}
</script>

<main class="mx-auto max-w-2xl px-6 py-16">
	<h1 class="mb-10 font-serif text-4xl font-bold text-foreground">Checkout</h1>

	{#if pageLoading}
		<p class="text-center text-muted-foreground">Loading…</p>

	{:else if pageError}
		<div class="rounded-xl border border-destructive bg-destructive/10 p-6 text-center text-destructive">
			<p>{pageError}</p>
		</div>

	{:else if $cart.items.length === 0}
		<div class="rounded-xl border border-border bg-card p-8 text-center text-muted-foreground">
			<p>Your cart is empty.</p>
			<a href={resolve('/')} class="mt-4 inline-block text-primary hover:underline">Go back to the menu</a>
		</div>

	{:else if phase === 'form'}
		<form onsubmit={(e) => { e.preventDefault(); confirmOrder(); }} class="flex flex-col gap-8">

			<!-- ── Contact ─────────────────────────────────────────────────── -->
			{#if isGuest}
				<section class="rounded-xl border border-border bg-card p-6 shadow-sm">
					<h2 class="mb-1 text-lg font-semibold text-foreground">Contact</h2>
					<p class="mb-4 text-sm text-muted-foreground">
						Already have an account?
						<a href={resolve('/login')} class="text-primary hover:underline">Sign in</a>
					</p>
					<div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
						<div class="flex flex-col gap-1">
							<label for="guestFirst" class="text-sm font-medium text-foreground">First Name</label>
							<input
								id="guestFirst"
								type="text"
								bind:value={guestFirstName}
								onblur={() => handleBlur('guestFirstName')}
								oninput={() => handleInput('guestFirstName')}
								class="rounded-lg border border-input bg-background px-3 py-2 text-sm text-foreground focus:ring-2 focus:ring-ring focus:outline-none {errors.guestFirstName && touched.guestFirstName ? 'ring-2 ring-red-400' : ''}"
							/>
							{#if errors.guestFirstName && touched.guestFirstName}
								<p class="px-1 text-xs text-red-500">{errors.guestFirstName}</p>
							{/if}
						</div>
						<div class="flex flex-col gap-1">
							<label for="guestLast" class="text-sm font-medium text-foreground">Last Name</label>
							<input
								id="guestLast"
								type="text"
								bind:value={guestLastName}
								onblur={() => handleBlur('guestLastName')}
								oninput={() => handleInput('guestLastName')}
								class="rounded-lg border border-input bg-background px-3 py-2 text-sm text-foreground focus:ring-2 focus:ring-ring focus:outline-none {errors.guestLastName && touched.guestLastName ? 'ring-2 ring-red-400' : ''}"
							/>
							{#if errors.guestLastName && touched.guestLastName}
								<p class="px-1 text-xs text-red-500">{errors.guestLastName}</p>
							{/if}
						</div>
						<div class="flex flex-col gap-1">
							<label for="guestEmail" class="text-sm font-medium text-foreground">
								Email <span class="text-red-500">*</span>
							</label>
							<input
								id="guestEmail"
								type="email"
								bind:value={guestEmail}
								onblur={() => handleBlur('guestEmail')}
								oninput={() => handleInput('guestEmail')}
								class="rounded-lg border border-input bg-background px-3 py-2 text-sm text-foreground focus:ring-2 focus:ring-ring focus:outline-none {errors.guestEmail && touched.guestEmail ? 'ring-2 ring-red-400' : ''}"
							/>
							{#if errors.guestEmail && touched.guestEmail}
								<p class="px-1 text-xs text-red-500">{errors.guestEmail}</p>
							{/if}
						</div>
						<div class="flex flex-col gap-1">
							<label for="guestPhone" class="text-sm font-medium text-foreground">
								Phone <span class="text-muted-foreground text-xs">(or email)</span>
							</label>
							<input
								id="guestPhone"
								type="tel"
								value={guestPhone}
								onblur={() => handleBlur('guestPhone')}
								oninput={(e) => { guestPhone = formatPhone((e.target as HTMLInputElement).value); handleInput('guestPhone'); }}
								class="rounded-lg border border-input bg-background px-3 py-2 text-sm text-foreground focus:ring-2 focus:ring-ring focus:outline-none {errors.guestPhone && touched.guestPhone ? 'ring-2 ring-red-400' : ''}"
							/>
							{#if errors.guestPhone && touched.guestPhone}
								<p class="px-1 text-xs text-red-500">{errors.guestPhone}</p>
							{/if}
						</div>
					</div>
				</section>
			{:else if customer}
				<section class="rounded-xl border border-border bg-card p-6 shadow-sm">
					<h2 class="mb-3 text-lg font-semibold text-foreground">Contact</h2>
					<div class="flex items-center justify-between">
						<div>
							{#if customer.firstName || customer.lastName}
								<p class="font-medium text-foreground">
									{[customer.firstName, customer.lastName].filter(Boolean).join(' ')}
								</p>
							{/if}
							{#if customer.email}
								<p class="text-sm text-muted-foreground">{customer.email}</p>
							{/if}
							{#if customer.phone}
								<p class="text-sm text-muted-foreground">{customer.phone}</p>
							{/if}
						</div>
						<a href={resolve('/profile')} class="text-xs text-primary hover:underline">Edit profile</a>
					</div>
				</section>
			{/if}

			<!-- ── Fulfillment ─────────────────────────────────────────────── -->
			<section class="rounded-xl border border-border bg-card p-6 shadow-sm">
				<h2 class="mb-4 text-lg font-semibold text-foreground">Fulfillment</h2>

				<!-- Pickup / Delivery toggle -->
				<div class="mb-5 flex gap-3">
					{#each [['pickup', 'Pickup'], ['delivery', 'Delivery']] as [val, label] (val)}
						<button
							type="button"
							onclick={() => (orderMethod = val as 'pickup' | 'delivery')}
							class="flex-1 rounded-lg border py-2 text-sm font-medium transition-colors {orderMethod === val
								? 'border-primary bg-primary text-primary-foreground'
								: 'border-border bg-background text-foreground hover:bg-muted'}"
						>
							{label}
						</button>
					{/each}
				</div>

				<!-- Bakery selector -->
				<div class="mb-4 flex flex-col gap-1">
					<label for="bakery" class="text-sm font-medium text-foreground">
						{orderMethod === 'pickup' ? 'Pickup Location' : 'Nearest Bakery'}
						<span class="text-destructive">*</span>
					</label>
					{#if userLat !== null}
						<p class="mb-1 text-xs text-muted-foreground">Sorted by distance from your location</p>
					{/if}
					<select
						id="bakery"
						bind:value={selectedBakeryId}
						required
						class="rounded-lg border border-input bg-background px-3 py-2 text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-ring"
					>
						{#each bakeries as b (b.id)}
							<option value={b.id}>{formatBakeryOption(b)}</option>
						{/each}
					</select>
				</div>

				<!-- Selected bakery address -->
				{#if selectedBakery?.address}
					<div class="mb-4 rounded-lg border border-border bg-muted/50 p-3 text-sm">
						<p class="mb-1 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
							{orderMethod === 'pickup' ? 'Pickup address' : 'Bakery address'}
						</p>
						<p class="text-foreground">
							{selectedBakery.address.line1}{selectedBakery.address.line2
								? `, ${selectedBakery.address.line2}`
								: ''}
						</p>
						<p class="text-foreground">
							{selectedBakery.address.city}, {selectedBakery.address.province}
							{selectedBakery.address.postalCode}
						</p>
						{#if selectedBakery.latitude && selectedBakery.longitude && userLat !== null}
							<p class="mt-1 text-xs text-primary font-medium">
								{distanceLabel(selectedBakery)}
							</p>
						{/if}
					</div>
				{/if}

				<!-- Delivery address (logged-in with saved address) -->
				{#if orderMethod === 'delivery' && !isGuest && customer?.address}
					{#if !useCustomAddress}
						<div class="rounded-lg border border-border bg-muted/50 p-3 text-sm">
							<div class="flex items-start justify-between gap-2">
								<div>
									<p class="mb-1 text-xs font-semibold uppercase tracking-wider text-muted-foreground">Delivering to</p>
									<p class="text-foreground">
										{customer.address.line1}{customer.address.line2 ? `, ${customer.address.line2}` : ''}
									</p>
									<p class="text-foreground">
										{customer.address.city}, {customer.address.province}
										{customer.address.postalCode}
									</p>
								</div>
								<button
									type="button"
									onclick={() => { useCustomAddress = true; }}
									class="shrink-0 rounded-lg border border-border px-3 py-1.5 text-xs font-medium text-foreground transition-colors hover:bg-muted"
								>
									Use different address
								</button>
							</div>
						</div>
					{:else}
						<div class="flex items-center justify-between mb-2">
							<p class="text-sm font-medium text-foreground">Delivery address</p>
							<button
								type="button"
								onclick={() => { useCustomAddress = false; deliveryLine1 = ''; deliveryLine2 = ''; deliveryCity = ''; deliveryProvince = ''; deliveryPostal = ''; }}
								class="text-xs text-primary hover:underline"
							>
								Use saved address
							</button>
						</div>
					{/if}
				{/if}

				<!-- Delivery address form (guest or no saved address) -->
				{#if needsDeliveryForm}
					<div class="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2">
						<div class="flex flex-col gap-1 sm:col-span-2">
							<label for="dLine1" class="text-sm font-medium text-foreground">
								Address Line 1 <span class="text-destructive">*</span>
							</label>
							<input
								id="dLine1"
								type="text"
								bind:value={deliveryLine1}
								onblur={() => handleBlur('deliveryLine1')}
								oninput={() => handleInput('deliveryLine1')}
								class="rounded-lg border border-input bg-background px-3 py-2 text-sm text-foreground focus:ring-2 focus:ring-ring focus:outline-none {errors.deliveryLine1 && touched.deliveryLine1 ? 'ring-2 ring-red-400' : ''}"
							/>
							{#if errors.deliveryLine1 && touched.deliveryLine1}
								<p class="px-1 text-xs text-red-500">{errors.deliveryLine1}</p>
							{/if}
						</div>
						<div class="flex flex-col gap-1 sm:col-span-2">
							<label for="dLine2" class="text-sm font-medium text-foreground">Address Line 2</label>
							<input
								id="dLine2"
								type="text"
								bind:value={deliveryLine2}
								class="rounded-lg border border-input bg-background px-3 py-2 text-sm text-foreground focus:ring-2 focus:ring-ring focus:outline-none"
							/>
						</div>
						<div class="flex flex-col gap-1">
							<label for="dCity" class="text-sm font-medium text-foreground">
								City <span class="text-destructive">*</span>
							</label>
							<input
								id="dCity"
								type="text"
								bind:value={deliveryCity}
								maxlength={20}
								onblur={() => handleBlur('deliveryCity')}
								oninput={() => handleInput('deliveryCity')}
								class="rounded-lg border border-input bg-background px-3 py-2 text-sm text-foreground focus:ring-2 focus:ring-ring focus:outline-none {errors.deliveryCity && touched.deliveryCity ? 'ring-2 ring-red-400' : ''}"
							/>
							{#if errors.deliveryCity && touched.deliveryCity}
								<p class="px-1 text-xs text-red-500">{errors.deliveryCity}</p>
							{/if}
						</div>
						<div class="flex flex-col gap-1">
							<label for="dProvince" class="text-sm font-medium text-foreground">
								Province <span class="text-destructive">*</span>
							</label>
							<select
								id="dProvince"
								bind:value={deliveryProvince}
								onblur={() => handleBlur('deliveryProvince')}
								class="rounded-lg border border-input bg-background px-3 py-2 text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-ring {errors.deliveryProvince && touched.deliveryProvince ? 'ring-2 ring-red-400' : ''}"
							>
								<option value="AB">Alberta</option>
								<option value="BC">British Columbia</option>
								<option value="MB">Manitoba</option>
								<option value="NB">New Brunswick</option>
								<option value="NL">Newfoundland and Labrador</option>
								<option value="NS">Nova Scotia</option>
								<option value="NT">Northwest Territories</option>
								<option value="NU">Nunavut</option>
								<option value="ON">Ontario</option>
								<option value="PE">Prince Edward Island</option>
								<option value="QC">Quebec</option>
								<option value="SK">Saskatchewan</option>
								<option value="YT">Yukon</option>
							</select>
							{#if errors.deliveryProvince && touched.deliveryProvince}
								<p class="px-1 text-xs text-red-500">{errors.deliveryProvince}</p>
							{/if}
						</div>
						<div class="flex flex-col gap-1">
							<label for="dPostal" class="text-sm font-medium text-foreground">
								Postal Code <span class="text-destructive">*</span>
							</label>
							<input
								id="dPostal"
								type="text"
								value={deliveryPostal}
								maxlength={7}
								inputmode="text"
								autocomplete="postal-code"
								placeholder="T2X 1Y4"
								onblur={() => handleBlur('deliveryPostal')}
								oninput={(e) => { deliveryPostal = formatCanadianPostalInput((e.target as HTMLInputElement).value); handleInput('deliveryPostal'); }}
								class="rounded-lg border border-input bg-background px-3 py-2 text-sm text-foreground focus:ring-2 focus:ring-ring focus:outline-none {errors.deliveryPostal && touched.deliveryPostal ? 'ring-2 ring-red-400' : ''}"
							/>
							{#if errors.deliveryPostal && touched.deliveryPostal}
								<p class="px-1 text-xs text-red-500">{errors.deliveryPostal}</p>
							{/if}
						</div>
					</div>
				{/if}
			</section>

			<!-- ── Schedule ────────────────────────────────────────────────── -->
			<section class="rounded-xl border border-border bg-card p-6 shadow-sm">
				<div class="flex items-center justify-between">
					<h2 class="text-lg font-semibold text-foreground">Schedule</h2>
					<label class="flex cursor-pointer items-center gap-2 text-sm text-muted-foreground">
						<input type="checkbox" bind:checked={scheduleEnabled} class="accent-primary" />
						Schedule for later
					</label>
				</div>

				{#if !scheduleEnabled}
					<!-- ASAP mode -->
					{#if bakeryHours.length > 0}
						{#if isOpenNow}
							<p class="mt-2 text-sm text-muted-foreground">
								Order will be prepared as soon as possible. Est. ready by
								<span class="font-medium text-foreground">{getAsapEstimateLabel()}</span>.
							</p>
						{:else if nextOpenStr}
							<div class="mt-3 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2.5 text-sm text-amber-800">
								This location is currently closed. It will open <span class="font-medium">{nextOpenStr}</span>.
							</div>
						{:else}
							<p class="mt-2 text-sm text-muted-foreground">
								This location appears to be closed for the near future.
							</p>
						{/if}
					{:else}
						<p class="mt-2 text-sm text-muted-foreground">Order will be prepared as soon as possible.</p>
					{/if}
				{:else}
					<!-- Scheduled mode -->
					<div class="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2">
						<div class="flex flex-col gap-1">
							<label for="schedDate" class="text-sm font-medium text-foreground">Date</label>
							<input
								id="schedDate"
								type="date"
								bind:value={scheduleDate}
								min={minScheduleDate()}
								max={maxScheduleDate()}
								class="rounded-lg border border-input bg-background px-3 py-2 text-sm text-foreground focus:ring-2 focus:ring-ring focus:outline-none"
							/>
						</div>
						<div class="flex flex-col gap-1">
							<label for="schedTime" class="text-sm font-medium text-foreground">Time</label>
							{#if bakeryHoursLoading}
								<p class="rounded-lg border border-border bg-muted/50 px-3 py-2 text-sm text-muted-foreground">
									Loading…
								</p>
							{:else if availableTimeSlots.length > 0}
								<select
									id="schedTime"
									bind:value={scheduleTime}
									class="rounded-lg border border-input bg-background px-3 py-2 text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-ring"
								>
									{#each availableTimeSlots as slot (slot)}
										<option value={slot}>{slot}</option>
									{/each}
								</select>
							{:else if scheduleDate}
								<p class="rounded-lg border border-border bg-muted/50 px-3 py-2 text-sm text-muted-foreground">
									No available slots
								</p>
							{:else}
								<p class="rounded-lg border border-border bg-muted/50 px-3 py-2 text-sm text-muted-foreground">
									Select a date first
								</p>
							{/if}
						</div>
					</div>

					{#if scheduledDayClosedNotice}
						<div class="mt-3 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2.5 text-sm text-amber-800">
							{scheduledDayClosedNotice}
						</div>
					{/if}
				{/if}
			</section>

			<!-- ── Order Notes ─────────────────────────────────────────────── -->
			<section class="rounded-xl border border-border bg-card p-6 shadow-sm">
				<h2 class="mb-4 text-lg font-semibold text-foreground">Order Notes</h2>
				<textarea
					bind:value={orderComment}
					rows={3}
					placeholder="Allergies, special requests…"
					class="w-full rounded-lg border border-input bg-background px-3 py-2 text-sm text-foreground focus:ring-2 focus:ring-ring focus:outline-none"
				></textarea>
			</section>

			<!-- ── Summary ─────────────────────────────────────────────────── -->
			<section class="rounded-xl border border-border bg-card p-6 shadow-sm">
				<h2 class="mb-4 text-lg font-semibold text-foreground">Summary</h2>
				{#if customer?.employeeDiscountEligible}
					<div
						class="mb-4 rounded-lg border border-primary/30 bg-primary/5 px-3 py-2 text-sm font-medium text-primary"
					>
						20% employee discount applies to your order after today&apos;s specials and your loyalty
						tier discount. Final amounts are calculated when you place the order.
					</div>
				{/if}
				{#each $cart.items as item (item.productId)}
					<div class="flex justify-between py-1 text-sm text-muted-foreground">
						<span>{item.productName} × {item.quantity}</span>
						<span>${item.lineTotal.toFixed(2)}</span>
					</div>
				{/each}
				<hr class="my-3 border-border" />
				<p class="text-xs text-muted-foreground">
					Line totals use menu prices. Today&apos;s specials, loyalty tier, tax, and any employee
					discount are applied on the server when you confirm.
				</p>
				<div class="mt-3 flex justify-between text-sm text-muted-foreground">
					<span>Subtotal</span>
					<span>${$cart.subtotal.toFixed(2)}</span>
				</div>
				{#if orderMethod === 'delivery'}
					<div class="mt-1 flex justify-between text-sm text-muted-foreground">
						<span>Delivery fee</span>
						{#if deliveryFee === 0}
							<span class="font-medium text-green-600">Free</span>
						{:else}
							<span>${deliveryFee.toFixed(2)}</span>
						{/if}
					</div>
					{#if deliveryFee > 0}
						<p class="mt-0.5 text-xs text-muted-foreground">Free delivery on orders $50+</p>
					{/if}
				{/if}
				<div class="mt-1 flex justify-between text-sm text-muted-foreground">
					<span>Est. tax (5%)</span>
					<span>${($cart.subtotal * 0.05).toFixed(2)}</span>
				</div>
				<hr class="my-3 border-border" />
				<div class="flex justify-between text-sm font-medium text-foreground">
					<span>Est. total</span>
					<span>${($cart.subtotal + deliveryFee + $cart.subtotal * 0.05).toFixed(2)}</span>
				</div>
				<p class="mt-2 text-xs text-muted-foreground">* Final discounts applied at payment</p>
			</section>

			{#if submitError}
				<p class="rounded-lg border border-destructive bg-destructive/10 px-4 py-3 text-sm text-destructive">
					{submitError}
				</p>
			{/if}

			<button
				type="submit"
				disabled={submitting}
				class="w-full rounded-lg bg-primary py-4 text-sm font-semibold text-primary-foreground transition-colors hover:opacity-90 disabled:opacity-60"
			>
				{submitting ? 'Processing…' : 'Confirm Order'}
			</button>
		</form>

	{:else}
		<!-- ── Payment phase ────────────────────────────────────────────── -->
		<div class="flex flex-col gap-6">
			<div class="rounded-xl border border-border bg-card p-6 shadow-sm">
				<p class="mb-1 text-xs font-semibold uppercase tracking-wider text-muted-foreground">Order</p>
				<p class="mb-4 font-semibold text-foreground">#{pendingOrderNumber}</p>
				<div class="flex flex-col gap-1 text-sm">
					<div class="flex justify-between text-muted-foreground">
						<span>Subtotal</span>
						<span>${(Number(pendingSubtotal ?? $cart.subtotal) + Number(pendingDiscount ?? 0)).toFixed(2)}</span>
					</div>
					{#if Number(pendingDiscount ?? 0) > 0}
						<div class="flex justify-between text-muted-foreground">
							<span>Discount</span>
							<span>−${Number(pendingDiscount).toFixed(2)}</span>
						</div>
					{/if}
					{#if orderMethod === 'delivery'}
						{@const fee = pendingDeliveryFee ?? 0}
						<div class="flex justify-between text-muted-foreground">
							<span>Delivery fee</span>
							{#if fee === 0}
								<span class="font-medium text-green-600">Free</span>
							{:else}
								<span>${Number(fee).toFixed(2)}</span>
							{/if}
						</div>
					{/if}
					<div class="flex justify-between text-muted-foreground">
						<span>Tax (5%)</span>
						<span>${Number(pendingTaxAmount ?? $cart.subtotal * 0.05).toFixed(2)}</span>
					</div>
					<hr class="my-1 border-border" />
					<div class="flex justify-between font-bold text-foreground">
						<span>Total</span>
						<span>${Number(pendingGrandTotal ?? ($cart.subtotal + deliveryFee + $cart.subtotal * 0.05)).toFixed(2)}</span>
					</div>
				</div>
			</div>

			<section class="rounded-xl border border-border bg-card p-6 shadow-sm">
				<h2 class="mb-4 text-lg font-semibold text-foreground">Payment</h2>

				{#if stripePaymentLoading}
					<p class="text-center text-sm text-muted-foreground">Loading payment form…</p>
				{/if}

				<div bind:this={paymentContainer} class="min-h-16"></div>

				{#if paymentError}
					<p class="mt-3 rounded-lg border border-destructive bg-destructive/10 px-4 py-3 text-sm text-destructive">
						{paymentError}
					</p>
				{/if}

				<button
					type="button"
					onclick={pay}
					disabled={stripePaymentLoading}
					class="mt-6 w-full rounded-lg bg-primary py-4 text-sm font-semibold text-primary-foreground transition-colors hover:opacity-90 disabled:opacity-60"
				>
					{stripePaymentLoading ? 'Processing…' : 'Pay Now'}
				</button>
			</section>

			<button
				type="button"
				onclick={() => (phase = 'form')}
				class="text-sm text-muted-foreground hover:text-foreground hover:underline"
			>
				← Back to order details
			</button>
		</div>
	{/if}
</main>
