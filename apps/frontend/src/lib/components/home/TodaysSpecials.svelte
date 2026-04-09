<script>
	import { onMount } from 'svelte';
	import SpecialCard from '$lib/components/product/SpecialCard.svelte';
	import { getTodaySpecial } from '$lib/services/product-specials';
	import { getProductById } from '$lib/services/products';

	let specials = $state([]);
	let loading = $state(true);

	function localDateIso() {
		const d = new Date();
		const y = d.getFullYear();
		const m = String(d.getMonth() + 1).padStart(2, '0');
		const day = String(d.getDate()).padStart(2, '0');
		return `${y}-${m}-${day}`;
	}

	onMount(async () => {
		try {
			const today = await getTodaySpecial(localDateIso());
			const pid = today?.productId;
			if (pid == null) {
				specials = [];
				return;
			}
			const product = await getProductById(pid);
			const pct = today.discountPercent != null ? Number(today.discountPercent) : null;
			specials = [
				{
					productSpecialId: pid,
					productId: product.id,
					productName: product.name,
					productDescription: product.description,
					productBasePrice: product.basePrice,
					discountPercent: pct,
					productImageUrl: product.imageUrl
				}
			];
		} catch {
			specials = [];
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
		<h2 class="mb-2 text-3xl font-black tracking-tight text-[#2C1A0E]">Today's special</h2>
		<p class="mb-8 text-sm text-muted-foreground">Our featured product for today's date.</p>

		{#if loading}
			<p class="text-sm text-muted-foreground">Loading today's special…</p>
		{:else if specials.length > 0}
			<div class="mx-auto grid max-w-lg grid-cols-1 gap-4">
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
		{:else}
			<p class="max-w-lg text-sm leading-relaxed text-muted-foreground">
				No special is available today. Check back another day!
			</p>
		{/if}
	</div>
</section>
