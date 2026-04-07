<script>
	import { onMount } from 'svelte';
	import { Skeleton } from '$lib/components/ui/skeleton';
	import { Button } from '$lib/components/ui/button';
	import { getPendingReviews, updateReviewStatus } from '$lib/services/staff-reviews.js';

	let reviews = $state([]);
	let loading = $state(true);
	let error = $state(null);
	let actioning = $state({});

	onMount(async () => {
		try {
			reviews = await getPendingReviews();
		} catch {
			error = true;
		} finally {
			loading = false;
		}
	});

	async function moderate(reviewId, status) {
		actioning[reviewId] = status;
		try {
			await updateReviewStatus(reviewId, status);
			reviews = reviews.filter((r) => r.id !== reviewId);
		} catch {
			// leave in list on failure
		} finally {
			actioning[reviewId] = null;
		}
	}

	function renderStars(rating) {
		return '★'.repeat(rating) + '☆'.repeat(5 - rating);
	}

	function formatDate(dt) {
		if (!dt) return '—';
		return new Date(dt).toLocaleDateString('en-CA', {
			year: 'numeric',
			month: 'short',
			day: 'numeric'
		});
	}
</script>

<main class="flex-1 overflow-y-auto p-8 lg:p-10">
	<div class="mx-auto max-w-4xl space-y-6">
		<div class="flex items-center justify-between">
			<div>
				<h1 class="text-2xl font-bold tracking-tight text-foreground">Reviews</h1>
				<p class="mt-1 text-sm text-muted-foreground">Pending reviews awaiting moderation</p>
			</div>
			{#if !loading && !error}
				<span class="rounded-full bg-muted px-3 py-1 text-xs font-semibold text-foreground">
					{reviews.length} pending
				</span>
			{/if}
		</div>

		{#if loading}
			<div class="space-y-3">
				{#each Array(4) as _, i (i)}
					<Skeleton class="h-28 rounded-xl" />
				{/each}
			</div>
		{:else if error}
			<p class="text-sm text-destructive">Failed to load reviews.</p>
		{:else if reviews.length === 0}
			<div class="rounded-xl border border-border bg-card p-10 text-center">
				<p class="text-sm text-muted-foreground">No pending reviews</p>
			</div>
		{:else}
			<div class="space-y-3">
				{#each reviews as review (review.id)}
					<div class="rounded-xl border border-border bg-card p-5">
						<div class="flex items-start justify-between gap-4">
							<div class="space-y-1">
								<div class="flex items-center gap-2">
									<span class="text-sm font-semibold text-foreground">
										{review.reviewerDisplayName ?? 'Anonymous'}
									</span>
									<span class="text-sm text-amber-500">{renderStars(review.rating)}</span>
								</div>
								<p class="text-xs text-muted-foreground">
									{review.bakeryName ?? '—'} · {formatDate(review.submittedAt)}
								</p>
								{#if review.comment}
									<p class="mt-2 text-sm text-foreground">{review.comment}</p>
								{/if}
							</div>
							<div class="flex shrink-0 gap-2">
								<Button
									size="sm"
									variant="outline"
									onclick={() => moderate(review.id, 'approved')}
									disabled={!!actioning[review.id]}
								>
									{actioning[review.id] === 'approved' ? '...' : 'Approve'}
								</Button>
								<Button
									size="sm"
									variant="destructive"
									onclick={() => moderate(review.id, 'rejected')}
									disabled={!!actioning[review.id]}
								>
									{actioning[review.id] === 'rejected' ? '...' : 'Reject'}
								</Button>
							</div>
						</div>
					</div>
				{/each}
			</div>
		{/if}
	</div>
</main>
