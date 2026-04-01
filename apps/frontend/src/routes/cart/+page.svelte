<script lang="ts">
	import { resolve } from '$app/paths';
	import { cart } from '$lib/stores/cart';
	import { ShoppingCart, Trash2, Plus, Minus } from '@lucide/svelte';
</script>

<main class="mx-auto max-w-4xl px-6 py-16">
	<h1 class="font-serif text-4xl font-bold text-foreground mb-10">Your Cart</h1>

	{#if $cart.items.length === 0}
		<div class="flex flex-col items-center gap-6 py-20 text-center">
			<ShoppingCart size={48} class="text-muted-foreground" />
			<p class="text-lg text-muted-foreground">Your cart is empty.</p>
			<a
				href={resolve('/')}
				class="rounded-lg bg-primary px-6 py-3 text-sm font-semibold text-primary-foreground transition-colors hover:opacity-90"
			>
				Browse the Menu
			</a>
		</div>
	{:else}
		<div class="flex flex-col gap-4">
			{#each $cart.items as item (item.productId)}
				<div
					class="flex items-center gap-4 rounded-xl border border-border bg-card p-4 shadow-sm"
				>
					{#if item.productImageUrl}
						<img
							src={item.productImageUrl}
							alt={item.productName}
							class="h-20 w-20 rounded-lg object-cover flex-shrink-0"
						/>
					{:else}
						<div
							class="flex h-20 w-20 flex-shrink-0 items-center justify-center rounded-lg bg-muted text-3xl"
						>
							🥐
						</div>
					{/if}

					<div class="flex-1 min-w-0">
						<p class="font-semibold text-foreground truncate">{item.productName}</p>
						<p class="text-sm text-muted-foreground">${item.unitPrice.toFixed(2)} each</p>
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
						${item.lineTotal.toFixed(2)}
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
					<span>${$cart.subtotal.toFixed(2)}</span>
				</div>
				{#if $cart.discount > 0}
					<div class="flex justify-between text-accent">
						<span>Discount</span>
						<span>−${$cart.discount.toFixed(2)}</span>
					</div>
				{/if}
				<hr class="my-2 border-border" />
				<div class="flex justify-between text-base font-bold text-foreground">
					<span>Total</span>
					<span>${$cart.total.toFixed(2)}</span>
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
					href={resolve('/checkout')}
					class="rounded-lg bg-primary px-6 py-3 text-center text-sm font-semibold text-primary-foreground transition-colors hover:opacity-90"
				>
					Proceed to Checkout
				</a>
			</div>
		</div>
	{/if}
</main>
