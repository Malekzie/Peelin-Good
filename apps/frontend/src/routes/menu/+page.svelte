<script>
	import ProductCard from '$lib/components/product/ProductCard.svelte';
	import MenuHeader from '$lib/components/menu/MenuHeader.svelte';
	import MenuSidebarFilters from '$lib/components/menu/MenuSidebarFilters.svelte';
	import MenuResultsSummary from '$lib/components/menu/MenuResultsSummary.svelte';
	import MenuLoadingGrid from '$lib/components/menu/MenuLoadingGrid.svelte';
	import MenuEmptyState from '$lib/components/menu/MenuEmptyState.svelte';
	import MenuProductSheet from '$lib/components/menu/MenuProductSheet.svelte';
	import MenuReviewModal from '$lib/components/menu/MenuReviewModal.svelte';
	import ReviewSubmissionOverlay from '$lib/components/review/ReviewSubmissionOverlay.svelte';
	import { Separator } from '$lib/components/ui/separator';
	import { cart } from '$lib/stores/cart';
	import { user } from '$lib/stores/authStore';
	import {
		filterMenuProducts,
		loadMenuCatalog,
		loadMenuProductReviews,
		resolveInitialMenuState,
		submitMenuProductReview
	} from '$lib/services/menu';
	import { getTodaySpecial } from '$lib/services/product-specials';
	import { goto } from '$app/navigation';
	import { page } from '$app/stores';
	import { resolve } from '$app/paths';
	import { onMount } from 'svelte';

	let activeTagId = $state(null);
	let searchQuery = $state('');
	let products = $state([]);
	let tags = $state([]);
	let loading = $state(true);
	let productReviews = $state([]);
	let reviewsLoading = $state(false);

	let reviewModal = $state(false);
	let reviewRating = $state(0);
	let reviewComment = $state('');
	let reviewSubmitting = $state(false);
	let reviewError = $state(null);
	let reviewSuccess = $state(false);
	let reviewGuestName = $state('');

	let sheetOpen = $state(false);
	let selectedProduct = $state(null);
	let sheetQty = $state(1);
	let sheetAdded = $state(false);
	let showAllReviews = $state(false);

	let todaySpecial = $state(null);

	async function openSheet(product) {
		selectedProduct = product;
		sheetQty = 1;
		sheetAdded = false;
		sheetOpen = true;
		productReviews = [];
		reviewsLoading = true;
		showAllReviews = false;

		try {
			productReviews = await loadMenuProductReviews(product.id);
		} catch {
			productReviews = [];
		} finally {
			reviewsLoading = false;
		}
	}

	function openReviewModal() {
		reviewRating = 0;
		reviewComment = '';
		reviewGuestName = '';
		reviewError = null;
		reviewSuccess = false;
		reviewModal = true;
		sheetOpen = false;
	}

	function closeReviewModal() {
		reviewModal = false;
		reviewGuestName = '';
	}

	async function submitProductReview() {
		if (reviewRating === 0 || !selectedProduct) {
			reviewError = 'Please select a star rating.';
			return;
		}

		reviewSubmitting = true;
		reviewError = null;
		reviewSuccess = false;

		try {
			const result = await submitMenuProductReview({
				productId: selectedProduct.id,
				rating: reviewRating,
				comment: reviewComment,
				guestName: reviewGuestName || ''
			});

			if (result.ok) {
				reviewSuccess = true;
				productReviews = await loadMenuProductReviews(selectedProduct.id);
				setTimeout(() => closeReviewModal(), 1500);
			} else {
				reviewError = result.error;
			}
		} catch (error) {
			reviewError = error?.message ?? 'Failed to submit review.';
		} finally {
			reviewSubmitting = false;
		}
	}

	function addSelectedToCart() {
		if (!selectedProduct) return;

		const isSpecial = todaySpecial?.productId === selectedProduct.id;
		const discountPercent = isSpecial ? todaySpecial.discountPercent : null;
		const originalPrice = isSpecial ? selectedProduct.basePrice : undefined;
		const unitPrice =
			isSpecial && discountPercent
				? +(selectedProduct.basePrice * (1 - discountPercent / 100)).toFixed(2)
				: selectedProduct.basePrice;

		cart.addItem({
			productId: selectedProduct.id,
			productName: selectedProduct.name,
			productImageUrl: selectedProduct.imageUrl ?? null,
			unitPrice,
			originalPrice,
			quantity: sheetQty
		});
		sheetAdded = true;
		sheetQty = 1;
		setTimeout(() => (sheetAdded = false), 1400);
	}

	onMount(async () => {
		try {
			[[products, tags], todaySpecial] = await Promise.all([
				loadMenuCatalog(),
				getTodaySpecial().catch(() => null)
			]);

			const initialState = resolveInitialMenuState($page.url, tags);
			activeTagId = initialState.activeTagId;
			searchQuery = initialState.searchQuery;

			const productParam = initialState.productId;
			if (productParam) {
				const product = products.find((item) => String(item.id) === productParam);
				if (product) openSheet(product);
			}
		} catch (error) {
			console.error('Failed to load menu:', error);
		} finally {
			loading = false;
		}
	});

	const filtered = $derived(filterMenuProducts(products, activeTagId, searchQuery));

	const activeTagName = $derived(tags.find((tag) => tag.id === activeTagId)?.name ?? null);
