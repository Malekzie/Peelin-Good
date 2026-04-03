<script>
	import ProductCard from '$lib/components/product/ProductCard.svelte';
	import { Skeleton } from '$lib/components/ui/skeleton';
	import { Separator } from '$lib/components/ui/separator';
	import { Input } from '$lib/components/ui/input';
	import { Button } from '$lib/components/ui/button';
	import { Sheet, SheetContent, SheetHeader, SheetTitle } from '$lib/components/ui/sheet';
	import { cart } from '$lib/stores/cart';
	import { getProducts } from '$lib/services/products';
	import { getTags } from '$lib/services/tags';
	import { Search, X, ShoppingBag, Plus, Minus, Check } from '@lucide/svelte';
	import { onMount } from 'svelte';

	let activeTagId = $state(null);
	let searchQuery = $state('');
	let products = $state([]);
	let tags = $state([]);
	let loading = $state(true);

	// Sheet state
	let sheetOpen = $state(false);
	let selectedProduct = $state(null);
	let sheetQty = $state(1);
	let sheetAdded = $state(false);

	function openSheet(product) {
		selectedProduct = product;
		sheetQty = 1;
		sheetAdded = false;
		sheetOpen = true;
	}

	function addSelectedToCart() {
		if (!selectedProduct) return;
		cart.addItem(selectedProduct, sheetQty);
		sheetAdded = true;
		sheetQty = 1;
		setTimeout(() => (sheetAdded = false), 1400);
	}

	const sheetPrice = $derived(
		selectedProduct
			? typeof selectedProduct.basePrice === 'number'
				? `$${selectedProduct.basePrice.toFixed(2)}`
				: `$${parseFloat(selectedProduct.basePrice).toFixed(2)}`
			: ''
	);

	onMount(async () => {
		try {
			[products, tags] = await Promise.all([getProducts(), getTags()]);
		} catch (e) {
			console.error('Failed to load menu:', e);
		} finally {
			loading = false;
		}
	});

	const filtered = $derived(
		products.filter((p) => {
			const matchesTag = activeTagId === null || p.tagIds?.includes(activeTagId);
			const q = searchQuery.toLowerCase();
			const matchesSearch =
				!q ||
				(p.name ?? '').toLowerCase().includes(q) ||
				(p.description ?? '').toLowerCase().includes(q);
			return matchesTag && matchesSearch;
		})
	);

	const activeTagName = $derived(tags.find((t) => t.id === activeTagId)?.name ?? null);
</script>

