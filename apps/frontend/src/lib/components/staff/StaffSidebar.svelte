<script>
	import { page } from '$app/state';
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { user } from '$lib/stores/authStore';
	import { logoutUser } from '$lib/services/auth';
	import { Avatar, AvatarFallback } from '$lib/components/ui/avatar';
	import { Separator } from '$lib/components/ui/separator';
	import { Button } from '$lib/components/ui/button';
	import {
		LayoutDashboard,
		ShoppingBag,
		Star,
		Users,
		BarChart2,
		Package,
		UserCog,
		Shield,
		User,
		LogOut
	} from '@lucide/svelte';

	const allNavLinks = [
		{ label: 'Dashboard', href: '/staff/dashboard', icon: LayoutDashboard, roles: null },
		{ label: 'Orders', href: '/staff/orders', icon: ShoppingBag, roles: null },
		{ label: 'Customers', href: '/staff/customers', icon: Users, roles: null },
		{ label: 'Analytics', href: '/staff/analytics', icon: BarChart2, roles: ['admin'] },
		{ label: 'Products', href: '/staff/products', icon: Package, roles: ['admin'] },
		{ label: 'Employees', href: '/staff/staff', icon: UserCog, roles: ['admin'] },
		{ label: 'Users', href: '/staff/users', icon: Shield, roles: ['admin'] },
		{ label: 'My Profile', href: '/staff/profile', icon: User, roles: null }
	];

	const navLinks = $derived(
		allNavLinks.filter((l) => l.roles === null || l.roles.includes($user?.role ?? ''))
	);

	const initials = $derived(($user?.username?.[0] ?? '?').toUpperCase());

	async function handleLogout() {
		await logoutUser();
		goto(resolve('/'));
	}
</script>

<!-- Mobile top nav -->
<nav
	class="flex items-center gap-1 overflow-x-auto border-b border-border bg-card px-4 py-2 md:hidden"
	style="scrollbar-width: none;"
>
	{#each navLinks as link (link.href)}
		{@const active =
			page.url.pathname === link.href || page.url.pathname.startsWith(link.href + '/')}
		<a
			href={resolve(link.href)}
			class="flex shrink-0 items-center gap-2 rounded-full px-4 py-2 text-sm font-medium transition-colors
				{active
				? 'bg-primary text-primary-foreground'
				: 'text-muted-foreground hover:bg-muted hover:text-foreground'}"
		>
			<link.icon class="h-4 w-4 shrink-0" />
			{link.label}
		</a>
	{/each}
	<button
		onclick={handleLogout}
		class="flex shrink-0 items-center gap-2 rounded-full px-4 py-2 text-sm font-medium text-muted-foreground transition-colors hover:bg-muted hover:text-destructive"
	>
		<LogOut class="h-4 w-4 shrink-0" />
		Log out
	</button>
</nav>

<aside
	class="hidden h-full w-64 shrink-0 flex-col overflow-y-auto border-r border-border bg-card md:flex"
>
	<div class="flex flex-col gap-6 p-6 pt-8">
		<div class="flex items-center gap-3">
			<Avatar class="h-10 w-10">
				<AvatarFallback class="bg-primary text-sm font-semibold text-primary-foreground">
					{initials}
				</AvatarFallback>
			</Avatar>
			<div class="min-w-0">
				<p class="truncate text-sm font-semibold text-foreground">{$user?.username ?? 'Staff'}</p>
				<p class="text-xs text-muted-foreground capitalize">{$user?.role ?? ''}</p>
			</div>
		</div>

		<Separator />

		<nav class="flex flex-col gap-1">
			{#each navLinks as link (link.href)}
				{@const active =
					page.url.pathname === link.href || page.url.pathname.startsWith(link.href + '/')}
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

	<div class="mt-auto border-t border-border p-6">
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
