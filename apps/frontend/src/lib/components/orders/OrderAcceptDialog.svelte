<script>
	/**
	 * Dialog for confirming delivery acceptance
	 * Offers two paths: accept and review, or just accept
	 */
	let {
		dialog = null,
		onClose = () => {},
		onAccept = () => {},
		onAcceptAndReview = () => {}
	} = $props();
</script>

{#if dialog}
	<div class="fixed inset-0 z-40 bg-black/50" onclick={() => onClose()} role="presentation"></div>
	<div class="fixed inset-0 z-50 flex items-center justify-center p-4">
		<div class="w-full max-w-sm rounded-2xl border border-border bg-card p-6 shadow-xl">
			<h2 class="text-lg font-bold text-foreground">Order Received?</h2>
			<p class="mt-1 text-sm text-muted-foreground">
				Confirm you received Order #{dialog.order.orderNumber} from {dialog.order.bakeryName ??
					"Peelin' Good"}.
			</p>
			<div class="mt-5 flex flex-col gap-3">
				<button
					onclick={() => onAcceptAndReview(dialog.order)}
					class="w-full rounded-full bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground hover:opacity-90"
				>
					Yes, leave a review
				</button>
				<button
					onclick={() => onAccept(dialog.order.id)}
					class="w-full rounded-full border border-border px-4 py-2 text-sm font-semibold text-foreground hover:bg-muted"
				>
					No, finish order
				</button>
				<button
					onclick={() => onClose()}
					class="w-full px-4 py-2 text-sm text-muted-foreground hover:text-foreground"
				>
					Cancel
				</button>
			</div>
		</div>
	</div>
{/if}
