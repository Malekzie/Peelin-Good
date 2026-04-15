<script>
	/**
	 * Modal form for submitting a review with star rating and comment
	 * Handles the review submission flow including moderation feedback
	 */
	let {
		modal = null,
		rating = $bindable(0),
		comment = $bindable(''),
		submitting = false,
		error = null,
		success = false,
		outcome = 'success',
		onClose = () => {},
		onSubmit = () => {}
	} = $props();
</script>

{#if modal}
	<div class="fixed inset-0 z-40 bg-black/50" onclick={onClose} role="presentation"></div>
	<div class="fixed inset-0 z-50 flex items-center justify-center p-4">
		<div class="w-full max-w-md rounded-2xl border border-border bg-card p-6 shadow-xl">
			<h2 class="text-lg font-bold text-foreground">
				{modal.type === 'order' ? 'Review your order' : 'Review product'}
			</h2>
			<p class="mt-1 text-sm text-muted-foreground">{modal.label}</p>

			<div class="mt-5 flex gap-2">
				{#each [1, 2, 3, 4, 5] as star (star)}
					<button
						onclick={() => {
							if (!submitting) rating = star;
						}}
						class="text-2xl transition-transform hover:scale-110 {rating >= star
							? 'text-yellow-400'
							: 'text-muted-foreground/30'} {submitting ? 'cursor-not-allowed' : ''}"
					>
						★
					</button>
				{/each}
			</div>

			<textarea
				bind:value={comment}
				placeholder="Leave a comment (optional)"
				rows="3"
				disabled={submitting}
				class="mt-4 w-full resize-none rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary disabled:cursor-not-allowed disabled:opacity-50"
			></textarea>

			{#if submitting}
				<div class="mt-3 flex items-center gap-2 text-xs text-muted-foreground">
					<span
						class="inline-block size-3 animate-spin rounded-full border-2 border-muted-foreground/30 border-t-primary"
						aria-hidden="true"
					></span>
					<span>Moderating your review... this can take a few seconds.</span>
				</div>
			{/if}

			{#if error}
				<p class="mt-2 text-xs text-destructive">{error}</p>
			{/if}

			{#if success}
				<p
					class="mt-2 text-xs {outcome === 'success'
						? 'text-green-600'
						: outcome === 'rejected'
							? 'text-destructive'
							: 'text-muted-foreground'}"
				>
					{#if outcome === 'success'}
						✓ Thanks! Your review was posted.
					{:else if outcome === 'rejected'}
						We couldn't post that review. Try different wording.
					{:else}
						Your review is being checked and will appear if approved.
					{/if}
				</p>
			{/if}

			<div class="mt-4 flex justify-end gap-3">
				<button
					onclick={onClose}
					disabled={submitting}
					class="rounded-full border border-border px-4 py-2 text-sm font-medium hover:bg-muted disabled:cursor-not-allowed disabled:opacity-50"
				>
					Back
				</button>
				<button
					onclick={onSubmit}
					disabled={submitting}
					class="rounded-full bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground hover:opacity-90 disabled:opacity-50"
				>
					{submitting ? 'Submitting...' : 'Submit'}
				</button>
			</div>
		</div>
	</div>
{/if}
