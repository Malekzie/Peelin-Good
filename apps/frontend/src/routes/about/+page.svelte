<script lang="ts">
	import AboutUsHero from '$lib/components/about/AboutUsHero.svelte';
	import AboutUsValues from '$lib/components/about/AboutUsValues.svelte';
	import BakeryLocations from '$lib/components/about/BakeryLocations.svelte';
	import { resolve } from '$app/paths';
</script>

<div class="bg-surface-container-low min-h-screen px-6 py-16">
	<div class="mx-auto max-w-5xl space-y-20">
		<!-- Hero -->
		<AboutUsHero />

		<!-- Values -->
		<AboutUsValues />

		<!-- Locations -->
		<BakeryLocations />

		<!-- CTA -->
		<section class="space-y-6 text-center">
			<h2 class="font-headline text-2xl font-bold">Come Taste the Difference</h2>
			<p class="text-on-surface-variant text-sm">
				Explore our menu and discover your new favorite.
			</p>

			<a
				href={resolve('/menu')}
				class="inline-block rounded-full bg-primary px-6 py-3 text-sm font-semibold text-white hover:bg-primary/90"
			>
				View Menu
			</a>
		</section>
	</div>
</div>
