<script lang="ts">
	import './layout.css';
	import { page } from '$app/state';
	import favicon from '$lib/assets/favicon.svg';
	import Navbar from '$lib/components/layout/Navbar.svelte';
	import Footer from '$lib/components/layout/Footer.svelte';
	import { ModeWatcher } from 'mode-watcher';
	import { user } from '$lib/stores/authStore';
	import { cart, cartCount } from '$lib/stores/cart';

	let { children } = $props();
	const hideFooter = $derived(
		page.url.pathname.startsWith('/staff') || page.url.pathname.startsWith('/profile')
	);

	$effect(() => {
		cart.switchUser($user?.userId ?? null);
	});
</script>

<svelte:head><link rel="icon" href={favicon} /></svelte:head>
<ModeWatcher />
<Navbar cartCount={$cartCount} />
{@render children()}
{#if !hideFooter}
	<Footer />
{/if}
