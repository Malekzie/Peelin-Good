<script>
	/**
	 * Expanded accordion panel showing order details and items
	 * Displays product list, tracking link, and action buttons
	 */
	import { ShoppingBag } from '@lucide/svelte';
	import { resolve } from '$app/paths';
	import { formatPrice } from '$lib/utils/OrdersHelper';

	let { order = null, productImages = {}, onAcceptDelivery = () => {}, onLeaveReview = () => {}, onToggle = () => {} } =
		$props();

</script>

{#if order}
	<div class="border-t border-border px-5 pt-4 pb-5">
		{#if order.items && order.items.length > 0}
			<div class="mb-4 flex flex-col gap-2">
				{#each order.items as item (item.productId ?? item.id)}
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
						<div class="min-w-0">
							<p class="truncate text-sm font-medium text-foreground">
								{item.productName}
							</p>
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

		{#if ['delivered', 'picked_up'].includes(order.status)}
			<div class="mt-4 border-t border-border pt-4">
				<button
					onclick={() => onAcceptDelivery(order)}
					class="rounded-full bg-primary px-6 py-2 text-xs font-semibold text-primary-foreground hover:opacity-90"
				>
					Accept Delivery
				</button>
			</div>
		{/if}

		{#if order.hasReviewableSlot}
			<div class="mt-4 flex items-center gap-3 border-t border-border pt-4">
				<button
					onclick={() => onLeaveReview(order)}
					class="rounded-full border border-border px-4 py-2 text-sm font-semibold text-foreground hover:bg-muted"
				>
					Leave a Review
				</button>
				<button
					onclick={() => onToggle(order.id)}
					class="rounded-full bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground hover:bg-primary/90"
				>
					Done
				</button>
			</div>
		{/if}
	</div>
{/if}
