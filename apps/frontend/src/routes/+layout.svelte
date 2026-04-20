<script lang="ts">
	import './layout.css';
	import { page } from '$app/state';
	import { navigating } from '$app/state';
	import favicon from '$lib/assets/favicon.svg';
	import Navbar from '$lib/components/layout/Navbar.svelte';
	import Footer from '$lib/components/layout/Footer.svelte';
	import { ModeWatcher } from 'mode-watcher';
	import { user } from '$lib/stores/authStore';
	import { cart, cartCount } from '$lib/stores/cart';
	import ChatWidget from '$lib/components/chat/ChatWidget.svelte';
	import { connectWs, disconnectWs } from '$lib/services/ws';

	let { children, data } = $props();
	const hideFooter = $derived(
		page.url.pathname.startsWith('/staff') || page.url.pathname.startsWith('/profile')
	);
	const canUseChat = $derived(data?.user?.role === 'customer');

	$effect(() => {
		cart.switchUser($user?.userId ?? null);
	});

	$effect(() => {
		if ($user) {
			connectWs();
		} else {
			disconnectWs();
		}
	});
</script>

<svelte:head><link rel="icon" href={favicon} /></svelte:head>
<ModeWatcher />

{#if navigating}
	<div
		class="fixed top-0 right-0 left-0 z-50 h-0.75 overflow-hidden"
		role="progressbar"
		aria-label="Page loading"
	>
		<div class="animate-nav-progress h-full w-full bg-[#C4714A]"></div>
	</div>
{/if}

<Navbar cartCount={$cartCount} />
{@render children()}
{#if !hideFooter}
	<Footer />
{/if}
{#if canUseChat}
	<ChatWidget />
{/if}
