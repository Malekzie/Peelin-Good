<script>
	import { onMount } from 'svelte';
	import { Skeleton } from '$lib/components/ui/skeleton';
	import { getBakeries, getBakeryReviews, getBakeryAverage } from '$lib/services/bakeries';
	import { MapPin, Phone, Mail } from '@lucide/svelte';

	let bakeries = $state([]);
	let loading = $state(true);

	onMount(async () => {
		try {
			const raw = await getBakeries();
			bakeries = await Promise.all(
				raw.map(async (b) => {
					const [reviews, average] = await Promise.all([
						getBakeryReviews(b.id).catch(() => []),
						getBakeryAverage(b.id).catch(() => null)
					]);
					return { ...b, reviews: reviews.slice(0, 4), average };
				})
			);
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

	function stars(rating) {
		return '★'.repeat(rating) + '☆'.repeat(5 - rating);
	}
</script>

<!-- Hero -->
<div class="border-b border-border bg-[#FAF7F2] px-6 py-16 text-center">
	<p class="mb-2 text-xs font-semibold tracking-[0.2em] text-primary uppercase">Find us</p>
	<h1 class="font-headline text-4xl font-black tracking-tight text-foreground">Our Locations</h1>
	<p class="mx-auto mt-3 max-w-sm text-sm text-muted-foreground">
		Fresh bread and pastries baked every morning. Come visit us.
	</p>
</div>

<!-- Location cards -->
<div class="mx-auto max-w-5xl space-y-12 px-6 py-14">
	{#if loading}
		<div class="mb-6 flex items-center justify-center gap-2 text-sm text-muted-foreground">
			<div
				class="h-4 w-4 animate-spin rounded-full border-2 border-primary border-t-transparent"
			></div>
			Loading locations...
		</div>
		<div class="space-y-8">
			{#each Array(3) as _, i (i)}
				<Skeleton class="h-80 w-full rounded-2xl" />
			{/each}
		</div>
	{:else if bakeries.length === 0}
		<p class="text-center text-sm text-muted-foreground">No locations found.</p>
	{:else}
		{#each bakeries as bakery (bakery.id)}
			<div class="overflow-hidden rounded-2xl border border-border bg-card shadow-sm">
				<div class="grid grid-cols-1 md:grid-cols-2">
					<!-- Left: image + info -->
					<div class="flex flex-col border-b border-border md:border-r md:border-b-0">
						{#if bakery.bakeryImageUrl}
							<img src={bakery.bakeryImageUrl} alt={bakery.name} class="h-56 w-full object-cover" />
						{/if}
						<div class="flex flex-1 flex-col gap-5 p-8">
							<div>
								<h2 class="text-2xl font-bold text-foreground">{bakery.name}</h2>
								{#if bakery.average !== null}
									<p class="mt-1 text-base text-yellow-500">
										{stars(Math.round(bakery.average))}
										<span class="ml-1 text-sm text-muted-foreground"
											>({bakery.average.toFixed(1)})</span
										>
									</p>
								{:else}
									<p class="mt-1 text-sm text-muted-foreground">No reviews yet</p>
								{/if}
							</div>

							<div class="space-y-3">
								{#if bakery.address}
									<div class="flex items-start gap-3">
										<MapPin size={16} class="mt-0.5 shrink-0 text-primary" />
										<span class="text-sm text-foreground">{formatAddress(bakery.address)}</span>
									</div>
								{/if}
								<div class="flex items-center gap-3">
									<Phone size={16} class="shrink-0 text-primary" />
									<a href="tel:{bakery.phone}" class="text-sm text-foreground hover:text-primary"
										>{bakery.phone}</a
									>
								</div>
								<div class="flex items-center gap-3">
									<Mail size={16} class="shrink-0 text-primary" />
									<a href="mailto:{bakery.email}" class="text-sm text-foreground hover:text-primary"
										>{bakery.email}</a
									>
								</div>
							</div>
						</div>
					</div>

					<!-- Right: reviews -->
					<div class="flex flex-col gap-4 bg-muted/30 p-8">
						<h3 class="text-xs font-semibold tracking-widest text-muted-foreground uppercase">
							Customer Reviews
						</h3>
						{#if bakery.reviews.length === 0}
							<p class="text-sm text-muted-foreground">No reviews yet — be the first!</p>
						{:else}
							<div class="space-y-4">
								{#each bakery.reviews as review (review.id)}
									<div class="rounded-xl border border-border bg-background p-4">
										<div class="mb-1 flex items-center justify-between">
											<p class="text-sm font-semibold text-foreground">
												{review.reviewerDisplayName}
											</p>
											<p class="text-sm text-yellow-500">{stars(review.rating)}</p>
										</div>
										{#if review.comment}
											<p class="text-sm leading-relaxed text-muted-foreground">{review.comment}</p>
										{/if}
									</div>
								{/each}
							</div>
						{/if}
					</div>
				</div>
			</div>
		{/each}
	{/if}
</div>
