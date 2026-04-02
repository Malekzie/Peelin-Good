<script>
	let {
		name = '',
		description = '',
		price = 0,
		imageUrl = '',
		badge = '',
		badgeColor = 'default',
		onAddToCart = () => {}
	} = $props();

	let quantity = $state(1);

	const badgeClass = $derived(
		badgeColor === 'tertiary'
			? 'bg-surface/90 text-tertiary'
			: badgeColor === 'muted'
				? 'bg-surface/90 text-on-surface'
				: 'bg-surface/90 text-primary'
	);

	const formattedPrice = $derived(
		typeof price === 'number' ? `$${price.toFixed(2)}` : `$${parseFloat(price).toFixed(2)}`
	);
</script>

<div
	class="group bg-surface-container-lowest flex flex-col overflow-hidden rounded-xl shadow-sm transition-shadow duration-300 hover:shadow-lg"
>
	<!-- Image -->
	<div class="relative h-40 shrink-0 overflow-hidden">
		{#if imageUrl}
			<img
				src={imageUrl}
				alt={name}
				class="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
			/>
		{:else}
			<div class="bg-surface-container flex h-full w-full items-center justify-center">
				image not found
			</div>
		{/if}

		{#if badge}
			<div class="absolute top-2 right-2">
				<span
					class="rounded-full px-2 py-0.5 text-xs font-bold tracking-wider uppercase backdrop-blur-sm {badgeClass}"
				>
					{badge}
				</span>
			</div>
		{/if}
	</div>

	<!-- Content -->
	<div class="flex flex-1 flex-col p-4">
		<h2 class="font-headline mb-1 text-sm leading-tight font-bold text-primary">{name}</h2>
		<p class="text-on-surface-variant mb-3 line-clamp-2 flex-1 text-xs">{description}</p>

		<!-- Price + Stepper -->
		<div class="mb-3 flex items-center justify-between">
			<span class="font-headline text-lg font-bold text-secondary">{formattedPrice}</span>
			<div class="bg-surface-container flex items-center rounded-full p-0.5">
				<button
					onclick={() => {
						if (quantity > 1) quantity -= 1;
					}}
					class="flex h-6 w-6 items-center justify-center rounded-full transition-colors hover:cursor-pointer hover:bg-white"
					aria-label="Decrease quantity"
				>
					-
				</button>
				<span class="px-2 text-xs font-bold">{quantity}</span>
				<button
					onclick={() => (quantity += 1)}
					class="flex h-6 w-6 items-center justify-center rounded-full transition-colors hover:cursor-pointer hover:bg-white"
					aria-label="Increase quantity"
				>
					+
				</button>
			</div>
		</div>

		<!-- Add to Cart -->
		<button
			onclick={() => onAddToCart({ name, price, quantity })}
			class="hover:bg-primary-container flex w-full items-center justify-center gap-1 rounded-full bg-primary py-2 text-xs font-bold text-white transition-colors hover:cursor-pointer hover:bg-primary/90"
		>
			Add to Cart
		</button>
	</div>
</div>
