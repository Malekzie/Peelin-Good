<script lang="ts">
	import { resolve } from '$app/paths';
	import { api } from '$lib/utils/apiClient';
	import { getProducts } from '$lib/services/products';
	import { ShoppingBag } from '@lucide/svelte';
	import { formatDiscountCad, formatPriceCad } from '$lib/utils/money';

	let { orderNumber }: { orderNumber: string } = $props();

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
		comment?: string;
		items: OrderItem[];
	}

	let order = $state<Order | null>(null);
	let productImages = $state<Record<number, string | null>>({});
	let loading = $state(true);
	let error = $state('');

	async function fetchOrder() {
		loading = true;
		error = '';
		try {
			const [orderData, productsData] = await Promise.all([
				api.get<Order>(`/orders/by-number/${orderNumber}`),
				getProducts()
			]);
			order = orderData;
			const map: Record<string, string | null> = {};
			for (const p of productsData ?? []) {
				map[String(p.id)] = p.imageUrl ?? null;
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
		const i = STATUS_STEPS.indexOf(status);
		return i < 0 ? 0 : i;
	}

	function isCancelled(status: string): boolean {
		return status === 'cancelled' || status === 'scheduled';
	}

	function statusLabel(s: string): string {
		return s.replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase());
	}
</script>

<main class="mx-auto w-full max-w-2xl px-6 py-16">
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
				<span
					class="rounded-full bg-muted px-3 py-1 text-xs font-semibold text-foreground capitalize"
				>
					{statusLabel(order.status)}
				</span>
			</div>

			{#if !isCancelled(order.status)}
				{@const idx = statusIndex(order.status)}
				<div class="mt-2 w-full min-w-0" role="list" aria-label="Order status progress">
					<div class="flex w-full min-w-0 items-start">
						{#each STATUS_STEPS as step, i (i)}
							{@const active = i <= idx}
							{@const current = i === idx}
							<div
								class="flex min-w-0 flex-1 basis-0 flex-col items-center gap-0.5 px-0.5"
								role="listitem"
							>
								<div
									class="flex h-5 w-5 shrink-0 items-center justify-center rounded-full text-[9px] font-bold transition-colors sm:h-6 sm:w-6 sm:text-[10px] {active
										? 'bg-primary text-primary-foreground'
										: 'bg-muted text-muted-foreground'}"
									aria-current={current ? 'step' : undefined}
								>
									{i + 1}
								</div>
								<p
									title={statusLabel(step)}
									class="w-full text-center text-[7px] leading-tight text-balance wrap-break-word hyphens-auto sm:text-[8px] {current
										? 'font-semibold text-foreground'
										: active
											? 'font-medium text-foreground/85'
											: 'text-muted-foreground'}"
								>
									{statusLabel(step)}
								</p>
							</div>
							{#if i < STATUS_STEPS.length - 1}
								<div
									class="flex min-w-0 flex-1 basis-0 items-center self-stretch pt-2.25 sm:pt-2.75"
									aria-hidden="true"
								>
									<div
										class="h-0.5 w-full min-w-0.5 rounded-full {i < idx
											? 'bg-primary'
											: 'bg-border'}"
									></div>
								</div>
							{/if}
						{/each}
					</div>
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

				{#if order.comment}
					<div class="col-span-2">
						<p class="mb-1 text-xs font-semibold tracking-widest text-muted-foreground uppercase">
							Notes
						</p>
						<p class="text-foreground">{order.comment}</p>
					</div>
				{/if}
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
						<div class="flex h-12 w-12 shrink-0 items-center justify-center rounded-md bg-muted">
							<ShoppingBag class="h-5 w-5 text-primary/40" />
						</div>
					{/if}
					<div class="min-w-0 flex-1">
						<p class="truncate text-sm font-medium text-foreground">{item.productName}</p>
						<p class="text-xs text-muted-foreground">Qty {item.quantity}</p>
					</div>
					<span class="shrink-0 text-sm font-semibold text-foreground"
						>{formatPriceCad(item.lineTotal)}</span
					>
				</a>
			{/each}

			<hr class="border-border" />

			<div class="flex justify-between text-sm text-muted-foreground">
				<span>Subtotal</span>
				<span>{formatPriceCad(order.orderTotal)}</span>
			</div>
			{#if Number(order.orderDiscount) > 0}
				<div class="flex justify-between text-sm text-muted-foreground">
					<span>Discount</span>
					<span>{formatDiscountCad(order.orderDiscount)}</span>
				</div>
			{/if}
			{#if order.orderMethod === 'delivery'}
				{@const fee = order.deliveryFee ?? 0}
				<div class="flex justify-between text-sm text-muted-foreground">
					<span>Delivery fee</span>
					{#if fee === 0}
						<span class="font-medium text-green-600">Free</span>
					{:else}
						<span>{formatPriceCad(fee)}</span>
					{/if}
				</div>
			{/if}
			{#if order.orderTaxAmount}
				<div class="flex justify-between text-sm text-muted-foreground">
					<span>Tax (5%)</span>
					<span>{formatPriceCad(order.orderTaxAmount)}</span>
				</div>
			{/if}
			<hr class="border-border" />
			<div class="flex justify-between font-bold text-foreground">
				<span>Total</span>
				<span>{formatPriceCad(order.orderGrandTotal ?? order.orderTotal)}</span>
			</div>
		</div>

		<div class="mt-8 text-center">
			<a href={resolve('/')} class="text-sm text-primary hover:underline">Back to Home</a>
		</div>
	{/if}
</main>
