<script lang="ts">
	import { resolve } from '$app/paths';
	import { page } from '$app/state';
	import { api } from '$lib/api';
	import { getProducts } from '$lib/services/products';
	import { ShoppingBag } from '@lucide/svelte';

	const STATUS_STEPS = [
		'placed',
		'pending_payment',
		'paid',
		'preparing',
		'ready',
		'picked_up',
		'delivered',
		'completed'
	];

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
		loading = true;
		error = '';
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

	function statusIndex(status: string): number {
		return STATUS_STEPS.indexOf(status);
	}

	function isCancelled(status: string): boolean {
		return status === 'cancelled' || status === 'scheduled';
	}

	function statusLabel(s: string): string {
		return s.replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase());
	}
</script>

<main class="mx-auto max-w-2xl px-6 py-16">
	<div class="mb-8 flex items-center justify-between">
		<h1 class="font-serif text-3xl font-bold text-foreground">Order Tracking</h1>
		<button class="text-sm text-primary hover:underline" onclick={fetchOrder}> Refresh </button>
	</div>

	{#if loading}
		<p class="text-center text-muted-foreground">Loading order…</p>
	{:else if error}
		<div
			class="rounded-xl border border-destructive bg-destructive/10 p-6 text-center text-destructive"
		>
			<p>{error}</p>
		</div>
	{:else if order}
		<div class="mb-6 rounded-xl border border-border bg-card p-6 shadow-sm">
			<div class="mb-4 flex items-center justify-between">
				<div>
					<p class="text-xs font-semibold tracking-widest text-muted-foreground uppercase">
						Order Number
					</p>
					<p class="font-semibold text-foreground">#{order.orderNumber}</p>
				</div>
				<span class="rounded-full bg-muted px-3 py-1 text-xs font-semibold text-foreground capitalize">
					{statusLabel(order.status)}
				</span>
			</div>

			{#if !isCancelled(order.status)}
				<div class="flex items-center gap-0 overflow-x-auto pb-2">
					{#each STATUS_STEPS as step, i (i)}
						{@const active = i <= statusIndex(order.status)}
						<div class="flex flex-shrink-0 flex-col items-center">
							<div
								class="flex h-6 w-6 items-center justify-center rounded-full text-xs font-bold transition-colors {active
									? 'bg-primary text-primary-foreground'
									: 'bg-muted text-muted-foreground'}"
							>
								{i + 1}
							</div>
							<p class="mt-1 w-16 text-center text-[10px] leading-tight text-muted-foreground">
								{statusLabel(step)}
							</p>
						</div>
						{#if i < STATUS_STEPS.length - 1}
							<div
								class="mb-5 h-0.5 min-w-4 flex-1 transition-colors {i < statusIndex(order.status)
									? 'bg-primary'
									: 'bg-border'}"
							></div>
						{/if}
					{/each}
				</div>
			{:else}
				<p class="text-sm font-semibold text-destructive capitalize">{statusLabel(order.status)}</p>
			{/if}
		</div>

		<div class="flex flex-col gap-3 rounded-xl border border-border bg-card p-6 shadow-sm">
			<h2 class="mb-2 font-semibold text-foreground">Order Details</h2>
			<div class="grid grid-cols-2 gap-3 text-sm">
				<div>
					<p class="mb-1 text-xs font-semibold tracking-widest text-muted-foreground uppercase">
						Method
					</p>
					<p class="text-foreground capitalize">{order.orderMethod}</p>
				</div>
				<div class="col-span-2">
					<p class="mb-1 text-xs font-semibold tracking-widest text-muted-foreground uppercase">
						Placed
					</p>
					<p class="text-foreground">{new Date(order.placedAt).toLocaleString()}</p>
				</div>
			</div>

			<hr class="border-border" />

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
							<ShoppingBag class="h-5 w-5 text-[#C25F1A]/40" />
						</div>
					{/if}
					<div class="min-w-0 flex-1">
						<p class="truncate text-sm font-medium text-foreground">{item.productName}</p>
						<p class="text-xs text-muted-foreground">Qty {item.quantity}</p>
					</div>
					<span class="shrink-0 text-sm font-semibold text-foreground">${Number(item.lineTotal).toFixed(2)}</span>
				</a>
			{/each}

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
					<span>Tax (5%)</span>
					<span>${Number(order.orderTaxAmount).toFixed(2)}</span>
				</div>
			{/if}
			<hr class="border-border" />
			<div class="flex justify-between font-bold text-foreground">
				<span>Total</span>
				<span>${Number(order.orderGrandTotal ?? order.orderTotal).toFixed(2)}</span>
			</div>
		</div>

		<div class="mt-8 text-center">
			<a href={resolve('/')} class="text-sm text-primary hover:underline">Back to Home</a>
		</div>
	{/if}
</main>
