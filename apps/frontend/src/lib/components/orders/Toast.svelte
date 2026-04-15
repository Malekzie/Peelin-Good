<script>
	let { message = null } = $props();

	const isRejected = $derived(
		message?.toLowerCase().includes("couldn't") ||
			message?.toLowerCase().includes('rejected') ||
			message?.toLowerCase().includes("couldn't post")
	);
</script>

{#if message}
	<div
		class="fixed bottom-6 left-1/2 z-[200] flex max-w-md -translate-x-1/2 items-start gap-3 rounded-xl border px-4 py-3 text-sm shadow-lg
			{isRejected
			? 'border-destructive/30 bg-destructive/10 text-destructive'
			: 'border-emerald-200 bg-emerald-50 text-emerald-800 dark:border-emerald-800 dark:bg-emerald-950 dark:text-emerald-300'}"
		role="status"
	>
		<span class="mt-0.5 text-base">{isRejected ? '✕' : '✓'}</span>
		<span>{message}</span>
	</div>
{/if}
