<script>
	import { cart } from '$lib/stores/cart';
	import { Card, CardContent } from '$lib/components/ui/card';
	import { Button } from '$lib/components/ui/button';
	import { Plus, Minus, ShoppingBag, Check } from '@lucide/svelte';

	let { product, onselect = () => {} } = $props();

	let quantity = $state(1);
	let added = $state(false);

	const price = $derived(
		typeof product.basePrice === 'number'
			? `$${product.basePrice.toFixed(2)}`
			: `$${parseFloat(product.basePrice).toFixed(2)}`
	);

	function addToCart(e) {
		e.stopPropagation();
		cart.addItem(product, quantity);
		added = true;
		quantity = 1;
		setTimeout(() => (added = false), 1400);
	}

	function stepperClick(e) {
		e.stopPropagation();
	}
</script>

<Card
	role="button"
	tabindex="0"
	onclick={() => onselect(product)}
	onkeydown={(e) => e.key === 'Enter' && onselect(product)}
	class="group flex cursor-pointer flex-col overflow-hidden border-border bg-white pt-0 pb-0.5 transition-all duration-300 hover:-translate-y-0.5 hover:shadow-md"
>
	<!-- Image -->
	<div class="relative h-48 shrink-0 overflow-hidden bg-[#F5EFE6]">
		{#if product.imageUrl}
			<img
				src={product.imageUrl}
				alt={product.name}
				class="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
			/>
		{:else}
			<div class="flex h-full w-full items-center justify-center">
				<ShoppingBag class="h-10 w-10 text-[#C25F1A]/30" />
			</div>
		{/if}
	</div>

	<!-- Content -->
	<CardContent class="flex flex-1 flex-col gap-3 p-4">
		<div class="flex-1">
			<h2 class="text-sm leading-snug font-bold text-[#2C1A0E]">{product.name}</h2>
			{#if product.description}
				<p class="mt-1 line-clamp-2 text-xs text-muted-foreground">{product.description}</p>
			{/if}
		</div>

		<p class="text-lg font-bold text-[#C25F1A]">{price}</p>

		<!-- Stepper + Add — clicks here don't open the sheet -->
		<!-- svelte-ignore a11y_click_events_have_key_events -->
		<!-- svelte-ignore a11y_no_static_element_interactions -->
		<div class="flex items-center gap-2" onclick={stepperClick}>
			<div class="flex items-center rounded-full border border-border bg-background">
				<button
					onclick={(e) => {
						e.stopPropagation();
						if (quantity > 1) quantity -= 1;
					}}
					class="flex h-7 w-7 items-center justify-center rounded-full transition-colors hover:bg-muted"
					aria-label="Decrease"
				>
					<Minus class="h-3 w-3" />
				</button>
				<span class="w-6 text-center text-xs font-semibold">{quantity}</span>
				<button
					onclick={(e) => {
						e.stopPropagation();
						quantity += 1;
					}}
					class="flex h-7 w-7 items-center justify-center rounded-full transition-colors hover:bg-muted"
					aria-label="Increase"
				>
					<Plus class="h-3 w-3" />
				</button>
			</div>

			<Button
				onclick={addToCart}
				class="flex-1 gap-1.5 text-xs transition-all duration-300
					{added ? 'bg-[#8A9E7F] hover:bg-[#8A9E7F]' : 'bg-[#C25F1A] hover:bg-[#C25F1A]/90'}"
			>
				{#if added}
					<Check class="h-3.5 w-3.5" />
					Added
				{:else}
					<ShoppingBag class="h-3.5 w-3.5" />
					Add
				{/if}
			</Button>
		</div>
	</CardContent>
</Card>
