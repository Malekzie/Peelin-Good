<script lang="ts">
	import { resolve } from '$app/paths';
	import { goto } from '$app/navigation';
	import { cart } from '$lib/stores/cart';
	import { isProfileComplete } from '$lib/utils/profile';
	import { onMount } from 'svelte';
	import { getProfile } from '$lib/services/profile';
	import * as Sentry from '@sentry/sveltekit';

	const API = 'http://localhost:8080';

	let profile = $state<{
		firstName?: string;
		lastName?: string;
		email?: string;
		phone?: string;
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
			postalCode = profile.address?.postalCode ?? '';
		} catch {
			console.warn('Failed to load profile, proceeding with empty checkout form');
		}
	});

	async function submit() {
		error = '';
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
			const body: Record<string, unknown> = {
				guestName,
				guestEmail,
				guestPhone: guestPhone || undefined,
				bakeryId: BAKERY_ID,
				orderMethod,
				paymentMethod,
				orderComment: orderComment || undefined,
				// eslint-disable-next-line @typescript-eslint/no-explicit-any
				items: $cart.items.map((i: any) => ({
					productId: i.productId,
					quantity: i.quantity
				}))
			};

			if (orderMethod === 'delivery') {
				body.deliveryAddress = { line1, line2: line2 || undefined, city, province, postalCode };
			}

			const res = await fetch(`${API}/api/v1/checkout`, {
				method: 'POST',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify(body)
			});

			if (!res.ok) {
				const data = await res.json().catch(() => ({}));
				throw new Error(data.message ?? `Server error ${res.status}`);
			}

			const order = await res.json();
			cart.clear();
			goto(resolve(`/orders/${order.orderNumber}/confirmation`));
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
								bind:value={postalCode}
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
				{#each $cart.items as item (item.productId)}
					<div class="flex justify-between py-1 text-sm text-muted-foreground">
						<span>{item.productName} × {item.quantity}</span>
						<span>${item.lineTotal.toFixed(2)}</span>
					</div>
				{/each}
				<hr class="my-3 border-border" />
				{#if $cart.discount > 0}
					<div class="flex justify-between text-sm text-accent">
						<span>Discount</span>
						<span>−${$cart.discount.toFixed(2)}</span>
					</div>
				{/if}
				<div class="mt-1 flex justify-between font-bold text-foreground">
					<span>Total</span>
					<span>${$cart.total.toFixed(2)}</span>
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
