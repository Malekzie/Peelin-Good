<script>
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { user } from '$lib/stores/authStore';
	import { Skeleton } from '$lib/components/ui/skeleton';
	import { Badge } from '$lib/components/ui/badge';
	import { Button } from '$lib/components/ui/button';
	import { listUsers, setUserActive } from '$lib/services/staff-users.js';

	let users = $state([]);
	let loading = $state(true);
	let error = $state(null);
	let toggling = $state({});

	onMount(async () => {
		if ($user?.role !== 'admin') {
			goto(resolve('/staff/dashboard'), { replaceState: true });
			return;
		}
		try {
			users = await listUsers();
		} catch {
			error = true;
		} finally {
			loading = false;
		}
	});

	async function handleToggle(u) {
		toggling[u.id] = true;
		try {
			const updated = await setUserActive(u.id, !u.active);
			users = users.map((x) => (x.id === u.id ? updated : x));
		} catch {
			// leave unchanged
		} finally {
			toggling[u.id] = false;
		}
	}
</script>

<main class="flex-1 overflow-y-auto p-8 lg:p-10">
	<div class="mx-auto max-w-4xl space-y-6">
		<div>
			<h1 class="text-2xl font-bold tracking-tight text-foreground">Users</h1>
			<p class="mt-1 text-sm text-muted-foreground">
				All registered accounts — enable or disable access
			</p>
		</div>

		{#if loading}
			<div class="space-y-3">
				{#each Array(6) as _, i (i)}
					<Skeleton class="h-14 rounded-xl" />
				{/each}
			</div>
		{:else if error}
			<p class="text-sm text-destructive">Failed to load users.</p>
		{:else if users.length === 0}
			<div class="rounded-xl border border-border bg-card p-10 text-center">
				<p class="text-sm text-muted-foreground">No users found.</p>
			</div>
		{:else}
			<div class="rounded-xl border border-border bg-card">
				<div class="divide-y divide-border">
					{#each users as u (u.id)}
						<div class="flex items-center justify-between px-5 py-3">
							<div>
								<p class="text-sm font-medium text-foreground">{u.username}</p>
								<p class="text-xs text-muted-foreground">
									{u.email} · <span class="capitalize">{u.role}</span>
								</p>
							</div>
							<div class="flex items-center gap-3">
								<Badge variant={u.active ? 'default' : 'secondary'}>
									{u.active ? 'Active' : 'Disabled'}
								</Badge>
								<Button
									size="sm"
									variant={u.active ? 'destructive' : 'outline'}
									onclick={() => handleToggle(u)}
									disabled={!!toggling[u.id]}
								>
									{toggling[u.id] ? '...' : u.active ? 'Disable' : 'Enable'}
								</Button>
							</div>
						</div>
					{/each}
				</div>
			</div>
		{/if}
	</div>
</main>
