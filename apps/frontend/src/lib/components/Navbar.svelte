<script lang="ts">
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { ShoppingCart, User, ChevronDown, Menu, X } from '@lucide/svelte';
	import Cookies from 'js-cookie';

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

	function handleProfileClick() {
		const loggedIn = !!Cookies.get('loggedIn');

		if (loggedIn) {
			goto(resolve('/profile'));
		} else {
			goto(resolve('/login'));
		}
	}
</script>

<svelte:window onclick={handleClickOutside} />

<nav class="sticky top-0 z-50 w-full border-b border-border bg-background/95 backdrop-blur">
	<div class="mx-auto flex max-w-7xl items-center justify-between px-6 py-4">
		<!-- Logo -->
		<a href={resolve('/')} class="font-serif text-xl font-bold tracking-tight text-foreground">
			Peelin' Good
		</a>

		<!-- Desktop Nav -->
		<div class="hidden items-center gap-8 md:flex">
			<!-- Menu dropdown -->
			<div class="category-dropdown relative">
				<button
					class="flex items-center gap-1 text-sm font-medium text-foreground transition-colors hover:text-primary"
					aria-expanded={categoryOpen}
					onclick={(e) => {
						e.stopPropagation();
						categoryOpen = !categoryOpen;
					}}
				>
					Menu
					<ChevronDown
						size={14}
						class={categoryOpen ? 'rotate-180 transition-transform' : 'transition-transform'}
					/>
				</button>

				{#if categoryOpen}
					<div
						class="absolute top-full left-0 mt-2 w-40 rounded-lg border border-border bg-background py-1 shadow-lg"
					>
						{#each categories as cat (cat)}
							<a
								href={resolve(`/menu/${cat.toLowerCase()}`)}
								class="block px-4 py-2 text-sm text-foreground transition-colors hover:bg-muted hover:text-primary"
								onclick={() => (categoryOpen = false)}
							>
								{cat}
							</a>
						{/each}
					</div>
				{/if}
			</div>

			<a
				href={resolve('/about')}
				class="text-sm font-medium text-foreground transition-colors hover:text-primary">About</a
			>
			<a
				href={resolve('/order')}
				class="text-sm font-medium text-foreground transition-colors hover:text-primary">Order</a
			>
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
			{#each categories as cat (cat)}
				<a
					href={resolve(`/menu/${cat.toLowerCase()}`)}
					class="text-sm text-foreground hover:text-primary">{cat}</a
				>
			{/each}
			<hr class="border-border" />
			<!-- <a href="/about" class="text-sm text-foreground hover:text-primary">About</a> -->
			<!-- <a href="/order" class="text-sm text-foreground hover:text-primary">Order</a> -->
			<div class="flex gap-4 pt-2">
				<button aria-label="Account" class="text-foreground hover:text-primary"
					><User size={20} /></button
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
