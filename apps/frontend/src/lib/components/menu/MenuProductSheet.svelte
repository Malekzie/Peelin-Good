<script>
	import { Sheet, SheetContent, SheetHeader, SheetTitle } from '$lib/components/ui/sheet';
	import { Separator } from '$lib/components/ui/separator';
	import { Button } from '$lib/components/ui/button';
	import { Skeleton } from '$lib/components/ui/skeleton';
	import { ShoppingBag, Plus, Minus, Check } from '@lucide/svelte';
	import { formatPriceCad } from '$lib/utils/money';

	let reviewFilter = $state('all');

	let {
		open = $bindable(false),
		product = null,
		tags = [],
		productReviews = [],
		reviewsLoading = false,
		sheetQty = $bindable(1),
		sheetAdded = false,
		showAllReviews = $bindable(false),
		isSpecial = false,
		specialDiscount = null,
		onOpenReviewModal = () => {},
		onAddToCart = () => {}
	} = $props();

	const filteredReviews = $derived(
		reviewFilter === 'verified'
			? productReviews.filter((r) => r.verifiedAccount)
			: reviewFilter === 'purchased'
				? productReviews.filter((r) => r.verifiedPurchase)
				: productReviews
	);

	const sheetPrice = $derived(
		product
			? formatPriceCad(
					isSpecial && specialDiscount
						? parseFloat(product.basePrice) * (1 - specialDiscount / 100)
						: typeof product.basePrice === 'number'
							? product.basePrice
							: parseFloat(product.basePrice)
				)
			: ''
	);
</script>

<Sheet bind:open>
	<SheetContent side="right" class="flex w-full flex-col gap-0 overflow-y-auto p-0 sm:max-w-md">
		{#if product}
			<div class="relative h-64 w-full shrink-0 bg-muted">
				{#if product.imageUrl}
					<img src={product.imageUrl} alt={product.name} class="h-full w-full object-cover" />
				{:else}
					<div class="flex h-full w-full items-center justify-center">
						<ShoppingBag class="h-14 w-14 text-primary/25" />
					</div>
				{/if}
			</div>

			<div class="flex flex-1 flex-col gap-5 p-6">
				<SheetHeader class="gap-1 text-left">
					<SheetTitle class="text-2xl font-bold text-foreground">{product.name}</SheetTitle>
					{#if isSpecial && specialDiscount}
						<div class="flex items-center gap-2">
							<p class="text-xl font-bold text-primary">{sheetPrice}</p>
							<p class="text-sm text-muted-foreground line-through">
								{formatPriceCad(product.basePrice)}
							</p>
						</div>
					{:else}
						<p class="text-xl font-bold text-primary">{sheetPrice}</p>
					{/if}
				</SheetHeader>

				{#if product.description}
					<p class="text-sm leading-relaxed text-muted-foreground">{product.description}</p>
				{/if}

				{#if product.tagIds && product.tagIds.length > 0}
					<div class="flex flex-wrap gap-2">
						{#each product.tagIds as tagId (tagId)}
							{@const tagName = tags.find((t) => t.id === tagId)?.name}
							{#if tagName}
								<span class="rounded-full bg-muted px-3 py-1 text-xs font-semibold text-primary">
									{tagName}
								</span>
							{/if}
						{/each}
					</div>
				{/if}

				<Separator />

				{#if reviewsLoading}
					<div class="space-y-2">
						<Skeleton class="h-4 w-24 rounded" />
						<Skeleton class="h-12 w-full rounded" />
					</div>
				{:else if productReviews.length > 0}
					<div class="space-y-3">
						<div class="flex items-center justify-between">
							<p class="text-xs font-semibold tracking-wider text-muted-foreground uppercase">
								Customer Reviews
							</p>
							<div class="flex gap-1">
								{#each [['all', 'All'], ['verified', 'Verified'], ['purchased', 'Purchased']] as [val, label] (val)}
									<button
										type="button"
										onclick={() => (reviewFilter = val)}
										class="rounded-full px-2 py-0.5 text-[10px] font-semibold transition-colors {reviewFilter ===
										val
											? 'bg-primary text-primary-foreground'
											: 'bg-muted text-muted-foreground hover:bg-muted/80'}"
									>
										{label}
									</button>
								{/each}
							</div>
						</div>
						{#each showAllReviews ? filteredReviews : filteredReviews.slice(0, 3) as review (review.id)}
							<div class="rounded-lg bg-muted/50 px-3 py-2">
								<div class="flex items-center justify-between">
									<div class="flex items-center gap-2">
										<p class="text-xs font-semibold text-foreground">
											{review.reviewerDisplayName}
										</p>
										{#if review.verifiedAccount}
											<span
												class="rounded-full bg-emerald-100 px-1.5 py-0.5 text-[9px] font-semibold text-emerald-800"
												>Verified</span
											>
										{/if}
										{#if review.verifiedPurchase}
											<span
												class="rounded-full bg-amber-100 px-1.5 py-0.5 text-[9px] font-semibold text-amber-700"
												>Purchased</span
											>
										{/if}
									</div>
									<p class="text-xs text-yellow-500">
										{'★'.repeat(review.rating)}{'☆'.repeat(5 - review.rating)}
									</p>
								</div>
								{#if review.comment}
									<p class="mt-1 text-xs text-muted-foreground">{review.comment}</p>
								{/if}
							</div>
						{/each}
						{#if filteredReviews.length > 3}
							<button
								onclick={() => (showAllReviews = !showAllReviews)}
								class="text-xs font-semibold text-primary hover:underline"
							>
								{showAllReviews ? 'Show less' : `See all ${filteredReviews.length} reviews`}
							</button>
						{/if}
					</div>
				{/if}

				<button
					onclick={onOpenReviewModal}
					class="text-sm font-semibold text-primary hover:underline"
				>
					Leave a Review
				</button>

				<Separator />

				<div class="flex flex-col gap-2">
					<p class="text-xs font-semibold tracking-wider text-muted-foreground uppercase">
						Quantity
					</p>
					<div class="flex items-center gap-3">
						<div class="flex items-center rounded-full border border-border bg-background">
							<button
								onclick={() => {
									if (sheetQty > 1) sheetQty -= 1;
								}}
								class="flex h-9 w-9 items-center justify-center rounded-full transition-colors hover:bg-muted"
								aria-label="Decrease"
							>
								<Minus class="h-4 w-4" />
							</button>
							<span class="w-8 text-center text-sm font-semibold">{sheetQty}</span>
							<button
								onclick={() => (sheetQty += 1)}
								class="flex h-9 w-9 items-center justify-center rounded-full transition-colors hover:bg-muted"
								aria-label="Increase"
							>
								<Plus class="h-4 w-4" />
							</button>
						</div>
					</div>
				</div>

				<Button
					onclick={onAddToCart}
					class="mt-auto h-12 w-full gap-2 text-sm font-semibold transition-all duration-300 {sheetAdded
						? 'bg-emerald-600 hover:bg-emerald-600'
						: 'bg-primary hover:bg-primary/90'}"
				>
					{#if sheetAdded}
						<Check class="h-4 w-4" />
						Added to cart
					{:else}
						<ShoppingBag class="h-4 w-4" />
						Add to cart: {sheetPrice}
					{/if}
				</Button>
			</div>
		{/if}
	</SheetContent>
</Sheet>
