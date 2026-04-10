<script>
	import ProfileSidebar from '$lib/components/profile/ProfileSidebar.svelte';
	import { Skeleton } from '$lib/components/ui/skeleton';
	import { Badge } from '$lib/components/ui/badge';
	import { onMount } from 'svelte';
	import { getMyOrders } from '$lib/services/orders';
	import { getProducts } from '$lib/services/products';
	import { resolve } from '$app/paths';
	import { ChevronDown, ShoppingBag } from '@lucide/svelte';

	let orders = $state([]);
	let productImages = $state({});
	let loading = $state(true);
	let error = $state(null);
	let openOrders = $state(new Set());

	onMount(async () => {
		try {
			const [ordersData, productsData] = await Promise.all([getMyOrders(), getProducts()]);
			orders = ordersData ?? [];
			const map = {};
			for (const p of productsData ?? []) {
				map[p.id] = p.imageUrl ?? null;
			}
			productImages = map;
		} catch {
			error = true;
		} finally {
			loading = false;
		}
	});

	function toggle(orderId) {
		const next = new Set(openOrders);
		if (next.has(orderId)) {
			next.delete(orderId);
		} else {
			next.add(orderId);
		}
		openOrders = next;
	}

	function statusColor(status) {
		switch (status) {
			case 'pending_payment':
				return 'secondary';
			case 'paid':
			case 'preparing':
				return 'outline';
			case 'ready':
				return 'default';
			case 'delivered':
			case 'picked_up':
			case 'completed':
				return 'default';
			case 'cancelled':
				return 'destructive';
			default:
				return 'secondary';
		}
	}

	function formatDate(dateStr) {
		if (!dateStr) return '—';
		return new Date(dateStr).toLocaleDateString('en-CA', {
			year: 'numeric',
			month: 'short',
			day: 'numeric'
		});
	}

	function formatPrice(amount) {
		if (amount == null) return '—';
		return `$${Number(amount).toFixed(2)}`;
	}
</script>

<div class="flex min-h-screen bg-background">
	<ProfileSidebar />

	<main class="flex-1 overflow-y-auto p-8 lg:p-10">
		<div class="mx-auto max-w-4xl space-y-6">
			<div>
				<h1 class="text-2xl font-bold tracking-tight text-foreground">Order History</h1>
				<p class="mt-1 text-sm text-muted-foreground">Your past and current orders</p>
			</div>

			{#if loading}
				<div class="space-y-4">
					{#each Array(3) as _item, i (i)}
						<Skeleton class="h-28 w-full rounded-xl" />
					{/each}
				</div>
			{:else if error}
				<p class="text-sm text-destructive">Failed to load orders. Please try again.</p>
			{:else if orders.length === 0}
				<div class="rounded-xl border border-border bg-card p-10 text-center">
					<p class="text-sm font-medium text-foreground">No orders yet</p>
					<p class="mt-1 text-sm text-muted-foreground">Place your first order to see it here.</p>
					<a
						href={resolve('/menu')}
						class="mt-4 inline-block text-sm font-semibold text-primary hover:underline"
					>
						Browse menu
					</a>
				</div>
			{:else}
				<div class="space-y-4">
					{#each orders as order (order.id)}
						<div class="overflow-hidden rounded-xl border border-border bg-card shadow-sm">
							<!-- Accordion header -->
							<button
								type="button"
								onclick={() => toggle(order.id)}
								class="w-full px-5 py-4 text-left transition-colors hover:bg-muted/40"
							>
								<div class="flex items-center justify-between gap-4">
									<div class="min-w-0 space-y-0.5">
										<p class="text-sm font-semibold text-foreground">
											Order #{order.orderNumber}
										</p>
										<p class="text-xs text-muted-foreground">
											{order.bakeryName ?? "Peelin' Good"} · {formatDate(order.placedAt)}
										</p>
									</div>
									<div class="flex shrink-0 items-center gap-3">
										<div class="flex flex-col items-end gap-1">
											<Badge variant={statusColor(order.status)}>
												{order.status?.replace(/_/g, ' ') ?? '—'}
											</Badge>
											<p class="text-sm font-bold text-foreground">
												{formatPrice(order.orderGrandTotal)}
											</p>
										</div>
										<ChevronDown
											class="h-4 w-4 shrink-0 text-muted-foreground transition-transform duration-200
												{openOrders.has(order.id) ? 'rotate-180' : ''}"
										/>
									</div>
								</div>
							</button>

							<!-- Accordion body -->
							{#if openOrders.has(order.id)}
								<div class="border-t border-border px-5 pb-5 pt-4">
									{#if order.items && order.items.length > 0}
										<div class="mb-4 flex flex-col gap-2">
											{#each order.items as item (item.id)}
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
													<div class="min-w-0">
														<p class="truncate text-sm font-medium text-foreground">{item.productName}</p>
														<p class="text-xs text-muted-foreground">
															Qty {item.quantity} · {formatPrice(item.lineTotal)}
														</p>
													</div>
												</a>
											{/each}
										</div>
									{/if}

									<a
										href={resolve(`/orders/${order.orderNumber}`)}
										class="inline-flex items-center gap-1 text-xs font-semibold text-primary hover:underline"
									>
										View tracking →
									</a>
								</div>
							{/if}
						</div>
					{/each}
				</div>
			{/if}
		</div>
	</main>
</div>
