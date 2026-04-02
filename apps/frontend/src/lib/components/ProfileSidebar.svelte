<script>
	import { page } from '$app/stores';
	import { goto } from '$app/navigation';
	import { clearAuth } from '$lib/stores/authStore';
	import { resolve } from '$app/paths';

	function handleLogout() {
		clearAuth();
		goto(resolve('/'));
	}
</script>

<aside
	class="hidden w-80 flex-col gap-4 border-r bg-[#fcf9f4] p-6 pt-24 text-sm font-medium md:flex dark:bg-stone-950"
>
	<div class="px-2">
		<div class="flex items-center space-x-4">
			<div
				class="flex h-11 w-11 items-center justify-center rounded-full bg-primary text-white shadow-md"
			>
				<span>Bakery</span>
			</div>
			<div>
				<h2 class="text-lg font-bold text-[#703210]">My Profile</h2>
				<p class="text-xs text-stone-500">Manage your bakery account</p>
			</div>
		</div>
	</div>

	<nav class="mt-4 flex-1 space-y-1">
		{#each [{ label: 'Profile', href: '/profile' }, { label: 'Orders', href: '/orders' }, { label: 'Preferences', href: '/profile/preferences' }] as link}
			<a
				href={link.href}
				class="flex items-center gap-3 rounded-full px-4 py-2.5 transition-colors
          {$page.url.pathname === link.href
					? 'bg-[#703210] text-white'
					: 'text-stone-600 hover:bg-[#8e4e14]/10'}"
			>
				<span>{link.label}</span>
			</a>
		{/each}
	</nav>

	<div class="mt-auto space-y-2 border-t pt-4">
		<button
			class="w-full rounded-full bg-primary py-2.5 text-sm font-semibold text-white hover:cursor-pointer hover:bg-primary/90"
		>
			New Order
		</button>
		<a
			// TODO add support link for customer service chat
			href="#"
			class="flex w-full items-center space-x-3 rounded-full px-4 py-2 text-stone-500 hover:bg-[#8e4e14]/10"
		>
			<span>Support</span>
		</a>
		<button
			onclick={handleLogout}
			class="flex w-full items-center space-x-3 rounded-full px-4 py-2 text-stone-500 hover:cursor-pointer hover:bg-[#8e4e14]/10"
		>
			<span>Logout</span>
		</button>
	</div>
</aside>
