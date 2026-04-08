<script lang="ts">
	import { resolve } from '$app/paths';
	import { onMount } from 'svelte';
	import { getTags } from '$lib/services/tags';

	interface Category {
		name: string;
		desc: string;
		id: string;
		badge?: string;
	}

	let categories = $state<Category[]>([]);

	onMount(async () => {
		const tags = await getTags();

		categories = tags
			.map((tag: { id: string; name: string; description?: string; isFeatured: boolean }) => ({
				id: tag.id,
				name: tag.name,
				desc: tag.description ?? 'Freshly baked goods',
				badge: tag.isFeatured ? 'Featured' : undefined
			}))
			.sort((a: Category, b: Category) => a.name.localeCompare(b.name));
	});
</script>

<section class="bg-[#FAF7F2] px-6 py-16">
	<div class="mx-auto max-w-7xl">
		<p class="mb-1 text-[11px] font-semibold tracking-[0.2em] text-[#C4714A] uppercase">Browse</p>
		<h2 class="mb-8 text-3xl font-black tracking-tight text-[#2C1A0E]">What are you craving?</h2>

		<div class="overflow-hidden rounded-xl border border-border">
			{#if categories.length === 0}
				<p class="p-6 text-sm text-muted-foreground">Loading categories...</p>
			{:else}
				<div class="grid grid-cols-2 gap-4 md:grid-cols-4">
					{#each categories as cat (cat.id)}
						<a
							href={resolve(`/menu?tag=${cat.id}`)}
							class="group relative flex flex-col gap-1 bg-white px-6 py-7 transition-colors hover:cursor-pointer hover:bg-[#FAF7F2]"
						>
							{#if cat.badge}
								<span
									class="mb-1 self-start rounded-full bg-[#F5EFE6] px-2 py-0.5 text-[10px] font-semibold tracking-wide text-[#C4714A] uppercase"
								>
									{cat.badge}
								</span>
							{/if}
							<span class="text-sm font-semibold text-foreground">{cat.name}</span>
							<span class="text-xs leading-snug text-muted-foreground">{cat.desc}</span>
							<span
								class="absolute top-5 right-4 text-[#C4714A] opacity-0 transition-opacity group-hover:opacity-100"
								>→</span
							>
						</a>
					{/each}
				</div>
			{/if}
		</div>
	</div>
</section>
