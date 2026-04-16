<script lang="ts">
	import { onMount } from 'svelte';
	import { resolve } from '$app/paths';
	import { Button } from '$lib/components/ui/button/index';
	import { Skeleton } from '$lib/components/ui/skeleton';
	import SpecialCard from '$lib/components/product/SpecialCard.svelte';
	import { getTodaySpecial } from '$lib/services/product-specials';
	import { getProductById } from '$lib/services/products';

	type Special = {
		productSpecialId: number;
		productId: number;
		productName: string;
		productDescription: string | null;
		productBasePrice: number;
		discountPercent: number | null;
		productImageUrl: string | null;
	};

	let special = $state<Special | null>(null);
	let loading = $state(true);

	function localDateIso() {
		const d = new Date();
		const y = d.getFullYear();
		const m = String(d.getMonth() + 1).padStart(2, '0');
		const day = String(d.getDate()).padStart(2, '0');
		return `${y}-${m}-${day}`;
	}

	onMount(async () => {
		try {
			const today = await getTodaySpecial(localDateIso());
			const pid = today?.productId;
			if (pid == null) {
				special = null;
				return;
			}
			const product = await getProductById(pid);
			const pct = today.discountPercent != null ? Number(today.discountPercent) : null;
			special = {
				productSpecialId: pid,
				productId: product.id,
				productName: product.name,
				productDescription: product.description,
				productBasePrice: product.basePrice,
				discountPercent: pct,
				productImageUrl: product.imageUrl
			};
		} catch {
			special = null;
		} finally {
			loading = false;
		}
	});
</script>

<section class="grid min-h-125 grid-cols-1 bg-background md:grid-cols-2">
	<!-- Text -->
	<div class="flex flex-col justify-center gap-5 px-8 py-16 md:px-12 lg:px-16">
		<img
			src="/images/Peelin' Good ~BAKERY~.png"
			alt="Peelin' Good Bakery"
			class="w-64 self-center object-contain"
		/>
		<h1 class="text-5xl leading-[1.08] font-black tracking-tight text-foreground lg:text-6xl">
			Made from<br />scratch.<br />
			<span class="text-primary">Always.</span>
		</h1>
		<p class="max-w-sm text-[15px] leading-relaxed text-muted-foreground">
			Bread, pastries, and cakes baked in small batches every morning, with local ingredients and
			nothing to hide.
		</p>
		<div class="flex gap-3 pt-2">
			<Button
				href={resolve('/menu')}
				class="rounded-full bg-primary px-6 py-3 text-sm font-semibold text-primary-foreground transition-opacity hover:opacity-90"
				size="default"
				variant="outline"
			>
				Browse the menu
			</Button>
			<Button
				href={resolve('/about')}
				class="rounded-full border border-border px-6 py-3 text-sm font-medium text-foreground transition-colors hover:bg-muted"
				variant="outline"
			>
				Our story
			</Button>
		</div>
	</div>

	<!-- Today's Special panel -->
	<div class="relative hidden bg-muted/40 md:block">
		<div class="relative flex h-full flex-col justify-center gap-6 px-10 py-16 lg:px-14">
			<div>
				<p class="text-[11px] font-semibold tracking-[0.25em] text-primary uppercase">
					Out of the oven
				</p>
				<h2 class="mt-1 text-3xl font-black tracking-tight text-foreground lg:text-4xl">
					Today's special
				</h2>
				<p class="mt-1 text-sm text-muted-foreground">Our featured product for today.</p>
			</div>

			{#if loading}
				<Skeleton class="mx-auto h-64 w-full max-w-sm rounded-2xl" />
			{:else if special}
				<div class="mx-auto w-full max-w-sm">
					<SpecialCard
						name={special.productName}
						description={special.productDescription}
						price={special.productBasePrice}
						discountPercent={special.discountPercent}
						imageUrl={special.productImageUrl}
						productId={special.productId}
					/>
				</div>
			{:else}
				<p class="text-sm text-muted-foreground">
					No special is available today. Check back another day!
				</p>
				<div>
					<a
						href={resolve('/menu')}
						class="inline-block rounded-full bg-primary px-4 py-2 text-xs font-bold tracking-widest text-primary-foreground uppercase"
					>
						Browse menu
					</a>
				</div>
			{/if}
		</div>
	</div>
</section>
