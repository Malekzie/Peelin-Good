<script>
	import ProductCard from '$lib/components/ProductCard.svelte';
	import { getProducts } from '$lib/services/products';
	import { getTags } from '$lib/services/tags';
	import { onMount } from 'svelte';

	let activeTagId = $state(null);
	let searchQuery = $state('');
	let cart = $state([]);
	let products = $state([]);
	let tags = $state([]);
	let loading = $state(true);

	// retrieve products and tags from the backend
	onMount(async () => {
		try {
			[products, tags] = await Promise.all([getProducts(), getTags()]);
		} catch (error) {
			console.error('Error fetching products or tags:', error);
		} finally {
			loading = false;
		}
	});

	const filtered = $derived(
		products.filter((p) => {
			const matchesTag = activeTagId === null || p.tagIds?.includes(activeTagId);
			const matchesSearch =
				(p.name ?? '').toLowerCase().includes(searchQuery.toLowerCase()) ||
				(p.description ?? '').toLowerCase().includes(searchQuery.toLowerCase());
			return matchesTag && matchesSearch;
		})
	);

	function handleAddToCart({ name, price, quantity }) {
		const existing = cart.find((item) => item.name === name);
		if (existing) {
			existing.quantity += quantity;
			cart = [...cart];
		} else {
			cart = [...cart, { name, price, quantity }];
		}
	}
</script>

<div class="mx-5 flex gap-10">
	<!-- Sidebar -->
	<div class="mt-21 w-56 shrink-0">
		<div class="border-outline-variant bg-surface sticky top-4 rounded-xl border p-4">
			<p class="text-on-surface-variant mb-3 text-xs font-semibold tracking-wide uppercase">
				Categories
			</p>

			<div class="flex flex-col gap-1">
				<button
					onclick={() => (activeTagId = null)}
					class="rounded-md px-3 py-2 text-left text-sm transition
				{activeTagId === null ? 'bg-primary text-white' : 'hover:bg-surface-container'}"
				>
					All
				</button>

				{#each tags as tag (tag.id)}
					<button
						onclick={() => (activeTagId = tag.id)}
						class="rounded-md px-3 py-2 text-left text-sm transition hover:cursor-pointer hover:bg-[#8e4e14]/10
					{activeTagId === tag.id ? 'bg-primary text-white' : 'hover:bg-surface-container'}"
					>
						{tag.name}
					</button>
				{/each}
			</div>
		</div>
	</div>

	<!-- Main content -->
	<div class="flex-1">
		<!-- Search -->
		<div class="m-6 flex justify-center">
			<input
				type="text"
				placeholder="Search breads, pastries, cakes..."
				bind:value={searchQuery}
				class="bg-surface-container w-full max-w-2xl rounded-full px-4 py-2 text-sm ring-1 ring-border transition outline-none focus:ring-2 focus:ring-primary"
			/>
		</div>

		{#if loading}
			<div class="flex justify-center py-24">
				<div
					class="h-10 w-10 animate-spin rounded-full border-4 border-primary border-t-transparent"
				></div>
			</div>
		{:else}
			<!-- Grid of Products -->
			<div class="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-4">
				{#each filtered as product (product.id)}
					<ProductCard
						name={product.name}
						description={product.description ?? ''}
						price={product.basePrice}
						imageUrl={product.imageUrl ?? ''}
						onAddToCart={handleAddToCart}
					/>
				{/each}
			</div>
		{/if}
	</div>
</div>
