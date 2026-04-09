<script>
	import { onMount } from 'svelte';
	import { resolve } from '$app/paths';
	import SpecialCard from '$lib/components/product/SpecialCard.svelte';
	import { getAllSpecials } from '$lib/services/product-specials';

	let specials = [];
	let loading = true;
	let error = false;

	onMount(async () => {
		try {
			specials = await getAllSpecials();
		} catch {
			error = true;
		} finally {
			loading = false;
		}
	});
</script>

<section class="bg-[#F5EFE6] px-6 py-16">
	<div class="mx-auto max-w-7xl">
		<p class="mb-1 text-[11px] font-semibold tracking-[0.2em] text-[#C4714A] uppercase">
			Out of the oven
		</p>
		<h2 class="mb-2 text-3xl font-black tracking-tight text-[#2C1A0E]">Today's specials</h2>
		<p class="mb-8 text-sm text-muted-foreground">A curated selection of what's fresh right now.</p>

		{#if loading}
			<p class="text-sm text-muted-foreground">Loading specials...</p>
		{:else if error}
			<p class="text-sm text-red-500">Could not load specials. Try again later.</p>
		{:else if specials.length === 0}
			<p class="text-sm text-muted-foreground">No specials available right now.</p>
		{:else}
			<div class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
				{#each specials as special (special.productSpecialId)}
					<SpecialCard
						name={special.productName}
						description={special.productDescription}
						price={special.productBasePrice}
						discountPercent={special.discountPercent}
						imageUrl={special.productImageUrl}
						productId={special.productId}
					/>
				{/each}
			</div>
		{/if}
	</div>
</section>
