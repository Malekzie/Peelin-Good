<script>
	import ProfileSidebar from '$lib/components/profile/ProfileSidebar.svelte';
	import { Skeleton } from '$lib/components/ui/skeleton';
	import { Badge } from '$lib/components/ui/badge';
	import { onMount } from 'svelte';
	import { getMyOrders } from '$lib/services/orders';
	import { getProducts } from '$lib/services/products';
	import { createProductReview, createOrderReview } from '$lib/services/review';
	import ReviewSubmissionOverlay from '$lib/components/review/ReviewSubmissionOverlay.svelte';
	import { apiFetch } from '$lib/utils/api';
	import { truncateModerationMessage } from '$lib/utils/reviewMessage';
	import { resolve } from '$app/paths';
	import { formatPriceCad } from '$lib/utils/money';
	import { ChevronDown, ShoppingBag } from '@lucide/svelte';

	const API = '/api/v1';

	let orders = $state([]);
	let productImages = $state({});
	let loading = $state(true);
	let error = $state(null);
	let openOrders = $state(new Set());

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
	/** @type {'success' | 'rejected' | 'pending'} */
	let reviewOutcome = $state('success');

	let toastMessage = $state(null);
	let toastClear = 0;

	function showToast(msg) {
		if (!msg?.trim()) return;
		clearTimeout(toastClear);
		toastMessage = msg.trim();
		toastClear = setTimeout(() => {
			toastMessage = null;
		}, 6500);
	}

	async function refreshOrdersList() {
		try {
			const fresh = await getMyOrders();
			if (fresh) orders = fresh;
		} catch {
			/* keep existing list */
		}
	}

	onMount(async () => {
		try {
			const [ordersData, productsData] = await Promise.all([getMyOrders(), getProducts()]);
			orders = ordersData ?? [];
			const map = {};
			for (const p of productsData ?? []) {
				map[p.id] = p.imageUrl ?? null;
			}
			productImages = map;
		} catch {
			error = true;
		} finally {
			loading = false;
		}
	});

	function toggle(orderId) {
		const next = new Set(openOrders);
		if (next.has(orderId)) {
			next.delete(orderId);
		} else {
			next.add(orderId);
		}
		openOrders = next;
	}

	/** @param {any} order */
	function orderHasAnyReviewableSlot(order) {
		if (!order || order.status !== 'completed') return false;
		const locDone = order.locationReviewSubmitted === true;
		const lineItems = order.items ?? [];
		if (!locDone) return true;
		if (lineItems.length === 0) return false;
		return lineItems.some((i) => i.productReviewSubmitted !== true);
	}

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
		const items = [
			{
				type: 'order',
				id: order.id,
				label: `Order experience at ${order.bakeryName ?? "Peelin' Good"}`,
				done: order.locationReviewSubmitted === true,
				failed: false
			},
			...(order.items ?? []).map((i) => ({
				type: 'product',
				id: i.productId,
				label: i.productName,
				done: i.productReviewSubmitted === true,
				failed: false
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
		reviewOutcome = 'success';
	}

	function closeModal() {
		reviewModal = null;
		reviewRating = 0;
		reviewComment = '';
		reviewError = null;
		reviewOutcome = 'success';
	}

	function closePicker() {
		reviewPicker = null;
	}

	async function submitReview() {
		if (reviewSubmitting) return;
		const activeModal = reviewModal;
		if (!activeModal) return;
		if (reviewRating === 0) {
			reviewError = 'Please select a star rating.';
			return;
		}
		reviewSubmitting = true;
		reviewError = null;
		reviewSuccess = false;
		const pickerType = activeModal.pickerItemType;
		const pickerId = activeModal.pickerItemId;
		try {
			/** @type {{ status?: string; moderationMessage?: string } | undefined} */
			let submitted;
			if (activeModal.type === 'order') {
				const order = orders.find((o) => o.id === activeModal.orderId);
				if (order && ['delivered', 'picked_up'].includes(order.status)) {
					await apiFetch(`${API}/orders/${activeModal.orderId}/accept-delivery`, {
						method: 'PATCH'
					});
					orders = orders.map((o) =>
						o.id === activeModal.orderId ? { ...o, status: 'completed' } : o
					);
				}
				submitted = await createOrderReview(activeModal.orderId, reviewRating, reviewComment);
			} else {
				submitted = await createProductReview(
					activeModal.productId,
					reviewRating,
					reviewComment,
					activeModal.orderId
				);
			}

			const st = (submitted?.status ?? '').toLowerCase();
			if (st === 'rejected') {
				const shortReason = truncateModerationMessage(submitted.moderationMessage);
				showToast(shortReason ? `Couldn't post review: ${shortReason}` : "Couldn't post review.");
				if (reviewPicker) {
					reviewPicker = {
						...reviewPicker,
						items: reviewPicker.items.map((i) =>
							i.type === pickerType && i.id === pickerId ? { ...i, done: true, failed: true } : i
						)
					};
				}
				closeModal();
				closePicker();
				await refreshOrdersList();
				return;
			}

			reviewSuccess = true;
			reviewOutcome = st === 'pending' ? 'pending' : 'success';
			showToast(
				reviewOutcome === 'success'
					? 'Thanks, your review was posted.'
					: 'Your review was submitted and is pending approval.'
			);

			if (reviewPicker) {
				reviewPicker = {
					...reviewPicker,
					items: reviewPicker.items.map((i) =>
						i.type === pickerType && i.id === pickerId ? { ...i, done: true } : i
					)
				};
			}

			const allDone = reviewPicker?.items.every((i) => i.done);
			setTimeout(async () => {
				closeModal();
				if (allDone) closePicker();
				await refreshOrdersList();
			}, 1500);
		} catch (e) {
			const msg = e?.message ?? 'Failed to submit review. Please try again.';
			const status = e?.status;
			if (status === 409) {
				showToast(msg.length > 120 ? `${msg.slice(0, 117)}…` : msg);
				reviewError = msg;
			} else {
				reviewError = msg;
			}
		} finally {
			reviewSubmitting = false;
		}
	}

	function statusColor(status) {
		switch (status) {
			case 'pending_payment':
				return 'secondary';
			case 'paid':
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
		return formatPriceCad(amount);
	}
</script>

<div
	class="flex h-[calc(100dvh-var(--app-navbar-height))] flex-col overflow-hidden bg-background md:flex-row"
>
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
						<div class="overflow-hidden rounded-xl border border-border bg-card shadow-sm">
							<!-- Accordion header -->
							<button
								type="button"
								onclick={() => toggle(order.id)}
								class="w-full px-5 py-4 text-left transition-colors hover:bg-muted/40"
							>
								<div class="flex items-center justify-between gap-4">
									<div class="min-w-0 space-y-0.5">
										<p class="text-sm font-semibold text-foreground">
											Order #{order.orderNumber}
										</p>
										<p class="text-xs text-muted-foreground">
											{order.bakeryName ?? "Peelin' Good"} · {formatDate(order.placedAt)}
										</p>
									</div>
									<div class="flex shrink-0 items-center gap-3">
										<div class="flex flex-col items-end gap-1">
											<Badge variant={statusColor(order.status)}>
												{order.status?.replace(/_/g, ' ') ?? '—'}
											</Badge>
											<p class="text-sm font-bold text-foreground">
												{formatPrice(order.orderGrandTotal)}
											</p>
										</div>
										<ChevronDown
											class="h-4 w-4 shrink-0 text-muted-foreground transition-transform duration-200
												{openOrders.has(order.id) ? 'rotate-180' : ''}"
										/>
									</div>
								</div>
							</button>

							<!-- Accordion body -->
							{#if openOrders.has(order.id)}
								<div class="border-t border-border px-5 pt-4 pb-5">
									{#if order.items && order.items.length > 0}
										<div class="mb-4 flex flex-col gap-2">
											{#each order.items as item (item.id)}
												<a
													href={resolve(`/menu?product=${item.productId}`)}
													class="flex items-center gap-3 rounded-lg border border-border bg-background px-3 py-2 transition-colors hover:bg-muted/60"
												>
													{#if productImages[item.productId]}
														<img
															src={productImages[item.productId]}
															alt={item.productName}
															class="h-12 w-12 shrink-0 rounded-md object-cover"
														/>
													{:else}
														<div
															class="flex h-12 w-12 shrink-0 items-center justify-center rounded-md bg-[#F5EFE6]"
														>
															<ShoppingBag class="h-5 w-5 text-[#C25F1A]/40" />
														</div>
													{/if}
													<div class="min-w-0">
														<p class="truncate text-sm font-medium text-foreground">
															{item.productName}
														</p>
														<p class="text-xs text-muted-foreground">
															Qty {item.quantity} · {formatPrice(item.lineTotal)}
														</p>
													</div>
												</a>
											{/each}
										</div>
									{/if}

									<a
										href={resolve(`/orders/${order.orderNumber}`)}
										class="inline-flex items-center gap-1 text-xs font-semibold text-primary hover:underline"
									>
										View tracking →
									</a>

									{#if ['delivered', 'picked_up'].includes(order.status)}
										<div class="mt-4 border-t border-border pt-4">
											<button
												onclick={() => (acceptDialog = { order })}
												class="rounded-full bg-primary px-6 py-2 text-xs font-semibold text-primary-foreground hover:opacity-90"
											>
												Accept Delivery
											</button>
										</div>
									{/if}

									{#if orderHasAnyReviewableSlot(order)}
										<div class="mt-4 flex items-center gap-3 border-t border-border pt-4">
											<button
												onclick={() => openReviewPicker(order)}
												class="rounded-full border border-border px-4 py-2 text-sm font-semibold text-foreground hover:bg-muted"
											>
												Leave a Review
											</button>
											<button
												onclick={() => toggle(order.id)}
												class="rounded-full bg-[#C25F1A] px-4 py-2 text-sm font-semibold text-white hover:bg-[#C25F1A]/90"
											>
												Done
											</button>
										</div>
									{/if}
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
					{:else if item.failed}
						<div
							class="flex w-full items-center justify-between rounded-xl border border-amber-200 bg-amber-50/80 px-4 py-3"
						>
							<span class="text-sm font-medium text-foreground">{item.label}</span>
							<span class="text-xs font-medium text-amber-800">Not posted</span>
						</div>
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

{#if toastMessage}
	<div
		class="fixed bottom-6 left-1/2 z-[200] max-w-md -translate-x-1/2 rounded-xl border border-border bg-card px-4 py-3 text-sm text-foreground shadow-lg"
		role="status"
	>
		{toastMessage}
	</div>
{/if}

<ReviewSubmissionOverlay visible={reviewSubmitting} />

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
				disabled={reviewSubmitting}
				class="mt-4 w-full resize-none rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
			></textarea>

			{#if reviewSubmitting}
				<div class="mt-3 flex items-center gap-2 text-xs text-muted-foreground">
					<span
						class="inline-block size-3 animate-spin rounded-full border-2 border-muted-foreground/30 border-t-primary"
						aria-hidden="true"
					></span>
					<span>Moderating your review... this can take a few seconds.</span>
				</div>
			{/if}

			{#if reviewError}
				<p class="mt-2 text-xs text-destructive">{reviewError}</p>
			{/if}

			{#if reviewSuccess}
				<p
					class="mt-2 text-xs {reviewOutcome === 'success'
						? 'text-green-600'
						: reviewOutcome === 'rejected'
							? 'text-destructive'
							: 'text-muted-foreground'}"
				>
					{#if reviewOutcome === 'success'}
						✓ Thanks! Your review was posted.
					{:else if reviewOutcome === 'rejected'}
						We couldn’t post that review. Try different wording.
					{:else}
						Your review is being checked and will appear if approved.
					{/if}
				</p>
			{/if}

			<div class="mt-4 flex justify-end gap-3">
				<button
					onclick={closeModal}
					disabled={reviewSubmitting}
					class="rounded-full border border-border px-4 py-2 text-sm font-medium hover:bg-muted disabled:cursor-not-allowed disabled:opacity-50"
				>
					Back
				</button>
				<button
					onclick={submitReview}
					disabled={reviewSubmitting}
					class="rounded-full bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground hover:opacity-90 disabled:opacity-50"
				>
					{reviewSubmitting ? 'Submitting...' : 'Submit'}
				</button>
			</div>
		</div>
	</div>
{/if}
