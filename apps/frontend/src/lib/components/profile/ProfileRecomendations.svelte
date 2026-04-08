<script>
	import { Card, CardContent, CardHeader, CardTitle } from '$lib/components/ui/card';
	import { Button } from '$lib/components/ui/button';
	import { Separator } from '$lib/components/ui/separator';
	import { Skeleton } from '$lib/components/ui/skeleton';
	import { resolve } from '$app/paths';
	import { onMount } from 'svelte';
	import { getRecommendations } from '$lib/services/profile';

	let recommendations = $state([]);
	let loading = $state(true);
	let error = $state(false);

	onMount(async () => {
		try {
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
			<CardTitle>Recommended for You</CardTitle>
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
			{:else if error || recommendations.length === 0}
				<p class="px-3 py-4 text-sm text-muted-foreground">
					No recommendations yet. Order something to get started!
				</p>
			{:else}
				{#each recommendations as name, i (name)}
					<a
						href={resolve(`/menu?search=${encodeURIComponent(name)}`)}
						class="flex items-center justify-between rounded-lg px-3 py-3 transition-colors hover:bg-muted"
					>
						<p class="text-sm font-medium text-foreground">{name}</p>
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
