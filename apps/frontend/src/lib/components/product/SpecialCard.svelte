<script>
	import { cart } from '$lib/stores/cart';
	import { Card, CardContent } from '$lib/components/ui/card';
	import { Button } from '$lib/components/ui/button';
	import { Plus, Minus, ShoppingBag, Check } from '@lucide/svelte';
	import { formatPriceCad } from '$lib/utils/money';

	let { name, description, price, discountPercent, imageUrl, productId, onselect = () => {} } =
		$props();

	let quantity = $state(1);
	let added = $state(false);

	const discountedPrice = $derived(discountPercent ? price * (1 - discountPercent / 100) : null);
	const finalPrice = $derived(discountedPrice ?? price);
	const formattedFinal = $derived(formatPriceCad(finalPrice));
	const formattedOriginal = $derived(discountedPrice ? formatPriceCad(price) : null);

	function addToCart(e) {
		e.stopPropagation();
		cart.addItem({
			productId,
			productName: name,
			productImageUrl: imageUrl ?? null,
			unitPrice: finalPrice,
			quantity
		});
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
	onclick={onselect}
	onkeydown={(e) => e.key === 'Enter' && onselect()}
	class="group flex cursor-pointer flex-col overflow-hidden border-border bg-card pt-0 pb-0.5 transition-all duration-300 hover:-translate-y-0.5 hover:shadow-md"
>
	<!-- Image -->
	<div class="relative h-32 shrink-0 overflow-hidden bg-muted sm:h-48">
		{#if imageUrl}
			<img
				src={imageUrl}
				alt={name}
				class="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
			/>
		{:else}
			<div class="flex h-full w-full items-center justify-center">
				<ShoppingBag class="h-10 w-10 text-primary/30" />
			</div>
		{/if}
		{#if discountPercent}
			<span
				class="absolute top-3 left-3 rounded-full bg-primary px-3 py-1 text-xs font-semibold text-primary-foreground"
			>
				{discountPercent}% off
			</span>
		{/if}
	</div>

	<!-- Content -->
	<CardContent class="flex flex-1 flex-col gap-3 p-4">
		<div class="flex-1">
			<h2 class="text-sm leading-snug font-bold text-foreground">{name}</h2>
			{#if description}
				<p class="mt-1 line-clamp-2 hidden text-xs text-muted-foreground sm:block">
					{description}
				</p>
			{/if}
		</div>

		<div class="flex items-baseline gap-2">
			<p class="text-lg font-bold text-primary">{formattedFinal}</p>
			{#if formattedOriginal}
				<p class="text-xs text-muted-foreground line-through">{formattedOriginal}</p>
			{/if}
		</div>

		<!-- Stepper + Add — clicks here don't open the sheet -->
		<!-- svelte-ignore a11y_click_events_have_key_events -->
		<!-- svelte-ignore a11y_no_static_element_interactions -->
		<div class="flex items-center gap-2" onclick={stepperClick}>
			<div class="hidden items-center rounded-full border border-border bg-background sm:flex">
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
					{added ? 'bg-emerald-600 hover:bg-emerald-600' : 'bg-primary hover:bg-primary/90'}"
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
