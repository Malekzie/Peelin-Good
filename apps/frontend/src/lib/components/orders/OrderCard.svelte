<script>
	/**
	 * Individual order card in collapsed state
	 * Shows order number, bakery, date, status badge, and total price
	 */
	import { Badge } from '$lib/components/ui/badge';
	import { ChevronDown } from '@lucide/svelte';
	import { formatDate, formatPrice, statusColor } from '$lib/utils/OrdersHelper';

	let { order = null, isOpen = false, onToggle = () => {}, onRetryPayment = () => {} } = $props();
</script>

{#if order}
	<button
		type="button"
		onclick={() => onToggle(order.id)}
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
						{isOpen ? 'rotate-180' : ''}"
				/>
			</div>
		</div>
	</button>
	{#if order.status === 'placed' || order.status === 'pending_payment'}
		<div
			class="flex items-center justify-between border-t border-amber-200 bg-amber-50 px-5 py-3 text-sm text-amber-800"
		>
			<span>Payment incomplete.</span>
			<button
				type="button"
				onclick={() => onRetryPayment(order)}
				class="rounded-full bg-amber-500 px-4 py-1.5 text-xs font-semibold text-white hover:bg-amber-600"
			>
				Complete Payment
			</button>
		</div>
	{/if}
{/if}
