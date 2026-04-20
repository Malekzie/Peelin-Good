<script lang="ts">
	import { onMount } from 'svelte';
	import { getTopReviews } from '$lib/services/review';
	import type { ReviewRecord } from '$lib/services/types';

	type ReviewWithBakery = ReviewRecord & { bakeryName?: string | null };

	let reviews = $state<ReviewWithBakery[]>([]);

	function stars(n: number) {
		return Array(n).fill('★').join('');
	}

	function initialsOf(name: string | null | undefined): string {
		const source = (name ?? '').trim();
		if (!source) return '?';
		if (/^guest(\s+customer|\s+c\.?)?$/i.test(source)) return 'GC';
		const parts = source.split(/\s+/).filter(Boolean);
		if (parts.length >= 2) {
			return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
		}
		return source.slice(0, 2).toUpperCase();
	}

	onMount(async () => {
		try {
			const topReviews = await getTopReviews(3);
			reviews = topReviews.map((r) => ({
				...r,
				bakeryName: (r as any).bakeryName ?? null
			}));
		} catch (e) {
			console.error('Failed to load reviews:', e);
		}
	});
</script>

<section class="bg-muted px-6 py-20">
	<div class="mx-auto max-w-7xl">
		<div class="mb-10 flex flex-col gap-2">
			<p class="text-sm font-semibold tracking-widest text-primary uppercase">Kind Words</p>
			<h2 class="font-serif text-4xl font-bold text-foreground">What our customers say</h2>
		</div>

		<div class="grid grid-cols-1 gap-6 md:grid-cols-3">
			{#each reviews as r (r.id)}
				<figure class="flex flex-col gap-4 rounded-2xl border border-border bg-card p-6">
					<p class="text-sm tracking-widest text-primary">{stars(r.rating)}</p>
					<blockquote class="flex-1 text-sm leading-relaxed text-foreground">
						"{r.comment}"
					</blockquote>
					<figcaption class="border-t border-border pt-4">
						<div class="flex items-center gap-2.5">
							{#if r.reviewerPhotoUrl && !r.reviewerPhotoApprovalPending}
								<img
									src={r.reviewerPhotoUrl}
									alt={r.reviewerDisplayName ?? 'Reviewer'}
									class="h-8 w-8 rounded-full object-cover"
								/>
							{:else if r.reviewerDisplayName}
								<div
									class="flex h-8 w-8 items-center justify-center rounded-full bg-primary text-[11px] font-semibold text-primary-foreground"
								>
									{initialsOf(r.reviewerDisplayName)}
								</div>
							{:else}
								<div
									class="flex h-8 w-8 items-center justify-center rounded-full bg-primary text-[11px] font-semibold text-primary-foreground"
								>
									GC
								</div>
							{/if}
							<div>
								<p class="text-sm font-semibold text-foreground">{r.reviewerDisplayName}</p>
								{#if r.bakeryName}
									<p class="text-xs text-muted-foreground">{r.bakeryName}</p>
								{/if}
							</div>
						</div>
					</figcaption>
				</figure>
			{/each}
		</div>
	</div>
</section>
