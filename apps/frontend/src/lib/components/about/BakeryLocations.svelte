<script>
	import { onMount } from 'svelte';
	import { Skeleton } from '$lib/components/ui/skeleton';
	import { getBakeries } from '$lib/services/bakeries';
	import { resolve } from '$app/paths';

	const INITIAL_COUNT = 3;

	let bakeries = $state([]);
	let loading = $state(true);
	let expanded = $state(false);

	const visibleBakeries = $derived(expanded ? bakeries : bakeries.slice(0, INITIAL_COUNT));
	const hasMore = $derived(bakeries.length > INITIAL_COUNT);

	onMount(async () => {
		try {
			bakeries = await getBakeries();
		} catch (e) {
			console.error('Failed to load bakeries:', e);
		} finally {
			loading = false;
		}
	});

	function formatAddress(address) {
		if (!address) return '';
		return `${address.line1}, ${address.city}, ${address.province} ${address.postalCode}`;
	}

	function mapsUrl(address) {
		if (!address) return '#';
		const query = encodeURIComponent(formatAddress(address));
		return `https://www.google.com/maps/dir/?api=1&destination=${query}`;
	}
</script>

<section class="space-y-6">
	<div class="text-center">
		<h2 class="font-headline text-2xl font-bold">Our Locations</h2>
		<p class="text-on-surface-variant mt-1 text-sm">Find a Peelin' Good near you.</p>
	</div>

	{#if loading}
		<div class="grid gap-6 sm:grid-cols-1 md:grid-cols-2 lg:grid-cols-3">
			{#each Array(3) as _, i (i)}
				<Skeleton class="h-40 w-full rounded-xl" />
			{/each}
		</div>
	{:else}
		<div class="grid gap-6 sm:grid-cols-1 md:grid-cols-2 lg:grid-cols-3">
			{#each visibleBakeries as bakery (bakery.id)}
				<div class="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
					<h3 class="mb-1 text-lg font-semibold">{bakery.name}</h3>
					<p class="mb-1 text-sm text-gray-600">{formatAddress(bakery.address)}</p>
					<p class="mb-1 text-sm text-gray-600">
						<span class="font-medium">Phone:</span>
						{bakery.phone}
					</p>
					<p class="mb-4 text-sm text-gray-600">
						<span class="font-medium">Email:</span>
						{bakery.email}
					</p>
					<a
						href={mapsUrl(bakery.address)}
						target="_blank"
						rel="noopener noreferrer"
						class="text-xs font-semibold text-primary hover:underline"
					>
						Get directions →
					</a>
				</div>
			{/each}
		</div>

		{#if hasMore}
			<div class="text-center">
				<button
					onclick={() => (expanded = !expanded)}
					class="inline-block rounded-full border border-primary px-6 py-2 text-sm font-semibold text-primary hover:bg-primary/10"
				>
					{expanded ? 'Show less' : `See all ${bakeries.length} locations`}
				</button>
			</div>
		{/if}

		<div class="text-center">
			<a
				href={resolve('/locations')}
				class="inline-block rounded-full border border-primary px-6 py-2 text-sm font-semibold text-primary hover:bg-primary/10"
			>
				View all locations & reviews
			</a>
		</div>
	{/if}
</section>
