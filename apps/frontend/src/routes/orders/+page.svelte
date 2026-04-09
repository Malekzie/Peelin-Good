<script>
	import ProfileSidebar from '$lib/components/profile/ProfileSidebar.svelte';
	import { Skeleton } from '$lib/components/ui/skeleton';
	import { Badge } from '$lib/components/ui/badge';
	import { onMount } from 'svelte';
	import { getMyOrders } from '$lib/services/orders';
	import { createProductReview, createOrderReview } from '$lib/services/review';
	import { apiFetch } from '$lib/utils/api';
	import { resolve } from '$app/paths';

	const API = '/api/v1';

	let orders = $state([]);
	let loading = $state(true);
	let error = $state(null);

	// Accept delivery dialog state
	let acceptDialog = $state(null);

	// Review picker state
	let reviewPicker = $state(null); // { order, items: [{type, id, label, done}] }

	// Review modal state
	let reviewModal = $state(null);
	let reviewRating = $state(0);
	let reviewComment = $state('');
	let reviewSubmitting = $state(false);
	let reviewError = $state(null);
	let reviewSuccess = $state(false);

	onMount(async () => {
		try {
			orders = await getMyOrders();
		} catch {
			error = true;
		} finally {
			loading = false;
		}
	});

	async function acceptDelivery(orderId) {
		try {
			await apiFetch(`${API}/orders/${orderId}/accept-delivery`, { method: 'PATCH' });
			orders = orders.map((o) => (o.id === orderId ? { ...o, status: 'completed' } : o));
		} catch {
			// silently fail
		}
		acceptDialog = null;
	}

	function acceptAndReview(order) {
		acceptDialog = null;
		openReviewPicker(order);
	}

	function openReviewPicker(order) {
		// Build list of reviewable things
		const items = [
			{
				type: 'order',
				id: order.id,
				label: `Order experience at ${order.bakeryName ?? "Peelin' Good"}`,
				done: false
			},
			...(order.items ?? []).map((i) => ({
				type: 'product',
				id: i.productId,
				label: i.productName,
				done: false
			}))
		];
		reviewPicker = { order, items };
	}

	function openReviewFromPicker(pickerItem) {
		reviewModal = {
			type: pickerItem.type,
			orderId: reviewPicker.order.id,
			productId: pickerItem.id,
			label: pickerItem.label,
			pickerItemId: pickerItem.id,
			pickerItemType: pickerItem.type
		};
		reviewRating = 0;
		reviewComment = '';
		reviewError = null;
		reviewSuccess = false;
	}

	function closeModal() {
		reviewModal = null;
		reviewRating = 0;
		reviewComment = '';
		reviewError = null;
	}

	function closePicker() {
		reviewPicker = null;
	}

	async function submitReview() {
		if (reviewRating === 0) {
			reviewError = 'Please select a star rating.';
			return;
		}
		reviewSubmitting = true;
		reviewError = null;
		try {
			if (reviewModal.type === 'order') {
				const order = orders.find((o) => o.id === reviewModal.orderId);
				if (order && ['delivered', 'picked_up'].includes(order.status)) {
					await apiFetch(`${API}/orders/${reviewModal.orderId}/accept-delivery`, {
						method: 'PATCH'
					});
					orders = orders.map((o) =>
						o.id === reviewModal.orderId ? { ...o, status: 'completed' } : o
					);
				}
				await createOrderReview(reviewModal.orderId, reviewRating, reviewComment);
			} else {
				await createProductReview(reviewModal.productId, reviewRating, reviewComment);
			}

			reviewSuccess = true;

			// Mark item as done in picker
			if (reviewPicker) {
				reviewPicker = {
					...reviewPicker,
					items: reviewPicker.items.map((i) =>
						i.type === reviewModal.pickerItemType && i.id === reviewModal.pickerItemId
							? { ...i, done: true }
							: i
					)
				};
			}

			// If all items reviewed, close everything
			const allDone = reviewPicker?.items.every((i) => i.done);
			setTimeout(() => {
				closeModal();
				if (allDone) {
					// Mark order locally so button disappears
					orders = orders.map((o) =>
						o.id === reviewModal?.orderId ? { ...o, _allReviewed: true } : o
					);
					closePicker();
				}
			}, 1500);
		} catch (e) {
			reviewError = e.message ?? 'Failed to submit review. Please try again.';
		} finally {
			reviewSubmitting = false;
		}
	}

	function statusColor(status) {
		switch (status) {
			case 'pending':
				return 'secondary';
			case 'confirmed':
				return 'outline';
			case 'preparing':
				return 'outline';
			case 'ready':
				return 'default';
			case 'delivered':
			case 'picked_up':
			case 'completed':
				return 'default';
			case 'cancelled':
				return 'destructive';
			default:
				return 'secondary';
		}
	}

	function formatDate(dateStr) {
		if (!dateStr) return '—';
		return new Date(dateStr).toLocaleDateString('en-CA', {
			year: 'numeric',
			month: 'short',
			day: 'numeric'
		});
	}

	function formatPrice(amount) {
		if (amount == null) return '—';
		return `$${Number(amount).toFixed(2)}`;
	}
