<script lang="ts">
	import { resolve } from '$app/paths';
	import { page } from '$app/state';
	import { api } from '$lib/api';

	interface OrderItem {
		productName: string;
		quantity: number;
		lineTotal: number;
	}

	interface Order {
		orderNumber: string;
		status: string;
		orderMethod: string;
		placedAt: string;
		orderTotal: number;
		orderDiscount: number;
		items: OrderItem[];
	}

	let order = $state<Order | null>(null);
	let loading = $state(true);
	let error = $state('');

	const orderNumber = page.params.orderNumber;

	async function fetchOrder() {
		try {
			order = await api.get<Order>(`/orders/by-number/${orderNumber}`);
		} catch (err: unknown) {
			error = err instanceof Error ? err.message : 'Could not load order.';
		} finally {
			loading = false;
		}
	}

	fetchOrder();
</script>

<main class="mx-auto max-w-2xl px-6 py-16">
	{#if loading}
		<p class="text-center text-muted-foreground">Loading your order…</p>
	{:else if error}
		<div
			class="rounded-xl border border-destructive bg-destructive/10 p-6 text-center text-destructive"
		>
			<p>{error}</p>
			<a href={resolve('/')} class="mt-4 inline-block text-primary hover:underline">Return home</a>
		</div>
	{:else if order}
		<div class="mb-10 flex flex-col items-center gap-2 text-center">
			<h1 class="font-serif text-4xl font-bold text-foreground">Order Confirmed!</h1>
			<p class="text-muted-foreground">
				Your order <span class="font-semibold text-foreground">#{order.orderNumber}</span> has been
				placed.
			</p>
		</div>

		<div class="flex flex-col gap-4 rounded-xl border border-border bg-card p-6 shadow-sm">
			<div class="grid grid-cols-2 gap-4 text-sm">
				<div>
					<p class="mb-1 text-xs font-semibold tracking-widest text-muted-foreground uppercase">
						Status
					</p>
					<p class="font-medium text-foreground capitalize">{order.status.replace(/_/g, ' ')}</p>
				</div>
				<div>
					<p class="mb-1 text-xs font-semibold tracking-widest text-muted-foreground uppercase">
						Method
					</p>
					<p class="font-medium text-foreground capitalize">{order.orderMethod}</p>
				</div>
				<div class="col-span-2">
					<p class="mb-1 text-xs font-semibold tracking-widest text-muted-foreground uppercase">
						Placed
					</p>
					<p class="font-medium text-foreground">
						{new Date(order.placedAt).toLocaleString()}
					</p>
				</div>
			</div>

			<hr class="border-border" />

			<div class="flex flex-col gap-2">
				{#each order.items as item (item.productName)}
					<div class="flex justify-between text-sm">
						<span class="text-muted-foreground">{item.productName} × {item.quantity}</span>
						<span class="text-foreground">${Number(item.lineTotal).toFixed(2)}</span>
					</div>
				{/each}
			</div>

			<hr class="border-border" />

			{#if Number(order.orderDiscount) > 0}
				<div class="flex justify-between text-sm text-accent">
					<span>Discount</span>
					<span>−${Number(order.orderDiscount).toFixed(2)}</span>
				</div>
			{/if}
			<div class="flex justify-between font-bold text-foreground">
				<span>Total</span>
				<span>${Number(order.orderTotal).toFixed(2)}</span>
			</div>
		</div>

		<div class="mt-8 flex flex-col gap-3 sm:flex-row sm:justify-center">
			<a
				href={resolve(`/orders/${order.orderNumber}`)}
				class="rounded-lg border border-border px-6 py-3 text-center text-sm font-medium text-foreground transition-colors hover:bg-muted"
			>
				Track Order
			</a>
			<a
				href={resolve('/')}
				class="rounded-lg bg-primary px-6 py-3 text-center text-sm font-semibold text-primary-foreground transition-colors hover:opacity-90"
			>
				Back to Home
			</a>
		</div>
	{/if}
</main>