<div class="min-h-screen bg-[#FAF7F2]">

	<!-- Page header -->
	<header class="border-b border-border/60 bg-[#FAF7F2] px-6 pb-10 pt-14 text-center">
		<p class="mb-3 text-[11px] font-semibold uppercase tracking-[0.2em] text-[#C4714A]">
			Peelin' Good Bakery
		</p>
		<h1 class="text-5xl font-black tracking-tight text-[#2C1A0E] sm:text-6xl">Our Menu</h1>
		<p class="mt-3 text-sm text-muted-foreground">Fresh from the oven, crafted with care</p>

		<!-- Search -->
		<div class="relative mx-auto mt-8 max-w-md">
			<Search
				class="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground"
			/>
			<Input
				type="text"
				placeholder="Search breads, pastries, cakes..."
				bind:value={searchQuery}
				class="rounded-full bg-white pl-10 pr-10 shadow-sm focus-visible:ring-[#C4714A]"
			/>
			{#if searchQuery}
				<button
					onclick={() => (searchQuery = '')}
					class="absolute right-3.5 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
					aria-label="Clear search"
				>
					<X class="h-4 w-4" />
				</button>
			{/if}
		</div>
	</header>

	<!-- Body -->
	<div class="mx-auto flex max-w-7xl gap-0">

		<!-- Sidebar -->
		<aside class="hidden w-52 shrink-0 md:block">
			<div class="sticky top-6 px-4 py-8">
				<p class="mb-3 text-[10px] font-bold uppercase tracking-widest text-muted-foreground">
					Categories
				</p>
				<div class="flex flex-col gap-0.5">
					<button
						onclick={() => (activeTagId = null)}
						class="rounded-lg px-3 py-2 text-left text-sm font-medium transition-colors
							{activeTagId === null
							? 'bg-[#C4714A] text-white'
							: 'text-foreground/70 hover:bg-black/5 hover:text-foreground'}"
					>
						All items
					</button>
					{#each tags as tag (tag.id)}
						<button
							onclick={() => (activeTagId = tag.id)}
							class="rounded-lg px-3 py-2 text-left text-sm font-medium transition-colors
								{activeTagId === tag.id
								? 'bg-[#C4714A] text-white'
								: 'text-foreground/70 hover:bg-black/5 hover:text-foreground'}"
						>
							{tag.name}
						</button>
					{/each}
				</div>
			</div>
		</aside>

		<Separator orientation="vertical" class="hidden md:block" />

		<!-- Main -->
		<main class="flex-1 px-6 py-8">

			{#if activeTagName || searchQuery}
				<div class="mb-5 flex items-center gap-2 text-sm text-muted-foreground">
					<span>
						{filtered.length} result{filtered.length !== 1 ? 's' : ''}
						{#if activeTagName} in <span class="font-medium text-foreground">{activeTagName}</span>{/if}
						{#if searchQuery} for <span class="font-medium text-foreground">"{searchQuery}"</span>{/if}
					</span>
					<button
						onclick={() => { activeTagId = null; searchQuery = ''; }}
						class="ml-1 flex items-center gap-1 rounded-full border border-border px-2 py-0.5 text-xs hover:bg-muted"
					>
						<X class="h-3 w-3" /> Clear
					</button>
				</div>
			{/if}

			{#if loading}
				<div class="grid grid-cols-2 gap-5 lg:grid-cols-3">
					{#each Array(6) as _, i (i)}
						<div class="flex flex-col overflow-hidden rounded-xl border border-border bg-white">
							<Skeleton class="h-48 w-full rounded-none" />
							<div class="flex flex-col gap-3 p-4">
								<Skeleton class="h-4 w-3/4 rounded" />
								<Skeleton class="h-3 w-full rounded" />
								<Skeleton class="h-3 w-2/3 rounded" />
								<Skeleton class="h-5 w-16 rounded" />
								<Skeleton class="h-8 w-full rounded-full" />
							</div>
						</div>
					{/each}
				</div>

			{:else if filtered.length === 0}
				<div class="flex flex-col items-center justify-center py-24 text-center">
					<div class="flex h-16 w-16 items-center justify-center rounded-full bg-[#C4714A]/10"><Search class="h-7 w-7 text-[#C4714A]" /></div>
					<h2 class="mt-4 text-lg font-semibold text-foreground">Nothing found</h2>
					<p class="mt-1 text-sm text-muted-foreground">
						Try a different search or browse all categories.
					</p>
					<button
						onclick={() => { activeTagId = null; searchQuery = ''; }}
						class="mt-5 rounded-full bg-[#C4714A] px-5 py-2 text-sm font-semibold text-white hover:bg-[#C4714A]/90"
					>
						Show all items
					</button>
				</div>

			{:else}
				<div class="grid grid-cols-2 gap-5 lg:grid-cols-3">
					{#each filtered as product, i (product.id)}
						<div
							class="product-card"
							style="animation-delay: {Math.min(i * 50, 350)}ms"
						>
							<ProductCard {product} onselect={openSheet} />
						</div>
					{/each}
				</div>
			{/if}
		</main>
	</div>
</div>

<!-- Product detail sheet -->
<Sheet bind:open={sheetOpen}>
	<SheetContent side="right" class="flex w-full flex-col gap-0 overflow-y-auto p-0 sm:max-w-md">
		{#if selectedProduct}
			<!-- Image -->
			<div class="relative h-64 w-full shrink-0 bg-[#F5EFE6]">
				{#if selectedProduct.imageUrl}
					<img
						src={selectedProduct.imageUrl}
						alt={selectedProduct.name}
						class="h-full w-full object-cover"
					/>
				{:else}
					<div class="flex h-full w-full items-center justify-center">
						<ShoppingBag class="h-14 w-14 text-[#C4714A]/25" />
					</div>
				{/if}
			</div>

			<!-- Details -->
			<div class="flex flex-1 flex-col gap-5 p-6">
				<SheetHeader class="gap-1 text-left">
					<SheetTitle class="text-2xl font-bold text-[#2C1A0E]">
						{selectedProduct.name}
					</SheetTitle>
					<p class="text-xl font-bold text-[#C4714A]">{sheetPrice}</p>
				</SheetHeader>

				{#if selectedProduct.description}
					<p class="text-sm leading-relaxed text-muted-foreground">
						{selectedProduct.description}
					</p>
				{/if}

				<Separator />

				<!-- Quantity -->
				<div class="flex flex-col gap-2">
					<p class="text-xs font-semibold uppercase tracking-wider text-muted-foreground">Quantity</p>
					<div class="flex items-center gap-3">
						<div class="flex items-center rounded-full border border-border bg-background">
							<button
								onclick={() => { if (sheetQty > 1) sheetQty -= 1; }}
								class="flex h-9 w-9 items-center justify-center rounded-full transition-colors hover:bg-muted"
								aria-label="Decrease"
							>
								<Minus class="h-4 w-4" />
							</button>
							<span class="w-8 text-center text-sm font-semibold">{sheetQty}</span>
							<button
								onclick={() => sheetQty += 1}
								class="flex h-9 w-9 items-center justify-center rounded-full transition-colors hover:bg-muted"
								aria-label="Increase"
							>
								<Plus class="h-4 w-4" />
							</button>
						</div>
					</div>
				</div>

				<Button
					onclick={addSelectedToCart}
					class="mt-auto h-12 w-full gap-2 text-sm font-semibold transition-all duration-300
						{sheetAdded ? 'bg-[#8A9E7F] hover:bg-[#8A9E7F]' : 'bg-[#C4714A] hover:bg-[#C4714A]/90'}"
				>
					{#if sheetAdded}
						<Check class="h-4 w-4" />
						Added to cart
					{:else}
						<ShoppingBag class="h-4 w-4" />
						Add to cart — {sheetPrice}
					{/if}
				</Button>
			</div>
		{/if}
	</SheetContent>
</Sheet>

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
