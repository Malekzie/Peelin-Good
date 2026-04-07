<script lang="ts">
	import SpecialCard from '$lib/components/product/SpecialCard.svelte';

	const specials = [
		{
			name: 'Sourdough Boule',
			description:
				'Tangy, open-crumb sourdough with a blistered crust. Made with our 3-year-old starter.',
			price: 8.5,
			badge: "Today's Pick"
		},
		{
			name: 'Butter Croissant',
			description: '72-hour lamination, French-style butter. Flaky layers from edge to edge.',
			price: 3.75
		},
		{
			name: 'Lemon Tart',
			description:
				'Silky curd in a crisp pastry shell. Bright, just-tart-enough, and gone by noon.',
			price: 5.25,
			badge: 'Seasonal'
		},
		{
			name: 'Cinnamon Roll',
			description: 'Brown sugar swirl, pillowy pull-apart texture, cream cheese glaze.',
			price: 4.5
		},
		{
			name: 'Carrot Cake Slice',
			description: 'Walnuts, warm spice, and whipped cream cheese frosting. A proper slice.',
			price: 6.0
		},
		{
			name: 'Almond Danish',
			description: 'Buttery pastry, almond cream centre, sliced almonds on top.',
			price: 4.25,
			badge: 'New'
		}
	];
</script>

<section class="bg-[#F5EFE6] px-6 py-16">
	<div class="mx-auto max-w-7xl">
		<p class="mb-1 text-[11px] font-semibold tracking-[0.2em] text-[#C4714A] uppercase">
			Out of the oven
		</p>
		<h2 class="mb-2 text-3xl font-black tracking-tight text-[#2C1A0E]">Today's specials</h2>
		<p class="mb-8 text-sm text-muted-foreground">A curated selection of what's fresh right now.</p>

		<div class="rounded-xl">
			<div class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
				{#each specials as special (special.name)}
					<SpecialCard {...special} />
				{/each}
			</div>
		</div>
	</div>
</section>
