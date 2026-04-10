<script lang="ts">
	import { resolve } from '$app/paths';
	import { goto } from '$app/navigation';
	import { get } from 'svelte/store';
	import { cart } from '$lib/stores/cart';
	import { user } from '$lib/stores/authStore.js';
	import { isProfileComplete } from '$lib/utils/profile';
	import { onMount } from 'svelte';
	import { getProfile, updateProfile } from '$lib/services/profile';
	import { apiFetch } from '$lib/utils/api.js';
	import * as Sentry from '@sentry/sveltekit';
	import { formatCanadianPostalInput } from '$lib/canadianPostalCode';

	const ORDERS_API = '/api/v1/orders';

	function localDateYmd(): string {
		const d = new Date();
		const y = d.getFullYear();
		const m = String(d.getMonth() + 1).padStart(2, '0');
		const day = String(d.getDate()).padStart(2, '0');
		return `${y}-${m}-${day}`;
	}

	let profile = $state<{
		firstName?: string;
		lastName?: string;
		email?: string;
		phone?: string;
		employeeDiscountEligible?: boolean;
		rewardTierDiscountPercent?: number | string | null;
		address?: {
			line1?: string;
			line2?: string;
			city?: string;
			province?: string;
			postalCode?: string;
		};
	} | null>(null);
	let guestName = $state('');
	let guestEmail = $state('');
	let guestPhone = $state('');
	let orderMethod = $state<'pickup' | 'delivery'>('pickup');
	let paymentMethod = $state<'cash' | 'credit_card' | 'debit_card' | 'online'>('cash');
	let orderComment = $state('');

	let line1 = $state('');
	let line2 = $state('');
	let city = $state('');
	let province = $state('');
	let postalCode = $state('');

	let submitting = $state(false);
	let error = $state('');

	const BAKERY_ID = 1;

	onMount(async () => {
		if (!get(user)) {
			goto(resolve('/login?redirect=/checkout'));
			return;
		}
		try {
			profile = await getProfile();

			if (!profile || !isProfileComplete(profile)) {
				goto(resolve('/profile/edit?reason=checkout'));
				return;
			}

			guestName = `${profile.firstName} ${profile.lastName}`.trim();
			guestEmail = profile.email ?? '';
			guestPhone = profile.phone ?? '';
			line1 = profile.address?.line1 ?? '';
			line2 = profile.address?.line2 ?? '';
			city = profile.address?.city ?? '';
			province = profile.address?.province ?? '';
			postalCode = formatCanadianPostalInput(profile.address?.postalCode ?? '');
		} catch {
			console.warn('Failed to load profile, proceeding with empty checkout form');
		}
	});

	async function submit() {
		error = '';
		if (!get(user)) {
			goto(resolve('/login?redirect=/checkout'));
			return;
		}
		if (!guestName || !guestEmail) {
			error = 'Name and email are required.';
			return;
		}
		if (orderMethod === 'delivery' && (!line1 || !city || !province || !postalCode)) {
			error = 'Full delivery address is required.';
			return;
		}
		if ($cart.items.length === 0) {
			error = 'Your cart is empty.';
			return;
		}

		submitting = true;
		try {
			if (orderMethod === 'delivery') {
				await updateProfile({
					address: {
						line1,
						line2: line2 || undefined,
						city,
						province,
						postalCode
					}
				});
			}

			const body: Record<string, unknown> = {
				bakeryId: BAKERY_ID,
				orderMethod,
				paymentMethod,
				comment: orderComment.trim() || undefined,
				pricingLocalDate: localDateYmd(),
				items: $cart.items.map((i) => ({
					productId: i.productId,
					quantity: i.quantity
				}))
			};

			const res = await apiFetch(ORDERS_API, {
				method: 'POST',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify(body)
			});

			if (!res) return;

			if (!res.ok) {
				const data = await res.json().catch(() => ({}));
				const msg =
					typeof data.message === 'string'
						? data.message
						: Array.isArray(data.errors)
							? data.errors.map((e: { defaultMessage?: string }) => e.defaultMessage).filter(Boolean).join(' ')
							: '';
				throw new Error(msg || `Server error ${res.status}`);
			}

			const session = (await res.json()) as { orderId?: string; orderNumber?: string };
			const orderId = session.orderId;
			if (!orderId) {
				throw new Error('Order placed but no order id returned.');
			}
			cart.clear();
			goto(resolve(`/orders/${orderId}/confirmation`));
		} catch (err: unknown) {
			Sentry.withScope((scope) => {
				scope.setTag('action', 'CHECKOUT_FAILED');
				scope.setTag('reason', 'api_error');
				Sentry.captureException(err instanceof Error ? err : new Error(String(err)));
			});
			error = err instanceof Error ? err.message : 'Unexpected error. Please try again.';
		} finally {
			submitting = false;
		}
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
	{:else}
		<form
			onsubmit={(e) => {
				e.preventDefault();
				submit();
			}}
			class="flex flex-col gap-8"
		>
			<!-- Contact -->
			<section class="rounded-xl border border-border bg-card p-6 shadow-sm">
				<h2 class="mb-4 text-lg font-semibold text-foreground">Contact</h2>
				<div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
					<div class="flex flex-col gap-1">
						<label for="guestName" class="text-sm font-medium text-foreground"
							>Name <span class="text-destructive">*</span></label
						>
						<input
							id="guestName"
							type="text"
							bind:value={guestName}
							required
							class="rounded-lg border border-input bg-background px-3 py-2 text-sm text-foreground focus:ring-2 focus:ring-ring focus:outline-none"
						/>
					</div>
					<div class="flex flex-col gap-1">
						<label for="guestEmail" class="text-sm font-medium text-foreground"
							>Email <span class="text-destructive">*</span></label
						>
						<input
							id="guestEmail"
							type="email"
							bind:value={guestEmail}
							required
							class="rounded-lg border border-input bg-background px-3 py-2 text-sm text-foreground focus:ring-2 focus:ring-ring focus:outline-none"
						/>
					</div>
					<div class="flex flex-col gap-1 sm:col-span-2">
						<label for="guestPhone" class="text-sm font-medium text-foreground">Phone</label>
						<input
							id="guestPhone"
							type="tel"
							bind:value={guestPhone}
							class="rounded-lg border border-input bg-background px-3 py-2 text-sm text-foreground focus:ring-2 focus:ring-ring focus:outline-none"
						/>
					</div>
				</div>
			</section>

			<!-- Fulfillment -->
			<section class="rounded-xl border border-border bg-card p-6 shadow-sm">
				<h2 class="mb-4 text-lg font-semibold text-foreground">Fulfillment</h2>
				<div class="flex gap-4">
					<label class="flex cursor-pointer items-center gap-2 text-sm">
						<input type="radio" bind:group={orderMethod} value="pickup" class="accent-primary" />
						Pickup
					</label>
					<label class="flex cursor-pointer items-center gap-2 text-sm">
						<input type="radio" bind:group={orderMethod} value="delivery" class="accent-primary" />
						Delivery
					</label>
				</div>

				{#if orderMethod === 'delivery'}
					<div class="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2">
						<div class="flex flex-col gap-1 sm:col-span-2">
							<label for="line1" class="text-sm font-medium text-foreground"
								>Address Line 1 <span class="text-destructive">*</span></label
							>
							<input
								id="line1"
								type="text"
								bind:value={line1}
								required
								class="rounded-lg border border-input bg-background px-3 py-2 text-sm text-foreground focus:ring-2 focus:ring-ring focus:outline-none"
							/>
						</div>
						<div class="flex flex-col gap-1 sm:col-span-2">
							<label for="line2" class="text-sm font-medium text-foreground">Address Line 2</label>
							<input
								id="line2"
								type="text"
								bind:value={line2}
								class="rounded-lg border border-input bg-background px-3 py-2 text-sm text-foreground focus:ring-2 focus:ring-ring focus:outline-none"
							/>
						</div>
						<div class="flex flex-col gap-1">
							<label for="city" class="text-sm font-medium text-foreground"
								>City <span class="text-destructive">*</span></label
							>
							<input
								id="city"
								type="text"
								bind:value={city}
								required
								class="rounded-lg border border-input bg-background px-3 py-2 text-sm text-foreground focus:ring-2 focus:ring-ring focus:outline-none"
							/>
						</div>
						<div class="flex flex-col gap-1">
							<label for="province" class="text-sm font-medium text-foreground"
								>Province <span class="text-destructive">*</span></label
							>
							<input
								id="province"
								type="text"
								bind:value={province}
								required
								class="rounded-lg border border-input bg-background px-3 py-2 text-sm text-foreground focus:ring-2 focus:ring-ring focus:outline-none"
							/>
						</div>
						<div class="flex flex-col gap-1">
							<label for="postalCode" class="text-sm font-medium text-foreground"
								>Postal Code <span class="text-destructive">*</span></label
							>
							<input
								id="postalCode"
								type="text"
								inputmode="text"
								autocomplete="postal-code"
								placeholder="A1A 1A1"
								maxlength="7"
								value={postalCode}
								oninput={(e) => {
									postalCode = formatCanadianPostalInput(e.currentTarget.value);
								}}
								required
								class="rounded-lg border border-input bg-background px-3 py-2 text-sm text-foreground focus:ring-2 focus:ring-ring focus:outline-none"
							/>
						</div>
					</div>
				{/if}
			</section>

			<!-- Payment -->
			<section class="rounded-xl border border-border bg-card p-6 shadow-sm">
				<h2 class="mb-4 text-lg font-semibold text-foreground">Payment</h2>
				<div class="grid grid-cols-2 gap-3 sm:grid-cols-4">
					{#each [['cash', 'Cash'], ['credit_card', 'Credit Card'], ['debit_card', 'Debit Card'], ['online', 'Online']] as [val, label] (val)}
						<label
							class="flex cursor-pointer items-center gap-2 rounded-lg border border-border px-3 py-2 text-sm transition-colors hover:bg-muted {paymentMethod ===
							val
								? 'border-primary bg-muted'
								: ''}"
						>
							<input type="radio" bind:group={paymentMethod} value={val} class="accent-primary" />
							{label}
						</label>
					{/each}
				</div>
			</section>

			<!-- Comment -->
			<section class="rounded-xl border border-border bg-card p-6 shadow-sm">
				<h2 class="mb-4 text-lg font-semibold text-foreground">Order Notes</h2>
				<textarea
					bind:value={orderComment}
					rows={3}
					placeholder="Allergies, special requests…"
					class="w-full rounded-lg border border-input bg-background px-3 py-2 text-sm text-foreground focus:ring-2 focus:ring-ring focus:outline-none"
				></textarea>
			</section>

			<!-- Summary -->
			<section class="rounded-xl border border-border bg-card p-6 shadow-sm">
				<h2 class="mb-4 text-lg font-semibold text-foreground">Summary</h2>
				{#if profile?.employeeDiscountEligible}
					<div
						class="mb-4 rounded-lg border border-primary/30 bg-primary/5 px-3 py-2 text-sm font-medium text-primary"
					>
						20% employee discount applies to your order after today&apos;s specials and your loyalty tier
						discount. Final amounts are calculated when you place the order.
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
					Line totals use menu prices. Today&apos;s specials, loyalty tier, tax, and any employee discount are
					applied on the server when you confirm.
				</p>
				<div class="mt-3 flex justify-between text-sm text-muted-foreground">
					<span>Cart subtotal (list prices)</span>
					<span>${$cart.subtotal.toFixed(2)}</span>
				</div>
			</section>

			{#if error}
				<p
					class="rounded-lg border border-destructive bg-destructive/10 px-4 py-3 text-sm text-destructive"
				>
					{error}
				</p>
			{/if}

			<button
				type="submit"
				disabled={submitting}
				class="w-full rounded-lg bg-primary py-4 text-sm font-semibold text-primary-foreground transition-colors hover:opacity-90 disabled:opacity-60"
			>
				{submitting ? 'Placing Order…' : 'Place Order'}
			</button>
		</form>
	{/if}
</main>
