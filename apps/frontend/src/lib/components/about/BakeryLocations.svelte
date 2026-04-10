<script lang="ts">
	import { onMount } from 'svelte';
	import { Skeleton } from '$lib/components/ui/skeleton';
	import { getBakeries, getBakeryReviews, getBakeryAverage } from '$lib/services/bakeries';

	type Review = {
		id: string;
		reviewerDisplayName: string;
		rating: number;
		comment: string | null;
	};

	type BakeryWithReviews = {
		id: number;
		name: string;
		phone: string;
		email: string;
		status: string;
		address: {
			addressLine1: string;
			addressCity: string;
			addressProvince: string;
			addressPostalCode: string;
		} | null;
		reviews: Review[];
		average: number | null;
	};

	let bakeries = $state<BakeryWithReviews[]>([]);
	let loading = $state(true);
	let expanded = $state<number | null>(null);

	onMount(async () => {
		try {
			const raw = await getBakeries();
			bakeries = await Promise.all(
				raw.map(async (b: any) => {
					const [reviews, average] = await Promise.all([
						getBakeryReviews(b.id).catch(() => []),
						getBakeryAverage(b.id).catch(() => null)
					]);
					return { ...b, reviews: reviews.slice(0, 3), average };
				})
			);
		} catch (e) {
			console.error('Failed to load bakeries:', e);
		} finally {
			loading = false;
		}
	});

	function formatAddress(address: any) {
		if (!address) return '';
		return `${address.addressLine1}, ${address.addressCity}, ${address.addressProvince}`;
	}

	function stars(rating: number) {
		return '★'.repeat(rating) + '☆'.repeat(5 - rating);
	}
</script>

<div class="mx-auto max-w-5xl px-4 py-12">
	<h2 class="mb-8 text-center text-3xl font-bold">Our Bakery Locations</h2>

	{#if loading}
		<div class="grid gap-6 sm:grid-cols-1 md:grid-cols-2 lg:grid-cols-3">
			{#each Array(3) as _, i (i)}
				<Skeleton class="h-48 w-full rounded-xl" />
			{/each}
		</div>
	{:else}
		<div class="grid gap-6 sm:grid-cols-1 md:grid-cols-2 lg:grid-cols-3">
			{#each bakeries as bakery (bakery.id)}
				<div
					class="rounded-xl border border-gray-200 bg-white shadow-sm transition hover:shadow-lg"
				>
					<div class="p-6">
						<h3 class="mb-1 text-xl font-semibold">{bakery.name}</h3>

						{#if bakery.average !== null}
							<p class="mb-2 text-sm text-yellow-500">
								{stars(Math.round(bakery.average))}
								<span class="text-gray-500">({bakery.average.toFixed(1)})</span>
							</p>
						{/if}

						<p class="mb-1 text-sm text-gray-600">{formatAddress(bakery.address)}</p>
						<p class="mb-1 text-sm text-gray-600">
							<span class="font-medium">Phone:</span>
							{bakery.phone}
						</p>
						<p class="mb-3 text-sm text-gray-600">
							<span class="font-medium">Email:</span>
							{bakery.email}
						</p>

						{#if bakery.reviews.length > 0}
							<button
								onclick={() => (expanded = expanded === bakery.id ? null : bakery.id)}
								class="text-xs font-semibold text-primary hover:underline"
							>
								{expanded === bakery.id ? 'Hide reviews' : `See reviews (${bakery.reviews.length})`}
							</button>

							{#if expanded === bakery.id}
								<div class="mt-3 space-y-3 border-t border-gray-100 pt-3">
									{#each bakery.reviews as review (review.id)}
										<div class="rounded-lg bg-gray-50 px-3 py-2">
											<div class="flex items-center justify-between">
												<p class="text-xs font-semibold text-gray-800">
													{review.reviewerDisplayName}
												</p>
												<p class="text-xs text-yellow-500">{stars(review.rating)}</p>
											</div>
											{#if review.comment}
												<p class="mt-1 text-xs text-gray-500">{review.comment}</p>
											{/if}
										</div>
									{/each}
								</div>
							{/if}
						{/if}
					</div>
				</div>
			{/each}
		</div>
	{/if}
</div>
