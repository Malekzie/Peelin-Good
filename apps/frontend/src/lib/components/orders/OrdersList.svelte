<script>
	/**
	 * Main orders list container with loading, error, and empty states
	 * Composes OrderCard and OrderDetailsPanel components for each order
	 */
	import ProfileSidebar from '$lib/components/profile/ProfileSidebar.svelte';
	import { Skeleton } from '$lib/components/ui/skeleton';
	import { resolve } from '$app/paths';
	import { orderHasAnyReviewableSlot } from '$lib/utils/OrdersHelper';
	import OrderCard from './OrderCard.svelte';
	import OrderDetailsPanel from './OrderDetailsPanel.svelte';

	let {
		orders = [],
		loading = false,
		error = false,
		productImages = {},
		openOrders = new Set(),
		onToggleOrder = () => {},
		onAcceptDelivery = () => {},
		onLeaveReview = () => {}
	} = $props();
</script>

<div
	class="flex h-[calc(100dvh-var(--app-navbar-height))] flex-col overflow-hidden bg-background md:flex-row"
>
	<ProfileSidebar />

	<main class="flex-1 overflow-y-auto p-8 lg:p-10">
		<div class="mx-auto max-w-4xl space-y-6">
			<div>
				<h1 class="text-2xl font-bold tracking-tight text-foreground">Order History</h1>
				<p class="mt-1 text-sm text-muted-foreground">Your past and current orders</p>
			</div>

			{#if loading}
				<div class="space-y-4">
					{#each Array(3) as _, i (i)}
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
							<OrderCard {order} isOpen={openOrders.has(order.id)} onToggle={onToggleOrder} />

							{#if openOrders.has(order.id)}
								<OrderDetailsPanel
									order={{ ...order, hasReviewableSlot: orderHasAnyReviewableSlot(order) }}
									{productImages}
									{onAcceptDelivery}
									{onLeaveReview}
									onToggle={onToggleOrder}
								/>
							{/if}
						</div>
					{/each}
				</div>
			{/if}
		</div>
	</main>
</div>
