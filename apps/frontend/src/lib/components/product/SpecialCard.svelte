<script>
	import { ShoppingCart } from '@lucide/svelte';
	import { cart } from '$lib/stores/cart';

	let { name, description, price, discountPercent, imageUrl, productId } = $props();

	const discountedPrice = discountPercent ? price * (1 - discountPercent / 100) : null;
	const finalPrice = discountedPrice ?? price;

	let added = $state(false);

	function addToCart() {
		cart.addItem(
			{
				id: productId,
				name,
				description,
				basePrice: finalPrice,
				imageUrl
			},
			1
		);
		added = true;
		setTimeout(() => (added = false), 1400);
	}
</script>

<article
	class="group relative flex flex-col overflow-hidden rounded-2xl border border-border bg-card transition-all duration-200 hover:border-primary/30 hover:shadow-lg"
>
	<div class="relative h-44 overflow-hidden bg-[oklch(0.94_0.03_70)]">
		{#if imageUrl}
			<img src={imageUrl} alt={name} class="h-full w-full object-cover" />
		{:else}
			<div class="h-full w-full bg-[oklch(0.94_0.03_70)]"></div>
		{/if}
		{#if discountPercent}
			<span
				class="absolute top-3 left-3 rounded-full bg-primary px-3 py-1 text-xs font-semibold text-primary-foreground"
			>
				{discountPercent}% off
			</span>
		{/if}
	</div>

	<div class="flex flex-1 flex-col gap-2 p-4">
		<h3 class="font-serif text-lg font-semibold text-foreground">{name}</h3>
		<p class="flex-1 text-sm leading-snug text-muted-foreground">{description}</p>

		<div class="mt-2 flex items-center justify-between border-t border-border pt-3">
			<div class="flex items-baseline gap-2">
				{#if discountedPrice}
					<span class="text-base font-bold text-foreground">${discountedPrice.toFixed(2)}</span>
					<span class="text-xs text-muted-foreground line-through">${price.toFixed(2)}</span>
				{:else}
					<span class="text-base font-bold text-foreground">${price.toFixed(2)}</span>
				{/if}
			</div>
			<button
				onclick={addToCart}
				class="flex items-center gap-1.5 rounded-full px-4 py-2 text-xs font-semibold text-primary-foreground transition-all duration-300
					{added ? 'bg-[#8A9E7F]' : 'bg-primary hover:opacity-90'}"
			>
				<ShoppingCart size={13} />
				{added ? 'Added!' : 'Add to Order'}
			</button>
		</div>
	</div>
</article>
