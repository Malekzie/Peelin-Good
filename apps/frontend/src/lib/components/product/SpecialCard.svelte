<script lang="ts">
	import { ShoppingCart } from '@lucide/svelte';

	interface Props {
		name: string;
		description: string;
		price: number;
		emoji: string; // placeholder until real images
		badge?: string;
	}

	let { name, description, price, emoji, badge }: Props = $props();
</script>

<article
	class="group relative flex flex-col overflow-hidden rounded-2xl border border-border bg-card transition-all duration-200 hover:border-primary/30 hover:shadow-lg"
>
	<!-- Image area (placeholder) -->
	<div class="relative flex h-44 items-center justify-center bg-[oklch(0.94_0.03_70)]">
		<span class="text-6xl">{emoji}</span>
		{#if badge}
			<span
				class="absolute top-3 left-3 rounded-full bg-primary px-3 py-1 text-xs font-semibold text-primary-foreground"
			>
				{badge}
			</span>
		{/if}
	</div>

	<!-- Content -->
	<div class="flex flex-1 flex-col gap-2 p-4">
		<h3 class="font-serif text-lg font-semibold text-foreground">{name}</h3>
		<p class="flex-1 text-sm leading-snug text-muted-foreground">{description}</p>

		<div class="mt-2 flex items-center justify-between border-t border-border pt-3">
			<span class="text-base font-bold text-foreground">${price.toFixed(2)}</span>
			<button
				class="flex items-center gap-1.5 rounded-full bg-primary px-4 py-2 text-xs font-semibold text-primary-foreground transition-opacity hover:opacity-90"
			>
				<ShoppingCart size={13} />
				Add to Order
			</button>
		</div>
	</div>
</article>
