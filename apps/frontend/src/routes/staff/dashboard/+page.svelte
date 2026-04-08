<script>
	import { onMount } from 'svelte';
	import { Skeleton } from '$lib/components/ui/skeleton';
	import { Badge } from '$lib/components/ui/badge';
	import KpiCard from '$lib/components/staff/KpiCard.svelte';
	import { getDashboardSummary } from '$lib/services/dashboard.js';

	let summary = $state(null);
	let loading = $state(true);
	let error = $state(null);

	onMount(async () => {
		try {
			summary = await getDashboardSummary();
		} catch {
			error = true;
		} finally {
			loading = false;
		}
	});

	function statusVariant(status) {
		if (!status) return 'secondary';
		if (['completed', 'delivered', 'picked_up'].includes(status)) return 'default';
		if (status === 'cancelled') return 'destructive';
		if (status === 'ready') return 'default';
		return 'secondary';
	}

	function formatCurrency(val) {
		if (val == null) return '—';
		return `$${Number(val).toFixed(2)}`;
	}

	function formatDate(dt) {
		if (!dt) return '—';
		return new Date(dt).toLocaleDateString('en-CA', {
			month: 'short',
			day: 'numeric',
			year: 'numeric'
		});
	}
</script>

<main class="flex-1 overflow-y-auto p-8 lg:p-10">
	<div class="mx-auto max-w-5xl space-y-8">
		<div>
			<h1 class="text-2xl font-bold tracking-tight text-foreground">Dashboard</h1>
			<p class="mt-1 text-sm text-muted-foreground">Overview of bakery operations</p>
		</div>

		{#if loading}
			<div class="grid grid-cols-2 gap-4 lg:grid-cols-4">
				{#each Array(4) as _, i (i)}
					<Skeleton class="h-28 rounded-xl" />
				{/each}
			</div>
			<Skeleton class="h-64 rounded-xl" />
		{:else if error}
			<p class="text-sm text-destructive">Failed to load dashboard. Please try again.</p>
		{:else}
			<div class="grid grid-cols-2 gap-4 lg:grid-cols-4">
				<KpiCard label="Total Revenue" value={formatCurrency(summary.totalRevenue)} />
				<KpiCard label="Total Orders" value={summary.totalOrders} />
				<KpiCard label="Customers" value={summary.totalCustomers} />
				<KpiCard label="Products" value={summary.totalProducts} />
			</div>

			<div class="rounded-xl border border-border bg-card shadow-sm">
				<div class="border-b border-border px-6 py-4">
					<h2 class="text-sm font-semibold text-foreground">Recent Orders</h2>
				</div>
				<div class="divide-y divide-border">
					{#if !summary.recentOrders?.length}
						<p class="px-6 py-8 text-center text-sm text-muted-foreground">No recent orders</p>
					{:else}
						{#each summary.recentOrders as order (order.id)}
							<div class="flex items-center justify-between px-6 py-4">
								<div class="space-y-0.5">
									<p class="text-sm font-semibold text-foreground">#{order.orderNumber}</p>
									<p class="text-xs text-muted-foreground">
										{order.bakeryName ?? "Peelin' Good"} · {formatDate(order.placedAt)}
									</p>
								</div>
								<div class="flex items-center gap-3">
									<Badge variant={statusVariant(order.status)}>
										{order.status?.replace(/_/g, ' ') ?? '—'}
									</Badge>
									<p class="text-sm font-bold text-foreground">
										{formatCurrency(order.orderGrandTotal)}
									</p>
								</div>
							</div>
						{/each}
					{/if}
				</div>
			</div>
		{/if}
	</div>
</main>