</script>

<div class="min-h-screen bg-background">
	<MenuHeader bind:activeTagId bind:searchQuery {tags} />

	<div class="mx-auto flex max-w-7xl gap-0">
		<MenuSidebarFilters bind:activeTagId {tags} />
		<Separator orientation="vertical" class="hidden md:block" />

		<main class="flex-1 px-6 py-8">
			<MenuResultsSummary
				resultCount={filtered.length}
				{activeTagName}
				{searchQuery}
				onClear={() => {
					activeTagId = null;
					searchQuery = '';
				}}
			/>

			{#if loading}
				<MenuLoadingGrid />
			{:else if filtered.length === 0}
				<MenuEmptyState
					onReset={() => {
						activeTagId = null;
						searchQuery = '';
					}}
				/>
			{:else}
				<div class="grid grid-cols-2 gap-3 sm:gap-5 lg:grid-cols-3">
					{#each filtered as product, i (product.id)}
						<div class="product-card" style="animation-delay: {Math.min(i * 50, 350)}ms">
							<ProductCard
								{product}
								onselect={openSheet}
								isSpecial={todaySpecial?.productId === product.id}
								specialDiscount={todaySpecial?.productId === product.id
									? todaySpecial.discountPercent
									: null}
							/>
						</div>
					{/each}
				</div>
			{/if}
		</main>
	</div>
</div>

<MenuProductSheet
	bind:open={sheetOpen}
	product={selectedProduct}
	{tags}
	{productReviews}
	{reviewsLoading}
	bind:sheetQty
	{sheetAdded}
	bind:showAllReviews
	isSpecial={todaySpecial != null &&
		selectedProduct != null &&
		todaySpecial.productId === selectedProduct.id}
	specialDiscount={todaySpecial != null &&
	selectedProduct != null &&
	todaySpecial.productId === selectedProduct.id
		? todaySpecial.discountPercent
		: null}
	onOpenReviewModal={openReviewModal}
	onAddToCart={addSelectedToCart}
/>

<MenuReviewModal
	bind:open={reviewModal}
	productName={selectedProduct?.name ?? ''}
	isLoggedIn={Boolean($user)}
	bind:rating={reviewRating}
	bind:comment={reviewComment}
	bind:guestName={reviewGuestName}
	bind:submitting={reviewSubmitting}
	bind:error={reviewError}
	bind:success={reviewSuccess}
	onClose={closeReviewModal}
	onSubmit={submitProductReview}
/>

<ReviewSubmissionOverlay visible={reviewSubmitting} />

<style>
	.product-card {
		animation: fadeUp 0.4s ease both;
	}

	@keyframes fadeUp {
		from {
			opacity: 0;
			transform: translateY(14px);
		}

		to {
			opacity: 1;
			transform: translateY(0);
		}
	}
</style>
