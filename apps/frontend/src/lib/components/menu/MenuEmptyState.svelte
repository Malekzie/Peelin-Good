<script>
	import { Search } from '@lucide/svelte';
	import Button from '$lib/components/ui/button/button.svelte';

	let { onReset = () => {} } = $props();
</script>

<div class="flex flex-col items-center justify-center py-24 text-center">
	<div class="flex h-16 w-16 items-center justify-center rounded-full bg-primary/10">
		<Search class="h-7 w-7 text-primary" />
	</div>
	<h2 class="mt-4 text-lg font-semibold text-foreground">Nothing found</h2>
	<p class="mt-1 text-sm text-muted-foreground">Try a different search or browse all categories.</p>
	<Button
		onclick={onReset}
		class="mt-5 rounded-full bg-primary px-5 py-2 text-sm font-semibold text-primary-foreground hover:bg-primary/90"
	>
		Show all items
	</Button>
</div>
