<script lang="ts">
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { ShoppingCart, User, ChevronDown, Menu, X } from '@lucide/svelte';
	import { isLoggedIn } from '$lib/stores/authStore';

	interface Props {
		cartCount?: number;
	}

	let { cartCount = 0 }: Props = $props();
	let menuOpen = $state(false);
	let categoryOpen = $state(false);

	const categories = ['Breads', 'Pastries', 'Cakes', 'Seasonal'];

	function handleClickOutside(e: MouseEvent) {
		const target = e.target as HTMLElement;
		if (!target.closest('.category-dropdown')) {
			categoryOpen = false;
		}
	}

	// handles where to direct user when clicking profile based on if they are logged in or not
	function handleProfileClick() {
		if ($isLoggedIn) {
			goto(resolve('/profile'));
		} else {
			goto(resolve('/login'));
		}
	}

	function handleMenuClick() {
		goto(resolve('/menu'));
	}
</script>

<svelte:window onclick={handleClickOutside} />

<nav class="sticky top-0 z-50 w-full border-b border-border bg-background/95 backdrop-blur">
	<div class="mx-auto flex max-w-7xl items-center justify-between px-6 py-4">
		<!-- Logo -->
		<a href={resolve('/')} class="font-serif text-xl font-bold tracking-tight text-foreground">
			<img src="/images/Peelin' Good.png" alt="Peelin' Good Logo" class="mr-1 inline h-10 w-15" />
		</a>

		<!-- Desktop Nav -->
		<div class="hidden items-center gap-8 md:flex">
			<!-- Menu -->
			<div class="relative">
				<button
					class="flex items-center gap-1 text-sm font-medium text-foreground transition-colors hover:cursor-pointer hover:text-primary"
					aria-expanded={categoryOpen}
					onclick={handleMenuClick}
				>
					Menu
				</button>
			</div>

			<a
				href={resolve('/about')}
				class="text-sm font-medium text-foreground transition-colors hover:text-primary">About</a
			>
			<!-- show order if user is logged in -->
			{#if $isLoggedIn}
				<a
					href={resolve('/order')}
					class="text-sm font-medium text-foreground transition-colors hover:text-primary">Order</a
				>
			{/if}
		</div>

		<!-- Right icons -->
		<div class="hidden items-center gap-4 md:flex">
			<button
				onclick={handleProfileClick}
				aria-label="Account"
				class="text-foreground transition-colors hover:cursor-pointer hover:text-primary"
			>
				<User size={20} />
			</button>
			<button
				aria-label="Cart ({cartCount} items)"
				class="relative text-foreground transition-colors hover:text-primary"
			>
				<ShoppingCart size={20} />
				{#if cartCount > 0}
					<span
						class="absolute -top-1.5 -right-1.5 flex h-4 w-4 items-center justify-center rounded-full bg-primary text-[10px] font-bold text-primary-foreground"
					>
						{cartCount}
					</span>
				{/if}
			</button>
		</div>

		<!-- Mobile hamburger -->
		<button
			aria-label={menuOpen ? 'Close menu' : 'Open menu'}
			class="text-foreground md:hidden"
			onclick={() => (menuOpen = !menuOpen)}
		>
			{#if menuOpen}
				<X size={22} />
			{:else}
				<Menu size={22} />
			{/if}
		</button>
	</div>

	<!-- Mobile menu -->
	{#if menuOpen}
		<div class="flex flex-col gap-4 border-t border-border bg-background px-6 py-4 md:hidden">
			<p class="text-xs font-semibold tracking-widest text-muted-foreground uppercase">Menu</p>

			<hr class="border-border" />
			<a href="/about" class="text-sm text-foreground hover:text-primary">About</a>
			<!-- show order if user is logged in -->
			{#if $isLoggedIn}
				<a href={resolve('/order')} class="text-sm text-foreground hover:text-primary">Order</a>
			{/if}
			<div class="flex gap-4 pt-2">
				<button
					onclick={handleProfileClick}
					aria-label="Account"
					class="text-foreground hover:text-primary"><User size={20} /></button
				>
				<button
					aria-label="Cart ({cartCount} items)"
					class="relative text-foreground hover:text-primary"
				>
					<ShoppingCart size={20} />
					{#if cartCount > 0}
						<span
							class="absolute -top-1.5 -right-1.5 flex h-4 w-4 items-center justify-center rounded-full bg-primary text-[10px] font-bold text-primary-foreground"
						>
							{cartCount}
						</span>
					{/if}
				</button>
			</div>
		</div>
	{/if}
</nav>
