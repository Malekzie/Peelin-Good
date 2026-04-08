<script>
	import ProfileSidebar from '$lib/components/profile/ProfileSidebar.svelte';
	import { Skeleton } from '$lib/components/ui/skeleton';
	import { Badge } from '$lib/components/ui/badge';
	import { onMount } from 'svelte';
	import { getMyOrders } from '$lib/services/orders';
	import { resolve } from '$app/paths';

	let orders = $state([]);
	let loading = $state(true);
	let error = $state(null);

	onMount(async () => {
		try {
			orders = await getMyOrders();
		} catch {
			error = true;
		} finally {
			loading = false;
		}
	});

	function statusColor(status) {
		switch (status) {
			case 'pending':
				return 'secondary';
			case 'confirmed':
				return 'outline';
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
						<div class="rounded-xl border border-border bg-card p-5 shadow-sm">
							<div class="flex items-start justify-between gap-4">
								<div class="space-y-1">
									<p class="text-sm font-semibold text-foreground">
										Order #{order.orderNumber}
									</p>
									<p class="text-xs text-muted-foreground">
										{order.bakeryName ?? "Peelin' Good"} · {formatDate(order.placedAt)}
									</p>
									{#if order.items && order.items.length > 0}
										<p class="text-xs text-muted-foreground">
											{order.items.map((i) => i.productName).join(', ')}
										</p>
									{/if}
								</div>
								<div class="flex shrink-0 flex-col items-end gap-2">
									<Badge variant={statusColor(order.status)}>
										{order.status?.replace('_', ' ') ?? '—'}
									</Badge>
									<p class="text-sm font-bold text-foreground">
										{formatPrice(order.orderGrandTotal)}
									</p>
								</div>
							</div>
						</div>
					{/each}
				</div>
			{/if}
		</div>
	</main>
</div>
