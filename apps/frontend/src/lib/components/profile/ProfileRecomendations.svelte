<script>
	import { Card, CardContent, CardHeader, CardTitle } from '$lib/components/ui/card';
	import { Button } from '$lib/components/ui/button';
	import { Separator } from '$lib/components/ui/separator';
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

<div class="md:col-span-4">
	<Card class="h-full">
		<CardHeader>
			<CardTitle>AI Powered Recommendations</CardTitle>
		</CardHeader>
		<CardContent class="flex flex-col gap-1">
			{#if loading}
				{#each Array(3) as _, i}
					<div class="flex items-center gap-3 rounded-lg px-3 py-3">
						<Skeleton class="h-4 w-full" />
					</div>
					{#if i < 2}
						<Separator />
					{/if}
				{/each}
			{:else if error}
				<p class="px-3 py-4 text-sm text-muted-foreground">Could not load recommendations.</p>
			{:else if needsPreferences}
				<p class="px-3 py-4 text-sm text-muted-foreground">
					Set your taste preferences first — then we can suggest products tailored to you.
				</p>
				<Button variant="secondary" href={resolve('/profile/preferences')} class="mt-1 w-full">
					Edit preferences
				</Button>
			{:else if recommendations.length === 0}
				<p class="px-3 py-4 text-sm text-muted-foreground">
					No recommendations yet. Order something to get started!
				</p>
			{:else}
				{#each recommendations as rec, i (rec.productId)}
					<a
						href={resolve(`/menu?search=${encodeURIComponent(rec.productName ?? '')}`)}
						class="flex items-center justify-between rounded-lg px-3 py-3 transition-colors hover:bg-muted"
					>
						<p class="text-sm font-medium text-foreground">{rec.productName}</p>
					</a>
					{#if i < recommendations.length - 1}
						<Separator />
					{/if}
				{/each}
			{/if}

			<Button variant="outline" href={resolve('/menu')} class="mt-4 w-full">
				View all products
			</Button>
		</CardContent>
	</Card>
</div>
