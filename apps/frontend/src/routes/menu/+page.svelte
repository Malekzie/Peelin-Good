<script>
	import ProductCard from '$lib/components/ProductCard.svelte';

	// temp tags and products until backend is set up
	let tags = [
		{ id: 1, tagName: 'Breads' },
		{ id: 2, tagName: 'Pastries' },
		{ id: 3, tagName: 'Cakes' },
		{ id: 4, tagName: 'Savory' },
		{ id: 5, tagName: 'Vegan' }
	];

	// temp products until backend is set up
	let products = [
		{
			id: 1,
			productName: 'Heritage Wheat Sourdough',
			productDescription:
				'Naturally leavened for 48 hours using our 100-year-old starter for a deep, complex tang and airy crumb.',
			productBasePrice: '12.50',
			productImageUrl: 'https://images.unsplash.com/photo-1586444248902-2f64eddc13df?w=400&q=80',
			tags: [{ id: 1 }]
		},
		{
			id: 2,
			productName: 'Double-Butter Croissant',
			productDescription:
				'Laminated by hand with grass-fed cultured butter for unparalleled flakiness and honeycomb internal structure.',
			productBasePrice: '5.75',
			productImageUrl: 'https://images.unsplash.com/photo-1555507036-ab1f4038808a?w=400&q=80',
			tags: [{ id: 2 }]
		},
		{
			id: 3,
			productName: 'Dark Ganache Truffle Cake',
			productDescription:
				'72% Valrhona chocolate infused with espresso, creating a silk-textured finish that melts away.',
			productBasePrice: '42.00',
			productImageUrl: 'https://images.unsplash.com/photo-1578985545062-69928b1d9587?w=400&q=80',
			tags: [{ id: 3 }]
		},
		{
			id: 4,
			productName: 'Herbed Garden Focaccia',
			productDescription:
				'Dimpled dough drizzled with extra virgin olive oil, topped with sea salt, rosemary, and seasonal roasted vegetables.',
			productBasePrice: '8.95',
			productImageUrl: 'https://images.unsplash.com/photo-1534620808146-d33bb39128b2?w=400&q=80',
			tags: [{ id: 4 }, { id: 5 }]
		},
		{
			id: 5,
			productName: 'Wild Berry Custard Tart',
			productDescription:
				'Crisp shortcrust pastry filled with vanilla bean pastry cream and finished with local seasonal berries.',
			productBasePrice: '6.50',
			productImageUrl: 'https://images.unsplash.com/photo-1519915028121-7d3463d20b13?w=400&q=80',
			tags: [{ id: 2 }]
		},
		{
			id: 6,
			productName: 'Nordic Rye & Sunflower',
			productDescription:
				'Dense, nutrient-rich rye loaf packed with toasted sunflower and pumpkin seeds for a nutty, earthy profile.',
			productBasePrice: '10.25',
			productImageUrl: 'https://images.unsplash.com/photo-1509440159596-0249088772ff?w=400&q=80',
			tags: [{ id: 1 }]
		},
		{
			id: 7,
			productName: 'Almond Morning Bun',
			productDescription:
				'Brioche-based bun filled with almond frangipane and finished with a pearl sugar crust.',
			productBasePrice: '4.50',
			productImageUrl: 'https://images.unsplash.com/photo-1558961363-fa8fdf82db35?w=400&q=80',
			tags: [{ id: 2 }]
		},
		{
			id: 8,
			productName: 'Lemon Olive Oil Cake',
			productDescription:
				'Moist, tender crumb with bright citrus notes from fresh-zested lemons and cold-pressed olive oil.',
			productBasePrice: '32.00',
			productImageUrl: 'https://images.unsplash.com/photo-1621303837174-89787a7d4729?w=400&q=80',
			tags: [{ id: 3 }, { id: 5 }]
		},
		{
			id: 9,
			productName: 'Rosemary Sea Salt Crackers',
			productDescription:
				'Thin, crisp crackers with whole rosemary sprigs and flaky Maldon sea salt. Perfect with cheese.',
			productBasePrice: '7.00',
			productImageUrl: 'https://images.unsplash.com/photo-1590080875515-8a3a8dc5735e?w=400&q=80',
			tags: [{ id: 4 }, { id: 5 }]
		},
		{
			id: 10,
			productName: 'Raspberry Galette',
			productDescription:
				'Free-form pastry with a buttery rough-puff base, fresh raspberries, and a dusting of raw cane sugar.',
			productBasePrice: '9.75',
			productImageUrl: 'https://images.unsplash.com/photo-1464305795204-6f5bbfc7fb81?w=400&q=80',
			tags: [{ id: 2 }]
		}
	];

	let activeTagId = $state(null);
	let searchQuery = $state('');
	let cart = $state([]);

	const filtered = $derived(
		products.filter((p) => {
			const matchesTag = activeTagId === null || p.tags?.some((t) => t.id === activeTagId);

			const matchesSearch =
				p.productName.toLowerCase().includes(searchQuery.toLowerCase()) ||
				p.productDescription.toLowerCase().includes(searchQuery.toLowerCase());

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

				{#each tags as tag}
					<button
						onclick={() => (activeTagId = tag.id)}
						class="rounded-md px-3 py-2 text-left text-sm transition
					{activeTagId === tag.id ? 'bg-primary text-white' : 'hover:bg-surface-container'}"
					>
						{tag.tagName}
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

		<!-- Your grid untouched -->
		<div class="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-4">
			{#each filtered as product (product.id)}
				<ProductCard
					name={product.productName}
					description={product.productDescription ?? ''}
					price={product.productBasePrice}
					imageUrl={product.productImageUrl ?? ''}
					onAddToCart={handleAddToCart}
				/>
			{/each}
		</div>
	</div>
</div>
