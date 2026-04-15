<script>
	import { Button } from '$lib/components/ui/button';
	import { Skeleton } from '$lib/components/ui/skeleton';
	import { resolve } from '$app/paths';
	import { onMount } from 'svelte';
	import { getRecommendations } from '$lib/services/profile';
	import { getMyPreferences } from '$lib/services/preferences';

	/** @type {{ productId: number, productName: string }[]} */
	let recommendations = $state([]);
	let loading = $state(true);
	let error = $state(false);
	let needsPreferences = $state(false);

	onMount(async () => {
		try {
			const prefs = await getMyPreferences();
			if (!prefs?.length) {
				needsPreferences = true;
				recommendations = [];
				return;
			}
			recommendations = await getRecommendations();
		} catch {
			error = true;
		} finally {
			loading = false;
		}
	});
</script>

<!-- Green “AI zone” — distinct from warm bakery chrome; suggestions stacked vertically -->
<div class="min-w-0 md:col-span-4">
	<div
		class="h-full min-w-0 overflow-hidden rounded-2xl border-2 border-emerald-600 bg-emerald-100 shadow-md dark:border-emerald-500 dark:bg-emerald-950/45"
	>
		<div class="flex min-w-0 flex-col gap-1 p-5">
			<div class="mb-0.5">
				<span class="text-sm font-bold tracking-tight text-emerald-950 dark:text-emerald-50"
					>AI-powered recommendations</span
				>
			</div>
			<p
				class="text-[10px] font-bold tracking-widest text-emerald-700 uppercase dark:text-emerald-400"
			>
				AI suggestions
			</p>
			<p class="mb-3 text-xs leading-snug text-emerald-900/90 dark:text-emerald-100/90">
				Set your taste preferences first, then we can suggest products tailored to you.
			</p>

			{#if loading}
				<div class="flex flex-col gap-3">
					{#each Array(4) as _, i (i)}
						<Skeleton
							class="h-10 w-full rounded-xl border border-emerald-300/70 bg-emerald-200/50 dark:border-emerald-700/50 dark:bg-emerald-900/40"
						/>
					{/each}
				</div>
			{:else if error}
				<p
					class="rounded-xl border border-emerald-400/50 bg-card/70 px-3 py-4 text-sm text-emerald-950/85 dark:border-emerald-700/50 dark:bg-emerald-900/35 dark:text-emerald-100/85"
				>
					Could not load recommendations.
				</p>
			{:else if needsPreferences}
				<p
					class="rounded-xl border border-emerald-400/50 bg-card/70 px-3 py-4 text-sm text-emerald-950/90 dark:border-emerald-700/50 dark:bg-emerald-900/35 dark:text-emerald-100/90"
				>
					Set your taste preferences first, then we can suggest products tailored to you.
				</p>
				<Button
					variant="secondary"
					href={resolve('/profile/preferences')}
					class="mt-3 w-full border-emerald-500 bg-emerald-200/90 text-emerald-950 hover:bg-emerald-300 dark:border-emerald-600 dark:bg-emerald-900/55 dark:text-emerald-50 dark:hover:bg-emerald-800/60"
				>
					Edit preferences
				</Button>
			{:else if recommendations.length === 0}
				<p
					class="rounded-xl border border-emerald-400/50 bg-card/70 px-3 py-4 text-sm text-emerald-950/90 dark:border-emerald-700/50 dark:bg-emerald-900/35 dark:text-emerald-100/90"
				>
					No recommendations yet. Order something to get started!
				</p>
			{:else}
				<div class="flex flex-col gap-2.5">
					{#each recommendations as rec (rec.productId)}
						<a
							href={resolve(`/menu?search=${encodeURIComponent(rec.productName ?? '')}`)}
							class="group flex w-full items-center justify-between rounded-xl border border-emerald-500/70 bg-emerald-50/95 px-4 py-3 shadow-sm transition hover:border-emerald-600 hover:shadow-md dark:border-emerald-600/55 dark:bg-emerald-900/40 dark:hover:border-emerald-500"
						>
							<p class="text-sm leading-snug font-semibold text-emerald-950 dark:text-emerald-50">
								{rec.productName}
							</p>
							<svg
								xmlns="http://www.w3.org/2000/svg"
								width="14"
								height="14"
								viewBox="0 0 24 24"
								fill="none"
								stroke="currentColor"
								stroke-width="2"
								class="shrink-0 text-emerald-600 opacity-75 transition-opacity group-hover:opacity-100 dark:text-emerald-400"
								aria-hidden="true"
							>
								<path d="M5 12h14M12 5l7 7-7 7" />
							</svg>
						</a>
					{/each}
				</div>
			{/if}

			<div class="mt-4 text-center">
				<p
					class="text-[10px] font-bold tracking-widest text-emerald-950 uppercase dark:text-emerald-100"
				>
					Disclaimer
				</p>
				<p class="mt-1 text-[10px] leading-snug text-emerald-800/90 dark:text-emerald-300/85">
					Suggestions use AI and may be inaccurate. Not dietary or medical advice.
				</p>
			</div>
		</div>
	</div>
</div>
