<script>
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { user } from '$lib/stores/authStore';
	import { Skeleton } from '$lib/components/ui/skeleton';
	import { Badge } from '$lib/components/ui/badge';
	import { listStaff } from '$lib/services/staff-employees.js';

	let employees = $state([]);
	let loading = $state(true);
	let error = $state(null);

	onMount(async () => {
		if ($user?.role !== 'admin') {
			goto(resolve('/staff/dashboard'), { replaceState: true });
			return;
		}
		try {
			employees = await listStaff();
		} catch {
			error = true;
		} finally {
			loading = false;
		}
	});
</script>

<main class="flex-1 overflow-y-auto p-8 lg:p-10">
	<div class="mx-auto max-w-5xl space-y-6">
		<div>
			<h1 class="text-2xl font-bold tracking-tight text-foreground">Employees</h1>
			<p class="mt-1 text-sm text-muted-foreground">Staff accounts and assignments</p>
		</div>

		{#if loading}
			<div class="space-y-3">
				{#each Array(4) as _, i (i)}
					<Skeleton class="h-16 rounded-xl" />
				{/each}
			</div>
		{:else if error}
			<p class="text-sm text-destructive">Failed to load employees.</p>
		{:else if employees.length === 0}
			<div class="rounded-xl border border-border bg-card p-10 text-center">
				<p class="text-sm text-muted-foreground">No employees found.</p>
			</div>
		{:else}
			<div class="rounded-xl border border-border bg-card">
				<div class="divide-y divide-border">
					{#each employees as emp (emp.id)}
						<div class="flex items-center justify-between gap-3 px-5 py-4">
							<div class="min-w-0 space-y-0.5">
								<p class="truncate text-sm font-medium text-foreground">
									{emp.firstName}
									{emp.lastName}
								</p>
								<p class="truncate text-xs text-muted-foreground">
									{emp.workEmail ?? '—'}
								</p>
							</div>
							<div class="flex shrink-0 items-center gap-3">
								{#if emp.position}
									<Badge variant="outline">{emp.position}</Badge>
								{/if}
							</div>
						</div>
					{/each}
				</div>
			</div>
		{/if}
	</div>
</main>
