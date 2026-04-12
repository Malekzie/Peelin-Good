<script>
	/** Full-screen blocking UI while a review request (AI moderation) is in flight. */
	let { visible = false } = $props();
</script>

{#if visible}
	<div
		class="fixed inset-0 z-[180] flex items-center justify-center bg-black/50 p-6"
		role="status"
		aria-live="polite"
		aria-busy="true"
	>
		<div
			class="flex max-w-sm flex-col items-center gap-4 rounded-2xl border border-border bg-card px-8 py-7 shadow-xl"
		>
			<span
				class="inline-block size-11 animate-spin rounded-full border-[3px] border-muted border-t-[#C25F1A]"
				aria-hidden="true"
			></span>
			<div class="text-center">
				<p class="text-sm font-semibold text-foreground">Checking your review…</p>
				<p class="mt-1 text-xs text-muted-foreground">
					Sending for AI moderation. This may take a few seconds.
				</p>
			</div>
		</div>
	</div>
{/if}
