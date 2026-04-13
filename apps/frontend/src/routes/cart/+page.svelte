<script lang="ts">
	import { resolve } from '$app/paths';
	import { cart } from '$lib/stores/cart';
	import { formatDiscountCad, formatPriceCad } from '$lib/utils/money';
	import { ShoppingCart, Trash2, Plus, Minus } from '@lucide/svelte';
	import { user } from '$lib/stores/authStore';

	let { data } = $props();
	// data.user comes from the JWT cookie (server-side). Fall back to the localStorage
	// store in case the cookie isn't readable by SvelteKit (e.g. OAuth flow, cookie path).

	const isUserSignedIn = $derived($user || data.user);

	const checkoutHref = $derived(isUserSignedIn ? '/checkout' : '/checkout/guest');
</script>

<main class="mx-auto max-w-4xl px-6 py-16">
	<h1 class="mb-10 font-serif text-4xl font-bold text-foreground">Your Cart</h1>

	{#if $cart.items.length === 0}
		<div class="flex flex-col items-center gap-6 py-20 text-center">
			<ShoppingCart size={48} class="text-muted-foreground" />
			<p class="text-lg text-muted-foreground">Your cart is empty.</p>
			<a
				href={resolve('/menu')}
				class="rounded-lg bg-primary px-6 py-3 text-sm font-semibold text-primary-foreground transition-colors hover:opacity-90"
			>
				Browse the Menu
			</a>
		</div>
	{:else}
		<div class="flex flex-col gap-4">
			{#each $cart.items as item (item.productId)}
				<div class="flex items-center gap-4 rounded-xl border border-border bg-card p-4 shadow-sm">
					{#if item.productImageUrl}
						<img
							src={item.productImageUrl}
							alt={item.productName}
							class="h-20 w-20 shrink-0 rounded-lg object-cover"
						/>
					{:else}
						<div
							class="flex h-20 w-20 shrink-0 items-center justify-center rounded-lg bg-muted text-3xl"
						>
							🥐
						</div>
					{/if}

					<div class="min-w-0 flex-1">
						<p class="truncate font-semibold text-foreground">{item.productName}</p>
						<p class="text-sm text-muted-foreground">{formatPriceCad(item.unitPrice)} each</p>
					</div>

					<div class="flex items-center gap-2">
						<button
							aria-label="Decrease quantity"
							class="flex h-8 w-8 items-center justify-center rounded-full border border-border text-foreground transition-colors hover:bg-muted"
							onclick={() => cart.updateQuantity(item.productId, item.quantity - 1)}
						>
							<Minus size={14} />
						</button>
						<span class="w-6 text-center text-sm font-medium">{item.quantity}</span>
						<button
							aria-label="Increase quantity"
							class="flex h-8 w-8 items-center justify-center rounded-full border border-border text-foreground transition-colors hover:bg-muted"
							onclick={() => cart.updateQuantity(item.productId, item.quantity + 1)}
						>
							<Plus size={14} />
						</button>
					</div>

					<p class="w-20 text-right font-semibold text-foreground">
						{formatPriceCad(item.lineTotal)}
					</p>

					<button
						aria-label="Remove {item.productName}"
						class="text-muted-foreground transition-colors hover:text-destructive"
						onclick={() => cart.removeItem(item.productId)}
					>
						<Trash2 size={18} />
					</button>
				</div>
			{/each}
		</div>

		<div class="mt-8 rounded-xl border border-border bg-card p-6 shadow-sm">
			<div class="flex flex-col gap-2 text-sm">
				<div class="flex justify-between text-muted-foreground">
					<span>Subtotal</span>
					<span>{formatPriceCad($cart.subtotal)}</span>
				</div>
				{#if $cart.discount > 0}
					<div class="flex justify-between text-accent">
						<span>Discount</span>
						<span>{formatDiscountCad($cart.discount)}</span>
					</div>
				{/if}
				<hr class="my-2 border-border" />
				<div class="flex justify-between text-base font-bold text-foreground">
					<span>Total</span>
					<span>{formatPriceCad($cart.total)}</span>
				</div>
			</div>

			<div class="mt-6 flex flex-col gap-3 sm:flex-row sm:justify-end">
				<a
					href={resolve('/')}
					class="rounded-lg border border-border px-6 py-3 text-center text-sm font-medium text-foreground transition-colors hover:bg-muted"
				>
					Continue Shopping
				</a>
				<a
					href={resolve(checkoutHref)}
					class="rounded-lg bg-primary px-6 py-3 text-center text-sm font-semibold text-primary-foreground transition-colors hover:opacity-90"
				>
					Proceed to Checkout
				</a>
			</div>
		</div>
	{/if}
</main>
