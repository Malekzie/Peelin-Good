<script>
	import { onMount } from 'svelte';
	import { Skeleton } from '$lib/components/ui/skeleton';
	import {
		getBakeries,
		getBakeryReviews,
		getBakeryAverage,
		createBakeryReview
	} from '$lib/services/bakeries';
	import { MapPin, Phone, Mail } from '@lucide/svelte';
	import ReviewSubmissionOverlay from '$lib/components/review/ReviewSubmissionOverlay.svelte';
	import { user } from '$lib/stores/authStore';
	import { truncateModerationMessage } from '$lib/utils/reviewMessage';

	let bakeries = $state([]);
	let loading = $state(true);
	let reviewModal = $state(null);
	let reviewRating = $state(0);
	let reviewComment = $state('');
	let reviewGuestName = $state('');
	let reviewSubmitting = $state(false);
	let reviewError = $state(null);
	let reviewSuccess = $state(false);
	let expandedBakeries = $state(new Set());

	onMount(async () => {
		try {
			const raw = await getBakeries();
			bakeries = await Promise.all(
				raw.map(async (b) => {
					const [reviews, average] = await Promise.all([
						getBakeryReviews(b.id).catch(() => []),
						getBakeryAverage(b.id).catch(() => null)
					]);
					return { ...b, reviews, average };
				})
			);
		} catch (e) {
			console.error('Failed to load bakeries:', e);
		} finally {
			loading = false;
		}
	});

	function openBakeryReview(bakery) {
		reviewModal = { bakeryId: bakery.id, bakeryName: bakery.name };
		reviewRating = 0;
		reviewComment = '';
		reviewGuestName = '';
		reviewError = null;
		reviewSuccess = false;
	}

	function closeReviewModal() {
		reviewModal = null;
	}

	async function submitBakeryReview() {
		if (reviewRating === 0) {
			reviewError = 'Please select a star rating.';
			return;
		}
		reviewSubmitting = true;
		reviewError = null;
		reviewSuccess = false;
		try {
			const submitted = await createBakeryReview(
				reviewModal.bakeryId,
				reviewRating,
				reviewComment,
				reviewGuestName || null
			);
			const status = (submitted?.status ?? '').toLowerCase();
			if (status === 'rejected') {
				const short = truncateModerationMessage(submitted?.moderationMessage);
				reviewError = short
					? `Couldn't post review: ${short}`
					: "We couldn't post that review. Try different wording.";
			} else {
				reviewSuccess = true;
				const freshReviews = await getBakeryReviews(reviewModal.bakeryId).catch(() => []);
				bakeries = bakeries.map((b) =>
					b.id === reviewModal.bakeryId ? { ...b, reviews: freshReviews } : b
				);
				setTimeout(() => closeReviewModal(), 1500);
			}
		} catch (e) {
			reviewError = e.message ?? 'Failed to submit review.';
		} finally {
			reviewSubmitting = false;
		}
	}

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
								{#if bakery.address}
									<a
										href="https://www.google.com/maps/dir/?api=1&destination={encodeURIComponent(
											formatAddress(bakery.address)
										)}"
										target="_blank"
										rel="noopener noreferrer"
										class="mt-2 inline-flex items-center gap-2 rounded-full bg-primary px-4 py-2 text-xs font-semibold text-white hover:opacity-90"
									>
										<MapPin size={12} />
										Get directions
									</a>
								{/if}
							</div>
						</div>
					</div>

					<!-- Right: reviews -->
					<div class="flex flex-col gap-4 bg-muted/30 p-8">
						<h3 class="text-xs font-semibold tracking-widest text-muted-foreground uppercase">
							Customer Reviews
						</h3>
						<button
							onclick={() => openBakeryReview(bakery)}
							class="text-sm font-semibold text-primary hover:underline"
						>
							Leave a Review
						</button>
						{#if bakery.reviews.length === 0}
							<p class="text-sm text-muted-foreground">No reviews yet — be the first!</p>
						{:else}
							<div class="space-y-4">
								{#each expandedBakeries.has(bakery.id) ? bakery.reviews : bakery.reviews.slice(0, 4) as review (review.id)}
									<div class="rounded-xl border border-border bg-background p-4">
										<div class="mb-1 flex items-center justify-between">
											<div class="flex items-center gap-2">
												<p class="text-sm font-semibold text-foreground">
													{review.reviewerDisplayName}
												</p>
												{#if review.verifiedAccount}
													<span
														class="rounded-full bg-emerald-100 px-2 py-0.5 text-[10px] font-semibold text-emerald-800"
														>✓ Verified</span
													>
												{/if}
											</div>
											<p class="text-sm text-yellow-500">{stars(review.rating)}</p>
										</div>
										{#if review.comment}
											<p class="text-sm leading-relaxed text-muted-foreground">{review.comment}</p>
										{/if}
									</div>
								{/each}
								{#if bakery.reviews.length > 4}
									<button
										onclick={() => {
											const next = new Set(expandedBakeries);
											if (next.has(bakery.id)) {
												next.delete(bakery.id);
											} else {
												next.add(bakery.id);
											}
											expandedBakeries = next;
										}}
										class="text-xs font-semibold text-primary hover:underline"
									>
										{expandedBakeries.has(bakery.id)
											? 'Show less'
											: `See all ${bakery.reviews.length} reviews`}
									</button>
								{/if}
							</div>
						{/if}
					</div>
				</div>
			</div>
		{/each}
	{/if}
</div>

{#if reviewModal}
	<div class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
		<div class="w-full max-w-md rounded-2xl border border-border bg-card p-6 shadow-xl">
			<h2 class="text-lg font-bold text-foreground">Review {reviewModal.bakeryName}</h2>

			<div class="mt-4 flex gap-2">
				{#each [1, 2, 3, 4, 5] as star (star)}
					<button
						onclick={() => (reviewRating = star)}
						class="text-2xl transition-transform hover:scale-110 {reviewRating >= star
							? 'text-yellow-400'
							: 'text-muted-foreground/30'}">★</button
					>
				{/each}
			</div>

			{#if !$user}
				<input
					type="text"
					placeholder="Your name (optional)"
					bind:value={reviewGuestName}
					class="mt-4 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
				/>
			{/if}

			<textarea
				bind:value={reviewComment}
				placeholder="Leave a comment (optional)"
				rows="3"
				disabled={reviewSubmitting}
				class="mt-3 w-full resize-none rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
			></textarea>

			{#if reviewError}
				<p class="mt-2 text-xs text-destructive">{reviewError}</p>
			{/if}
			{#if reviewSuccess}
				<p class="mt-2 text-xs text-green-600">✓ Thanks! Your review was posted.</p>
			{/if}

			<div class="mt-4 flex justify-end gap-3">
				<button
					onclick={closeReviewModal}
					disabled={reviewSubmitting}
					class="rounded-full border border-border px-4 py-2 text-sm font-medium hover:bg-muted disabled:opacity-50"
					>Cancel</button
				>
				<button
					onclick={submitBakeryReview}
					disabled={reviewSubmitting}
					class="rounded-full bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground hover:opacity-90 disabled:opacity-50"
					>{reviewSubmitting ? 'Submitting...' : 'Submit'}</button
				>
			</div>
		</div>
	</div>
{/if}

<ReviewSubmissionOverlay visible={reviewSubmitting} />
