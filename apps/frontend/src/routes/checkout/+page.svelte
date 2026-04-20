<script lang="ts">
	import { resolve } from '$app/paths';
	import { goto } from '$app/navigation';
	import { cart } from '$lib/stores/cart';
	import { onMount, untrack } from 'svelte';
	import { api } from '$lib/utils/apiClient';
	import { isProfileComplete } from '$lib/utils/profile';
	import {
		validateField,
		generateTimeSlots,
		sortedBakeries,
		asapScheduleDate,
		type Bakery,
		type BakeryHour,
		type CustomerProfile,
		type ErrorKey
	} from '$lib/services/checkout';
	import type { PageData } from './$types';
	import CheckoutContact from '$lib/components/checkout/CheckoutContact.svelte';
	import CheckoutGuestContact from '$lib/components/checkout/CheckoutGuestContact.svelte';
	import CheckoutDeliveryAddress from '$lib/components/checkout/CheckoutDeliveryAddress.svelte';
	import CheckoutFulfillment from '$lib/components/checkout/CheckoutFulfillment.svelte';
	import CheckoutSchedule from '$lib/components/checkout/CheckoutSchedule.svelte';
	import CheckoutOrderNotes from '$lib/components/checkout/CheckoutOrderNotes.svelte';
	import CheckoutSummary from '$lib/components/checkout/CheckoutSummary.svelte';
	import CheckoutPayment from '$lib/components/checkout/CheckoutPayment.svelte';

	// ── Data from server ─────────────────────────────────────────────────────────

	let { data }: { data: PageData } = $props();

	const isGuest = !untrack(() => data.user);

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

	// ── Validation ───────────────────────────────────────────────────────────

	let errors = $state<Record<ErrorKey, string>>({
		guestFirstName: '',
		guestLastName: '',
		guestEmail: '',
		guestPhone: '',
		deliveryLine1: '',
		deliveryLine2: '',
		deliveryCity: '',
		deliveryProvince: '',
		deliveryPostal: ''
	});

	let touched = $state<Record<ErrorKey, boolean>>({
		guestFirstName: false,
		guestLastName: false,
		guestEmail: false,
		guestPhone: false,
		deliveryLine1: false,
		deliveryLine2: false,
		deliveryCity: false,
		deliveryProvince: false,
		deliveryPostal: false
	});

	function currentValues() {
		return {
			guestFirstName,
			guestLastName,
			guestEmail,
			guestPhone,
			deliveryLine1,
			deliveryLine2,
			deliveryCity,
			deliveryProvince,
			deliveryPostal
		};
	}

	function handleBlur(name: ErrorKey) {
		touched[name] = true;
		errors[name] = validateField(name, currentValues());
	}

	function handleInput(name: ErrorKey) {
		if (touched[name]) {
			errors[name] = validateField(name, currentValues());
		}
	}

	function validateContactFields(): boolean {
		if (!isGuest) return true;
		const fields: ErrorKey[] = ['guestFirstName', 'guestLastName', 'guestEmail', 'guestPhone'];
		fields.forEach((f) => {
			touched[f] = true;
			errors[f] = validateField(f, currentValues());
		});
		return !fields.some((f) => errors[f]);
	}

	function validateDeliveryFields(): boolean {
		if (!needsDeliveryForm) return true;
		const fields: ErrorKey[] = [
			'deliveryLine1',
			'deliveryCity',
			'deliveryProvince',
			'deliveryPostal'
		];
		fields.forEach((f) => {
			touched[f] = true;
			errors[f] = validateField(f, currentValues());
		});
		return !fields.some((f) => errors[f]);
	}

	// Loaded data (from server) — untrack signals intentional one-time snapshots
	let customer = $state<CustomerProfile | null>(untrack(() => data.customer));
	let bakeries = $state<Bakery[]>(untrack(() => data.bakeries));
	let selectedBakeryId = $state<number | null>(untrack(() => data.bakeries[0]?.id ?? null));
	const stripePublishableKey = untrack(() => data.stripePublishableKey);

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

	// ── Derived ──────────────────────────────────────────────────────────────────

	const DELIVERY_FEE = 7;
	const DELIVERY_FREE_THRESHOLD = 50;

	const deliveryFee = $derived(
		orderMethod === 'delivery' && $cart.subtotal < DELIVERY_FREE_THRESHOLD ? DELIVERY_FEE : 0
	);

	const needsDeliveryForm = $derived(
		orderMethod === 'delivery' && (isGuest || !customer?.addressId || useCustomAddress)
	);

	const hasScheduledAt = $derived(scheduleEnabled && !!scheduleDate && !!scheduleTime);
	const scheduledAtIso = $derived(
		hasScheduledAt ? edmontonLocalToIso(scheduleDate, scheduleTime) : null
	);

	// Bakery hours are stored in America/Edmonton; the picker shows Edmonton wall-clock time.
	// Naively appending "Z" sent user-entered times as UTC, so server TZ conversion misaligned
	// and rejected in-hours picks as out-of-hours (e.g. 11:00 local → 05:00 MDT).
	function edmontonLocalToIso(dateStr: string, timeStr: string): string {
		const [y, mo, d] = dateStr.split('-').map(Number);
		const [h, mi] = timeStr.split(':').map(Number);
		const asUtcMs = Date.UTC(y, mo - 1, d, h, mi, 0);
		const offsetMs = timeZoneOffsetMs('America/Edmonton', new Date(asUtcMs));
		return new Date(asUtcMs - offsetMs).toISOString();
	}

	function timeZoneOffsetMs(timeZone: string, instant: Date): number {
		const parts = new Intl.DateTimeFormat('en-US', {
			timeZone,
			hour12: false,
			year: 'numeric',
			month: '2-digit',
			day: '2-digit',
			hour: '2-digit',
			minute: '2-digit',
			second: '2-digit'
		}).formatToParts(instant);
		const pick = (type: string) => Number(parts.find((p) => p.type === type)!.value);
		const asIfUtc = Date.UTC(
			pick('year'),
			pick('month') - 1,
			pick('day'),
			pick('hour') % 24,
			pick('minute'),
			pick('second')
		);
		return asIfUtc - instant.getTime();
	}

	// ── Lifecycle ────────────────────────────────────────────────────────────────

	onMount(() => {
		if ($cart.items.length === 0) {
			goto(resolve('/'));
			return;
		}
		if (!isGuest && customer && !isProfileComplete(customer)) {
			goto(resolve('/profile/edit?reason=checkout'));
			return;
		}

		// Request geolocation — silently ignore if denied
		if (typeof navigator !== 'undefined' && navigator.geolocation) {
			navigator.geolocation.getCurrentPosition(
				(pos) => {
					userLat = pos.coords.latitude;
					userLng = pos.coords.longitude;
				},
				() => {}
			);
		}
	});

	$effect(() => {
		if (userLat !== null && userLng !== null && bakeries.length > 0) {
			const sorted = sortedBakeries(bakeries, userLat, userLng);
			const ids = sorted.map((b) => b.id).join(',');
			const currentIds = bakeries.map((b) => b.id).join(',');
			if (ids !== currentIds) {
				bakeries = sorted;
			}
			if (!selectedBakeryId) selectedBakeryId = sorted[0]?.id ?? null;
		}
	});

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

	$effect(() => {
		const slots = generateTimeSlots(scheduleDate, bakeryHours);
		availableTimeSlots = slots;
		if (slots.length > 0 && (!scheduleTime || !slots.includes(scheduleTime))) {
			scheduleTime = slots[0];
		} else if (slots.length === 0) {
			scheduleTime = '';
		}
	});

	$effect(() => {
		if (!scheduleEnabled) {
			scheduleDate = '';
			scheduleTime = '';
			return;
		}
		if (!bakeryHours.length) return;
		if (!scheduleDate) scheduleDate = asapScheduleDate();
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
				if (!isGuest && useCustomAddress) {
					body.deliveryAddress = {
						line1: deliveryLine1,
						line2: deliveryLine2 || undefined,
						city: deliveryCity,
						province: deliveryProvince,
						postalCode: deliveryPostal
					};
				} else if (!isGuest && customer?.addressId) {
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

			// Persist the guest email so the confirmation / tracking pages can pass it back
			// to the backend as the second-factor for order lookup. For logged-in customers
			// the backend resolves ownership from the session, so we do not need to store it.
			if (isGuest && guestEmail && typeof sessionStorage !== 'undefined') {
				sessionStorage.setItem(`guestOrderEmail:${session.orderNumber}`, guestEmail);
			}

			if (!stripePublishableKey) {
				submitError = 'Payment processing is not available. Stripe is not configured.';
				return;
			}

			phase = 'payment';
		} catch (err) {
			submitError = err instanceof Error ? err.message : 'Unexpected error. Please try again.';
		} finally {
			submitting = false;
		}
	}

	function handlePaymentSuccess(orderNumber: string) {
		cart.clear();
		goto(resolve(`/guest/orders/${orderNumber}/confirmation`));
	}
</script>

<main class="mx-auto max-w-2xl px-6 py-16">
	<h1 class="mb-10 font-serif text-4xl font-bold text-foreground">Checkout</h1>

	{#if $cart.items.length === 0}
		<div class="rounded-xl border border-border bg-card p-8 text-center text-muted-foreground">
			<p>Your cart is empty.</p>
			<a href={resolve('/')} class="mt-4 inline-block text-primary hover:underline"
				>Go back to the menu</a
			>
		</div>
	{:else if phase === 'form'}
		<form
			onsubmit={(e) => {
				e.preventDefault();
				confirmOrder();
			}}
			class="flex flex-col gap-8"
		>
			{#if isGuest}
				<CheckoutGuestContact
					bind:firstName={guestFirstName}
					bind:lastName={guestLastName}
					bind:email={guestEmail}
					bind:phone={guestPhone}
					{errors}
					{touched}
					onBlur={handleBlur}
					onInput={handleInput}
					onPhoneInput={(value) => {
						guestPhone = value;
						if (touched.guestPhone)
							errors.guestPhone = validateField('guestPhone', currentValues());
					}}
				/>
			{:else if customer}
				<CheckoutContact {customer} />
			{/if}

			<CheckoutFulfillment
				{bakeries}
				bind:selectedBakeryId
				bind:orderMethod
				{userLat}
				{userLng}
				{isGuest}
				{customer}
				bind:useCustomAddress
				{deliveryLine1}
				{deliveryLine2}
				{deliveryCity}
				{deliveryProvince}
				{deliveryPostal}
				onBakeryChange={() => {}}
				onMethodChange={() => {}}
				onUseCustomAddress={() => (useCustomAddress = true)}
				onUseSavedAddress={() => {
					useCustomAddress = false;
					deliveryLine1 = '';
					deliveryLine2 = '';
					deliveryCity = '';
					deliveryProvince = 'AB';
					deliveryPostal = '';
				}}
			/>

			{#if needsDeliveryForm}
				<section class="rounded-xl border border-border bg-card p-6 shadow-sm">
					<h2 class="mb-4 text-lg font-semibold text-foreground">Delivery Address</h2>
					<CheckoutDeliveryAddress
						bind:line1={deliveryLine1}
						bind:line2={deliveryLine2}
						bind:city={deliveryCity}
						bind:province={deliveryProvince}
						bind:postal={deliveryPostal}
						{errors}
						{touched}
						onBlur={handleBlur}
						onInput={handleInput}
						onPostalInput={() => {}}
					/>
				</section>
			{/if}

			<CheckoutSchedule
				bind:scheduleEnabled
				bind:scheduleDate
				bind:scheduleTime
				{bakeryHours}
				{bakeryHoursLoading}
				{availableTimeSlots}
				onScheduleToggle={() => {}}
				onDateChange={() => {}}
				onTimeChange={() => {}}
			/>

			<CheckoutOrderNotes bind:value={orderComment} />

			<CheckoutSummary
				items={$cart.items}
				subtotal={$cart.subtotal}
				{deliveryFee}
				{orderMethod}
				{customer}
			/>

			{#if submitError}
				<p
					class="rounded-lg border border-destructive bg-destructive/10 px-4 py-3 text-sm text-destructive"
				>
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
		<CheckoutPayment
			orderNumber={pendingOrderNumber}
			orderId={pendingOrderId}
			publishableKey={stripePublishableKey}
			clientSecret={pendingClientSecret}
			paymentIntentId={pendingPaymentIntentId}
			subtotal={Number(pendingSubtotal ?? $cart.subtotal)}
			discount={pendingDiscount}
			deliveryFee={pendingDeliveryFee}
			taxAmount={pendingTaxAmount}
			grandTotal={pendingGrandTotal}
			{orderMethod}
			onSuccess={handlePaymentSuccess}
			onBack={() => (phase = 'form')}
		/>
	{/if}
</main>
