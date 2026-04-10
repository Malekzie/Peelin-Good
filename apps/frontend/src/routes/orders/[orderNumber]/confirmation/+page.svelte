<script lang="ts">
	import { resolve } from '$app/paths';
	import { page } from '$app/state';
	import { api } from '$lib/api';
	import { getProducts } from '$lib/services/products';
	import { ShoppingBag } from '@lucide/svelte';

	interface OrderItem {
		productId: number;
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
		orderTaxAmount?: number;
		orderGrandTotal?: number;
		deliveryFee?: number;
		items: OrderItem[];
	}

	let order = $state<Order | null>(null);
	let productImages = $state<Record<number, string | null>>({});
	let loading = $state(true);
	let error = $state('');

	const orderNumber = page.params.orderNumber;

	async function fetchOrder() {
		try {
			const [orderData, productsData] = await Promise.all([
				api.get<Order>(`/orders/by-number/${orderNumber}`),
				getProducts()
			]);
			order = orderData;
			const map: Record<number, string | null> = {};
			for (const p of productsData ?? []) {
				map[p.id] = p.imageUrl ?? null;
			}
			productImages = map;
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
					<a
						href={resolve(`/menu?product=${item.productId}`)}
						class="flex items-center gap-3 rounded-lg border border-border bg-background px-3 py-2 transition-colors hover:bg-muted/60"
					>
						{#if productImages[item.productId]}
							<img
								src={productImages[item.productId]}
								alt={item.productName}
								class="h-12 w-12 shrink-0 rounded-md object-cover"
							/>
						{:else}
							<div class="flex h-12 w-12 shrink-0 items-center justify-center rounded-md bg-[#F5EFE6]">
								<ShoppingBag class="h-5 w-5 text-[#C4714A]/40" />
							</div>
						{/if}
						<div class="min-w-0 flex-1">
							<p class="truncate text-sm font-medium text-foreground">{item.productName}</p>
							<p class="text-xs text-muted-foreground">Qty {item.quantity}</p>
						</div>
						<span class="shrink-0 text-sm font-semibold text-foreground">${Number(item.lineTotal).toFixed(2)}</span>
					</a>
				{/each}
			</div>

			<hr class="border-border" />

			<div class="flex justify-between text-sm text-muted-foreground">
				<span>Subtotal</span>
				<span>${Number(order.orderTotal).toFixed(2)}</span>
			</div>
			{#if Number(order.orderDiscount) > 0}
				<div class="flex justify-between text-sm text-muted-foreground">
					<span>Discount</span>
					<span>−${Number(order.orderDiscount).toFixed(2)}</span>
				</div>
			{/if}
			{#if order.orderMethod === 'delivery'}
				{@const fee = order.deliveryFee ?? 0}
				<div class="flex justify-between text-sm text-muted-foreground">
					<span>Delivery fee</span>
					{#if fee === 0}
						<span class="font-medium text-green-600">Free</span>
					{:else}
						<span>${Number(fee).toFixed(2)}</span>
					{/if}
				</div>
			{/if}
			{#if order.orderTaxAmount}
				<div class="flex justify-between text-sm text-muted-foreground">
					<span>Tax (13%)</span>
					<span>${Number(order.orderTaxAmount).toFixed(2)}</span>
				</div>
			{/if}
			<hr class="border-border" />
			<div class="flex justify-between font-bold text-foreground">
				<span>Total</span>
				<span>${Number(order.orderGrandTotal ?? order.orderTotal).toFixed(2)}</span>
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
