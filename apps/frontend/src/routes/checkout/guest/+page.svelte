<script lang="ts">
	import { resolve } from '$app/paths';
	import { goto } from '$app/navigation';
	import { cart } from '$lib/stores/cart';
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

	let firstName = $state('');
	let lastName = $state('');
	let email = $state('');
	let phone = $state('');
	let orderMethod = $state<'pickup' | 'delivery'>('pickup');
	let paymentMethod = $state<'cash' | 'credit_card' | 'debit_card' | 'online'>('cash');
	let orderComment = $state('');

	let line1 = $state('');
	let line2 = $state('');
	let city = $state('');
	let province = $state('AB');
	let postalCode = $state('');

	let submitting = $state(false);
	let error = $state('');

	const BAKERY_ID = 1;

	const provinces = [
		{ value: 'AB', label: 'Alberta' },
		{ value: 'BC', label: 'British Columbia' },
		{ value: 'MB', label: 'Manitoba' },
		{ value: 'NB', label: 'New Brunswick' },
		{ value: 'NL', label: 'Newfoundland and Labrador' },
		{ value: 'NS', label: 'Nova Scotia' },
		{ value: 'NT', label: 'Northwest Territories' },
		{ value: 'NU', label: 'Nunavut' },
		{ value: 'ON', label: 'Ontario' },
		{ value: 'PE', label: 'Prince Edward Island' },
		{ value: 'QC', label: 'Quebec' },
		{ value: 'SK', label: 'Saskatchewan' },
		{ value: 'YT', label: 'Yukon' }
	];

	function formatPhone(value: string): string {
		const digits = value.replace(/\D/g, '').substring(0, 10);
		const parts = [];
		if (digits.length > 0) parts.push('(' + digits.substring(0, 3));
		if (digits.length >= 4) parts.push(') ' + digits.substring(3, 6));
		if (digits.length >= 7) parts.push('-' + digits.substring(6, 10));
		return parts.join('');
	}

	async function submit() {
		error = '';

		if (!firstName.trim() || !lastName.trim()) {
			error = 'First and last name are required.';
			return;
		}
		if (!email.trim()) {
			error = 'Email is required.';
			return;
		}
		if (orderMethod === 'delivery' && (!line1 || !city || !province || !postalCode)) {
			error = 'Full delivery address is required for delivery orders.';
			return;
		}
		if ($cart.items.length === 0) {
			error = 'Your cart is empty.';
			return;
		}

		submitting = true;
		try {
			const body: Record<string, unknown> = {
				bakeryId: BAKERY_ID,
				orderMethod,
				paymentMethod,
				comment: orderComment.trim() || undefined,
				pricingLocalDate: localDateYmd(),
				guest: {
					firstName: firstName.trim(),
					lastName: lastName.trim(),
					email: email.trim(),
					phone: phone.trim() || undefined,
					...(orderMethod === 'delivery' && {
						addressLine1: line1.trim(),
						addressLine2: line2.trim() || undefined,
						city: city.trim(),
						province,
						postalCode: postalCode.trim().toUpperCase()
					})
				},
				items: $cart.items.map((i) => ({
					productId: i.productId,
					quantity: i.quantity
				}))
			};

			const res = await fetch(ORDERS_API, {
				method: 'POST',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify(body)
			});

			if (!res.ok) {
				const data = await res.json().catch(() => ({}));
				const msg =
					typeof data.message === 'string'
						? data.message
						: Array.isArray(data.errors)
							? data.errors
									.map((e: { defaultMessage?: string }) => e.defaultMessage)
									.filter(Boolean)
									.join(' ')
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
				scope.setTag('action', 'GUEST_CHECKOUT_FAILED');
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
	<div class="mb-10 flex items-center justify-between">
		<h1 class="font-serif text-4xl font-bold text-foreground">Guest Checkout</h1>
		<a href={resolve('/login?redirectTo=/checkout')} class="text-sm text-primary hover:underline">
			Sign in instead
		</a>
	</div>

	{#if $cart.items.length === 0}
		<div class="rounded-xl border border-border bg-card p-8 text-center text-muted-foreground">
			<p>Your cart is empty.</p>
			<a href={resolve('/')} class="mt-4 inline-block text-primary hover:underline">
				Go back to the menu
			</a>
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
						<label for="firstName" class="text-sm font-medium text-foreground">
							First Name <span class="text-destructive">*</span>
						</label>
						<input
							id="firstName"
							type="text"
							bind:value={firstName}
							required
							class="rounded-lg border border-input bg-background px-3 py-2 text-sm text-foreground focus:ring-2 focus:ring-ring focus:outline-none"
						/>
					</div>
					<div class="flex flex-col gap-1">
						<label for="lastName" class="text-sm font-medium text-foreground">
							Last Name <span class="text-destructive">*</span>
						</label>
						<input
							id="lastName"
							type="text"
							bind:value={lastName}
							required
							class="rounded-lg border border-input bg-background px-3 py-2 text-sm text-foreground focus:ring-2 focus:ring-ring focus:outline-none"
						/>
					</div>
					<div class="flex flex-col gap-1 sm:col-span-2">
						<label for="email" class="text-sm font-medium text-foreground">
							Email <span class="text-destructive">*</span>
						</label>
						<input
							id="email"
							type="email"
							bind:value={email}
							required
							class="rounded-lg border border-input bg-background px-3 py-2 text-sm text-foreground focus:ring-2 focus:ring-ring focus:outline-none"
						/>
					</div>
					<div class="flex flex-col gap-1 sm:col-span-2">
						<label for="phone" class="text-sm font-medium text-foreground">Phone</label>
						<input
							id="phone"
							type="tel"
							bind:value={phone}
							oninput={(e) => {
								phone = formatPhone(e.currentTarget.value);
							}}
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
							<label for="line1" class="text-sm font-medium text-foreground">
								Address Line 1 <span class="text-destructive">*</span>
							</label>
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
							<label for="city" class="text-sm font-medium text-foreground">
								City <span class="text-destructive">*</span>
							</label>
							<input
								id="city"
								type="text"
								bind:value={city}
								required
								class="rounded-lg border border-input bg-background px-3 py-2 text-sm text-foreground focus:ring-2 focus:ring-ring focus:outline-none"
							/>
						</div>
						<div class="flex flex-col gap-1">
							<label for="province" class="text-sm font-medium text-foreground">
								Province <span class="text-destructive">*</span>
							</label>
							<select
								id="province"
								bind:value={province}
								class="rounded-lg border border-input bg-background px-3 py-2 text-sm text-foreground focus:ring-2 focus:ring-ring focus:outline-none"
							>
								{#each provinces as p}
									<option value={p.value}>{p.label}</option>
								{/each}
							</select>
						</div>
						<div class="flex flex-col gap-1">
							<label for="postalCode" class="text-sm font-medium text-foreground">
								Postal Code <span class="text-destructive">*</span>
							</label>
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

			<!-- Order Notes -->
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
				<p class="text-xs text-muted-foreground">
					Line totals use menu prices. Tax and specials are applied when you confirm.
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
