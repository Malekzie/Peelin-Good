<script lang="ts">
	import { formatPriceCad } from '$lib/utils/money';
	import type { CustomerProfile } from '$lib/services/checkout';

	interface CartItem {
		productId: number;
		productName: string;
		quantity: number;
		lineTotal: number;
	}

	interface Props {
		items: CartItem[];
		subtotal: number;
		deliveryFee: number;
		orderMethod: 'pickup' | 'delivery';
		customer: CustomerProfile | null;
	}

	let { items, subtotal, deliveryFee, orderMethod, customer }: Props = $props();

	const taxAmount = $derived(subtotal * 0.05);
	const estimatedTotal = $derived(subtotal + deliveryFee + taxAmount);
</script>

<section class="rounded-xl border border-border bg-card p-6 shadow-sm">
	<h2 class="mb-4 text-lg font-semibold text-foreground">Summary</h2>
	{#if customer?.employeeDiscountEligible}
		<div
			class="mb-4 rounded-lg border border-primary/30 bg-primary/5 px-3 py-2 text-sm font-medium text-primary"
		>
			20% employee discount applies to your order after today's specials and your loyalty tier
			discount. Final amounts are calculated when you place the order.
		</div>
	{/if}
	{#each items as item (item.productId)}
		<div class="flex justify-between py-1 text-sm text-muted-foreground">
			<span>{item.productName} × {item.quantity}</span>
			<span>{formatPriceCad(item.lineTotal)}</span>
		</div>
	{/each}
	<hr class="my-3 border-border" />
	<p class="text-xs text-muted-foreground">
		Line totals use menu prices. Today's specials, loyalty tier, tax, and any employee discount are
		applied on the server when you confirm.
	</p>
	<div class="mt-3 flex justify-between text-sm text-muted-foreground">
		<span>Subtotal</span>
		<span>{formatPriceCad(subtotal)}</span>
	</div>
	{#if orderMethod === 'delivery'}
		<div class="mt-1 flex justify-between text-sm text-muted-foreground">
			<span>Delivery fee</span>
			{#if deliveryFee === 0}
				<span class="font-medium text-green-600">Free</span>
			{:else}
				<span>{formatPriceCad(deliveryFee)}</span>
			{/if}
		</div>
		{#if deliveryFee > 0}
			<p class="mt-0.5 text-xs text-muted-foreground">
				Free delivery on orders of {formatPriceCad(50)} or more
			</p>
		{/if}
	{/if}
	<div class="mt-1 flex justify-between text-sm text-muted-foreground">
		<span>Est. tax (5%)</span>
		<span>{formatPriceCad(taxAmount)}</span>
	</div>
	<hr class="my-3 border-border" />
	<div class="flex justify-between text-sm font-medium text-foreground">
		<span>Est. total</span>
		<span>{formatPriceCad(estimatedTotal)}</span>
	</div>
	<p class="mt-2 text-xs text-muted-foreground">* Final discounts applied at payment</p>
</section>
