<script>
	import { page } from '$app/stores';
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { logoutUser } from '$lib/services/auth';
	import { user } from '$lib/stores/authStore';
	import { Button } from '$lib/components/ui/button';
	import { Separator } from '$lib/components/ui/separator';
	import { Avatar, AvatarFallback } from '$lib/components/ui/avatar';
	import {
		User,
		ShoppingBag,
		SlidersHorizontal,
		HelpCircle,
		LogOut,
		ShoppingCart
	} from '@lucide/svelte';

	const navLinks = [
		{ label: 'Profile', href: '/profile', icon: User },
		{ label: 'Orders', href: '/orders', icon: ShoppingBag },
		{ label: 'Preferences', href: '/profile/preferences', icon: SlidersHorizontal }
	];

	const initials = $derived([$user?.username?.[0]].filter(Boolean).join('').toUpperCase() || '?');

	async function handleLogout() {
		await logoutUser();
		goto(resolve('/'));
	}
</script>

<aside class="hidden w-72 flex-col border-r border-border bg-card md:flex">
	<div class="flex flex-col gap-6 p-6 pt-8">
		<!-- User identity -->
		<div class="flex items-center gap-3">
			<Avatar class="h-10 w-10">
				<AvatarFallback class="bg-primary text-sm font-semibold text-primary-foreground">
					{initials}
				</AvatarFallback>
			</Avatar>
			<div class="min-w-0">
				<p class="truncate text-sm font-semibold text-foreground">{$user?.username ?? 'Account'}</p>
				<p class="text-xs text-muted-foreground capitalize">{$user?.role?.toLowerCase() ?? ''}</p>
			</div>
		</div>

		<Separator />

		<!-- Nav -->
		<nav class="flex flex-col gap-1">
			{#each navLinks as link (link.href)}
				{@const active = $page.url.pathname === link.href}
				<a
					href={resolve(link.href)}
					class="flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors
						{active
						? 'bg-primary text-primary-foreground'
						: 'text-muted-foreground hover:bg-muted hover:text-foreground'}"
				>
					<link.icon class="h-4 w-4 shrink-0" />
					{link.label}
				</a>
			{/each}
		</nav>
	</div>

	<!-- Bottom actions -->
	<div class="mt-auto flex flex-col gap-2 border-t border-border p-6">
		<Button href={resolve('/')} class="w-full gap-2">
			<ShoppingCart class="h-4 w-4" />
			New Order
		</Button>
		<Button variant="ghost" class="w-full justify-start gap-2 text-muted-foreground">
			<HelpCircle class="h-4 w-4" />
			Support
		</Button>
		<Button
			variant="ghost"
			onclick={handleLogout}
			class="w-full justify-start gap-2 text-muted-foreground hover:text-destructive"
		>
			<LogOut class="h-4 w-4" />
			Log out
		</Button>
	</div>
</aside>
