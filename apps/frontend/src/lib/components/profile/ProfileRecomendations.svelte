<script>
	import { Card, CardContent, CardHeader, CardTitle } from '$lib/components/ui/card';
	import { Button } from '$lib/components/ui/button';
	import { Separator } from '$lib/components/ui/separator';
	import { resolve } from '$app/paths';

	const items = [
		{ name: 'Croissant', desc: 'Buttery, flaky, golden' },
		{ name: 'Sourdough Bread', desc: 'Classic tangy loaf' },
		{ name: 'Chocolate Tart', desc: 'Rich dark chocolate filling' }
	];
</script>

<div class="md:col-span-4">
	<Card class="h-full">
		<CardHeader>
			<CardTitle>Recommended for You</CardTitle>
		</CardHeader>
		<CardContent class="flex flex-col gap-1">
			{#each items as item, i (item.name)}
				<div class="flex items-center justify-between rounded-lg px-3 py-3 hover:bg-muted transition-colors">
					<div>
						<p class="text-sm font-medium text-foreground">{item.name}</p>
						<p class="text-xs text-muted-foreground">{item.desc}</p>
					</div>
				</div>
				{#if i < items.length - 1}
					<Separator />
				{/if}
			{/each}

			<Button variant="outline" href={resolve('/menu')} class="mt-4 w-full">
				View all products
			</Button>
		</CardContent>
	</Card>
</div>
