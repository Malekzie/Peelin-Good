<script>
	import { onMount } from 'svelte';
	import { Skeleton } from '$lib/components/ui/skeleton';
	import { Badge } from '$lib/components/ui/badge';
	import { Button } from '$lib/components/ui/button';
	import { getDashboardSummary } from '$lib/services/dashboard';
	import { updateOrderStatus } from '$lib/services/staff-orders';
	import { formatPriceCad } from '$lib/utils/money';

	let orders = $state([]);
	let loading = $state(true);
	let error = $state(null);
	let updating = $state({});

	onMount(async () => {
		try {
			const summary = await getDashboardSummary();
			orders = summary.recentOrders ?? [];
		} catch {
			error = true;
		} finally {
			loading = false;
		}
	});

	const LINEAR_FLOW = {
		placed: 'pending_payment',
		pending_payment: 'paid',
		paid: 'preparing',
		preparing: 'ready'
	};

	/** After ready/scheduled, staff use Delivered (delivery) or Picked up (pickup)—not interchangeable. */
	function nextStatus(order) {
		const current = order.status;
		if (!current) return null;
		const method = (order.orderMethod ?? '').toLowerCase();
		if (current === 'ready' || current === 'scheduled') {
			return method === 'delivery' ? 'delivered' : 'picked_up';
		}
		return LINEAR_FLOW[current] ?? null;
	}

	function nextLabel(order) {
		const next = nextStatus(order);
		if (!next) return null;
		return next.replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase());
	}

	async function advance(order) {
		const next = nextStatus(order);
		if (!next) return;
		updating[order.id] = true;
		try {
			const updated = await updateOrderStatus(order.id, next);
			orders = orders.map((o) => (o.id === order.id ? updated : o));
		} catch {
			// badge stays unchanged on failure
		} finally {
			updating[order.id] = false;
		}
	}

	function statusVariant(status) {
		if (['completed', 'delivered', 'picked_up'].includes(status)) return 'default';
		if (status === 'cancelled') return 'destructive';
		if (status === 'ready') return 'default';
		return 'secondary';
	}

	function formatDate(dt) {
		if (!dt) return '—';
		return new Date(dt).toLocaleString('en-CA', {
			month: 'short',
			day: 'numeric',
			hour: '2-digit',
			minute: '2-digit'
		});
	}

	function formatCurrency(val) {
		if (val == null) return '—';
		return formatPriceCad(val);
	}
</script>

<main class="flex-1 overflow-y-auto p-8 lg:p-10">
	<div class="mx-auto max-w-5xl space-y-6">
		<div>
			<h1 class="text-2xl font-bold tracking-tight text-foreground">Orders</h1>
			<p class="mt-1 text-sm text-muted-foreground">
				Recent orders. Update status as work progresses.
			</p>
		</div>

		{#if loading}
			<div class="space-y-3">
				{#each Array(5) as _, i (i)}
					<Skeleton class="h-20 rounded-xl" />
				{/each}
			</div>
		{:else if error}
			<p class="text-sm text-destructive">Failed to load orders.</p>
		{:else if orders.length === 0}
			<div class="rounded-xl border border-border bg-card p-10 text-center">
				<p class="text-sm text-muted-foreground">No recent orders</p>
			</div>
		{:else}
			<div class="space-y-3">
				{#each orders as order (order.id)}
					<div
						class="flex flex-col gap-3 rounded-xl border border-border bg-card px-5 py-4 sm:flex-row sm:items-center sm:justify-between"
					>
						<div class="space-y-0.5">
							<p class="text-sm font-semibold text-foreground">#{order.orderNumber}</p>
							<p class="text-xs text-muted-foreground">
								{order.bakeryName ?? "Peelin' Good"} · {order.orderMethod} · {formatDate(
									order.placedAt
								)}
							</p>
							<p class="text-xs font-medium text-foreground">
								{formatCurrency(order.orderGrandTotal)}
							</p>
						</div>
						<div class="flex shrink-0 items-center gap-3">
							<Badge variant={statusVariant(order.status)}>
								{order.status?.replace(/_/g, ' ') ?? '—'}
							</Badge>
							{#if nextStatus(order)}
								<Button
									size="sm"
									variant="outline"
									onclick={() => advance(order)}
									disabled={!!updating[order.id]}
								>
									{updating[order.id] ? 'Updating...' : `Mark ${nextLabel(order)}`}
								</Button>
							{/if}
						</div>
					</div>
				{/each}
			</div>
		{/if}
	</div>
</main>
