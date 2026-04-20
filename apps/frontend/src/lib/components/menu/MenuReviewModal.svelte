<script>
	let {
		open = $bindable(false),
		productName = '',
		isLoggedIn = false,
		rating = $bindable(0),
		comment = $bindable(''),
		submitting = $bindable(false),
		error = $bindable(null),
		success = $bindable(false),
		onClose = () => {},
		onSubmit = () => {}
	} = $props();
</script>

{#if open}
	<div class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
		<div class="w-full max-w-md rounded-2xl border border-border bg-card p-6 shadow-xl">
			<h2 class="text-lg font-bold text-foreground">Review {productName}</h2>

			<div class="mt-4 flex gap-2">
				{#each [1, 2, 3, 4, 5] as star (star)}
					<button
						onclick={() => (rating = star)}
						class="text-2xl transition-transform hover:scale-110 {rating >= star
							? 'text-yellow-400'
							: 'text-muted-foreground/30'}"
					>
						★
					</button>
				{/each}
			</div>

			<textarea
				bind:value={comment}
				placeholder="Share your experience (optional)"
				rows="3"
				disabled={submitting}
				class="mt-4 w-full resize-none rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring"
			></textarea>

			{#if error}
				<p class="mt-2 text-xs text-destructive">{error}</p>
			{/if}
			{#if success}
				<p class="mt-2 text-xs text-emerald-600 dark:text-emerald-400">
					✓ Thanks! Your review was posted.
				</p>
			{/if}

			<div class="mt-4 flex justify-end gap-3">
				<button
					onclick={() => {
						onClose();
						open = false;
					}}
					disabled={submitting}
					class="rounded-full border border-border px-4 py-2 text-sm font-medium hover:bg-muted disabled:opacity-50"
				>
					Cancel
				</button>
				<button
					onclick={onSubmit}
					disabled={submitting}
					class="rounded-full bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
				>
					{submitting ? 'Submitting...' : 'Submit'}
				</button>
			</div>
		</div>
	</div>
{/if}
