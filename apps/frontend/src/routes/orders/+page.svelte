<script>
	import { onMount } from 'svelte';
	import { SvelteSet } from 'svelte/reactivity';
	import { getMyOrders } from '$lib/services/orders';
	import { getProducts } from '$lib/services/products';
	import { createProductReview, createOrderReview } from '$lib/services/review';
	import ReviewSubmissionOverlay from '$lib/components/review/ReviewSubmissionOverlay.svelte';
	import { apiFetch } from '$lib/utils/api';
	import { truncateModerationMessage } from '$lib/utils/reviewMessage';
	import { buildReviewableItems } from '$lib/utils/OrdersHelper';
	import OrdersList from '$lib/components/orders/OrdersList.svelte';
	import OrderAcceptDialog from '$lib/components/orders/OrderAcceptDialog.svelte';
	import OrderReviewPicker from '$lib/components/orders/OrderReviewPicker.svelte';
	import OrderReviewForm from '$lib/components/orders/OrderReviewForm.svelte';
	import Toast from '$lib/components/orders/Toast.svelte';

	const API = '/api/v1';

	// Data state
	let orders = $state([]);
	let productImages = $state({});
	let loading = $state(true);
	let error = $state(false);

	// Accept delivery dialog state
	let acceptDialog = $state(null);

	// Review picker state
	let reviewPicker = $state(null); // { order, items: [{type, id, label, done, failed}] }

	// Review modal state
	let reviewModal = $state(null);
	let reviewRating = $state(0);
	let reviewComment = $state('');
	let reviewSubmitting = $state(false);
	let reviewError = $state(null);
	let reviewSuccess = $state(false);
	/** @type {'success' | 'pending'} */
	let reviewOutcome = $state('success');

	// Toast state
	let toastMessage = $state(null);
	let toastClear = 0;

	// Accordion state
	const openOrders = new SvelteSet();

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
				map[String(p.id)] = p.imageUrl ?? null;
			}
			productImages = map;
		} catch (e) {
			console.error('[orders] failed to load:', e);
			error = true;
		} finally {
			loading = false;
		}
	});

	function toggleAccordion(orderId) {
		if (openOrders.has(orderId)) {
			openOrders.delete(orderId);
		} else {
			openOrders.add(orderId);
		}
	}

	async function acceptDeliveryHandler(orderId) {
		try {
			await apiFetch(`${API}/orders/${orderId}/accept-delivery`, { method: 'PATCH' });
			orders = orders.map((o) => (o.id === orderId ? { ...o, status: 'completed' } : o));
		} catch {
			// silently fail
		}
		acceptDialog = null;
	}

	function acceptAndReviewHandler(order) {
		acceptDialog = null;
		openReviewPickerHandler(order);
	}

	function openReviewPickerHandler(order) {
		reviewPicker = { order, items: buildReviewableItems(order) };
	}

	function openReviewFromPickerHandler(pickerItem) {
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

	function closeReviewModal() {
		reviewModal = null;
		reviewRating = 0;
		reviewComment = '';
		reviewError = null;
		reviewOutcome = 'success';
	}

	function closeReviewPicker() {
		reviewPicker = null;
	}

	async function submitReviewHandler() {
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
				closeReviewModal();
				closeReviewPicker();
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
				closeReviewModal();
				if (allDone) closeReviewPicker();
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
</script>

<OrdersList
	{orders}
	{loading}
	{error}
	{productImages}
	{openOrders}
	onToggleOrder={toggleAccordion}
	onAcceptDelivery={(order) => (acceptDialog = { order })}
	onLeaveReview={openReviewPickerHandler}
/>

<!-- Accept Delivery Dialog -->
<OrderAcceptDialog
	dialog={acceptDialog}
	onClose={() => (acceptDialog = null)}
	onAccept={acceptDeliveryHandler}
	onAcceptAndReview={acceptAndReviewHandler}
/>

<!-- Review Picker Modal -->
<OrderReviewPicker
	picker={reviewPicker}
	onSelectItem={openReviewFromPickerHandler}
	onClose={closeReviewPicker}
/>

<!-- Review Form Modal -->
<OrderReviewForm
	modal={reviewModal}
	rating={reviewRating}
	comment={reviewComment}
	submitting={reviewSubmitting}
	error={reviewError}
	success={reviewSuccess}
	outcome={reviewOutcome}
	onClose={closeReviewModal}
	onSubmit={submitReviewHandler}
/>

<!-- Toast Notification -->
<Toast message={toastMessage} />

<!-- Submission Overlay (moderation animation) -->
<ReviewSubmissionOverlay visible={reviewSubmitting} />
