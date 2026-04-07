<script lang="ts">
	import { resolve } from '$app/paths';
	import { page } from '$app/state';

	const API = 'http://localhost:8080';

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
		productName: string;
		quantity: number;
		lineTotal: number;
	}

	interface Order {
		orderNumber: string;
		orderStatus: string;
		orderMethod: string;
		orderPlacedDatetime: string;
		subtotal: number;
		discount: number;
		total: number;
		paymentStatus: string;
		items: OrderItem[];
	}

	let order = $state<Order | null>(null);
	let loading = $state(true);
	let error = $state('');

	const orderNumber = page.params.orderNumber;

	async function fetchOrder() {
		loading = true;
		error = '';
		try {
			const res = await fetch(`${API}/api/v1/orders/${orderNumber}`);
			if (!res.ok) throw new Error(`Order not found (${res.status})`);
			order = await res.json();
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
				<span
					class="rounded-full bg-muted px-3 py-1 text-xs font-semibold text-foreground capitalize"
				>
					{statusLabel(order.orderStatus)}
				</span>
			</div>

			{#if !isCancelled(order.orderStatus)}
				<div class="flex items-center gap-0 overflow-x-auto pb-2">
					{#each STATUS_STEPS as step, i (i)}
						{@const active = i <= statusIndex(order.orderStatus)}
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
								class="mb-5 h-0.5 min-w-4 flex-1 transition-colors {i <
								statusIndex(order.orderStatus)
									? 'bg-primary'
									: 'bg-border'}"
							></div>
						{/if}
					{/each}
				</div>
			{:else}
				<p class="text-sm font-semibold text-destructive capitalize">
					{statusLabel(order.orderStatus)}
				</p>
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
				<div>
					<p class="mb-1 text-xs font-semibold tracking-widest text-muted-foreground uppercase">
						Payment
					</p>
					<p class="text-foreground capitalize">{order.paymentStatus}</p>
				</div>
				<div class="col-span-2">
					<p class="mb-1 text-xs font-semibold tracking-widest text-muted-foreground uppercase">
						Placed
					</p>
					<p class="text-foreground">{new Date(order.orderPlacedDatetime).toLocaleString()}</p>
				</div>
			</div>

			<hr class="border-border" />

			{#each order.items as item (item.productName)}
				<div class="flex justify-between text-sm">
					<span class="text-muted-foreground">{item.productName} × {item.quantity}</span>
					<span class="text-foreground">${item.lineTotal.toFixed(2)}</span>
				</div>
			{/each}

			<hr class="border-border" />

			{#if order.discount > 0}
				<div class="flex justify-between text-sm text-accent">
					<span>Discount</span>
					<span>−${order.discount.toFixed(2)}</span>
				</div>
			{/if}
			<div class="flex justify-between font-bold text-foreground">
				<span>Total</span>
				<span>${order.total.toFixed(2)}</span>
			</div>
		</div>

		<div class="mt-8 text-center">
			<a href={resolve('/')} class="text-sm text-primary hover:underline">Back to Home</a>
		</div>
	{/if}
</main>