</script>

<div class="flex min-h-screen bg-background">
	<ProfileSidebar />

	<main class="flex-1 overflow-y-auto p-8 lg:p-10">
		<div class="mx-auto max-w-4xl space-y-6">
			<div>
				<h1 class="text-2xl font-bold tracking-tight text-foreground">Order History</h1>
				<p class="mt-1 text-sm text-muted-foreground">Your past and current orders</p>
			</div>

			{#if loading}
				<div class="space-y-4">
					{#each Array(3) as _item, i (i)}
						<Skeleton class="h-28 w-full rounded-xl" />
					{/each}
				</div>
			{:else if error}
				<p class="text-sm text-destructive">Failed to load orders. Please try again.</p>
			{:else if orders.length === 0}
				<div class="rounded-xl border border-border bg-card p-10 text-center">
					<p class="text-sm font-medium text-foreground">No orders yet</p>
					<p class="mt-1 text-sm text-muted-foreground">Place your first order to see it here.</p>
					<a
						href={resolve('/menu')}
						class="mt-4 inline-block text-sm font-semibold text-primary hover:underline"
					>
						Browse menu
					</a>
				</div>
			{:else}
				<div class="space-y-4">
					{#each orders as order (order.id)}
						<div class="rounded-xl border border-border bg-card p-5 shadow-sm">
							<div class="flex items-start justify-between gap-4">
								<div class="space-y-1">
									<p class="text-sm font-semibold text-foreground">
										Order #{order.orderNumber}
									</p>
									<p class="text-xs text-muted-foreground">
										{order.bakeryName ?? "Peelin' Good"} · {formatDate(order.placedAt)}
									</p>
									{#if order.items && order.items.length > 0}
										<p class="text-xs text-muted-foreground">
											{order.items.map((i) => i.productName).join(', ')}
										</p>
									{/if}
								</div>
								<div class="flex shrink-0 flex-col items-end gap-2">
									<Badge variant={statusColor(order.status)}>
										{order.status?.replace('_', ' ') ?? '—'}
									</Badge>
									<p class="text-sm font-bold text-foreground">
										{formatPrice(order.orderGrandTotal)}
									</p>
								</div>
							</div>

							<!-- Accept delivery button -->
							{#if ['delivered', 'picked_up'].includes(order.status)}
								<div class="mt-4 border-t border-border pt-4">
									<button
										onclick={() => (acceptDialog = { order })}
										class="w-full rounded-full bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground hover:opacity-90"
									>
										Accept Delivery
									</button>
								</div>
							{/if}

							<!-- Leave a review button for completed orders -->
							{#if order.status === 'completed' && !order._allReviewed}
								<div class="mt-4 border-t border-border pt-4">
									<button
										onclick={() => openReviewPicker(order)}
										class="w-full rounded-full border border-border px-4 py-2 text-sm font-semibold text-foreground hover:bg-muted"
									>
										Leave a Review
									</button>
								</div>
							{/if}
						</div>
					{/each}
				</div>
			{/if}
		</div>
	</main>
</div>

<!-- Accept Delivery Dialog -->
{#if acceptDialog}
	<div
		class="fixed inset-0 z-40 bg-black/50"
		onclick={() => (acceptDialog = null)}
		role="presentation"
	></div>
	<div class="fixed inset-0 z-50 flex items-center justify-center p-4">
		<div class="w-full max-w-sm rounded-2xl border border-border bg-card p-6 shadow-xl">
			<h2 class="text-lg font-bold text-foreground">Order Received?</h2>
			<p class="mt-1 text-sm text-muted-foreground">
				Confirm you received Order #{acceptDialog.order.orderNumber} from {acceptDialog.order
					.bakeryName ?? "Peelin' Good"}.
			</p>
			<div class="mt-5 flex flex-col gap-3">
				<button
					onclick={() => acceptAndReview(acceptDialog.order)}
					class="w-full rounded-full bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground hover:opacity-90"
				>
					Yes, leave a review
				</button>
				<button
					onclick={() => acceptDelivery(acceptDialog.order.id)}
					class="w-full rounded-full border border-border px-4 py-2 text-sm font-semibold text-foreground hover:bg-muted"
				>
					No, finish order
				</button>
				<button
					onclick={() => (acceptDialog = null)}
					class="w-full px-4 py-2 text-sm text-muted-foreground hover:text-foreground"
				>
					Cancel
				</button>
			</div>
		</div>
	</div>
{/if}

<!-- Review Picker -->
{#if reviewPicker && !reviewModal}
	<div class="fixed inset-0 z-40 bg-black/50" onclick={closePicker} role="presentation"></div>
	<div class="fixed inset-0 z-50 flex items-center justify-center p-4">
		<div class="w-full max-w-md rounded-2xl border border-border bg-card p-6 shadow-xl">
			<h2 class="text-lg font-bold text-foreground">What would you like to review?</h2>
			<p class="mt-1 text-sm text-muted-foreground">
				Order #{reviewPicker.order.orderNumber}
			</p>
			<div class="mt-5 space-y-2">
				{#each reviewPicker.items as item (item.id + item.type)}
					{#if !item.done}
						<button
							onclick={() => openReviewFromPicker(item)}
							class="flex w-full items-center justify-between rounded-xl border border-border px-4 py-3 text-left transition-colors hover:bg-muted"
						>
							<span class="text-sm font-medium text-foreground">{item.label}</span>
							<span class="text-xs text-primary">Review →</span>
						</button>
					{:else}
						<div
							class="flex w-full items-center justify-between rounded-xl border border-border bg-muted px-4 py-3 opacity-50"
						>
							<span class="text-sm font-medium text-foreground">{item.label}</span>
							<span class="text-xs text-green-600">✓ Reviewed</span>
						</div>
					{/if}
				{/each}
			</div>
			<button
				onclick={closePicker}
				class="mt-4 w-full px-4 py-2 text-sm text-muted-foreground hover:text-foreground"
			>
				Done
			</button>
		</div>
	</div>
{/if}

<!-- Review Modal -->
{#if reviewModal}
	<div class="fixed inset-0 z-40 bg-black/50" onclick={closeModal} role="presentation"></div>
	<div class="fixed inset-0 z-50 flex items-center justify-center p-4">
		<div class="w-full max-w-md rounded-2xl border border-border bg-card p-6 shadow-xl">
			<h2 class="text-lg font-bold text-foreground">
				{reviewModal.type === 'order' ? 'Review your order' : 'Review product'}
			</h2>
			<p class="mt-1 text-sm text-muted-foreground">{reviewModal.label}</p>

			<div class="mt-5 flex gap-2">
				{#each [1, 2, 3, 4, 5] as star (star)}
					<button
						onclick={() => (reviewRating = star)}
						class="text-2xl transition-transform hover:scale-110 {reviewRating >= star
							? 'text-yellow-400'
							: 'text-muted-foreground/30'}"
					>
						★
					</button>
				{/each}
			</div>

			<textarea
				bind:value={reviewComment}
				placeholder="Leave a comment (optional)"
				rows="3"
				class="mt-4 w-full resize-none rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
			></textarea>

			{#if reviewError}
				<p class="mt-2 text-xs text-destructive">{reviewError}</p>
			{/if}

			{#if reviewSuccess}
				<p class="mt-2 text-xs text-green-600">✓ Review submitted! It will appear once approved.</p>
			{/if}

			<div class="mt-4 flex justify-end gap-3">
				<button
					onclick={closeModal}
					class="rounded-full border border-border px-4 py-2 text-sm font-medium hover:bg-muted"
				>
					Back
				</button>
				<button
					onclick={submitReview}
					disabled={reviewSubmitting || reviewSuccess}
					class="rounded-full bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground hover:opacity-90 disabled:opacity-50"
				>
					{reviewSubmitting ? 'Submitting...' : 'Submit'}
				</button>
			</div>
		</div>
	</div>
{/if}
