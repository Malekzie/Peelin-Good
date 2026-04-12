<script>
	import { onMount } from 'svelte';
	import { resolve } from '$app/paths';
	import { Skeleton } from '$lib/components/ui/skeleton';
	import { Input } from '$lib/components/ui/input';
	import { Button } from '$lib/components/ui/button';
	import { Badge } from '$lib/components/ui/badge';
	import {
		listCustomers,
		getPendingPhotos,
		approvePhoto,
		rejectPhoto,
		patchCustomer
	} from '$lib/services/staff-customers.js';

	let tab = $state('all');
	let customers = $state([]);
	let pendingPhotos = $state([]);
	let search = $state('');
	let loading = $state(true);
	let error = $state(null);
	let actioning = $state({});

	onMount(async () => {
		try {
			[customers, pendingPhotos] = await Promise.all([listCustomers(), getPendingPhotos()]);
		} catch {
			error = true;
		} finally {
			loading = false;
		}
	});

	async function handleSearch() {
		loading = true;
		try {
			customers = await listCustomers(search);
		} catch {
			error = true;
		} finally {
			loading = false;
		}
	}

	async function handleApprove(id) {
		actioning[id] = 'approve';
		try {
			await approvePhoto(id);
			pendingPhotos = pendingPhotos.filter((c) => c.id !== id);
		} finally {
			actioning[id] = null;
		}
	}

	async function handleReject(id) {
		actioning[id] = 'reject';
		try {
			await rejectPhoto(id);
			pendingPhotos = pendingPhotos.filter((c) => c.id !== id);
		} finally {
			actioning[id] = null;
		}
	}

	const filteredCustomers = $derived(
		search
			? customers.filter(
					(c) =>
						c.firstName?.toLowerCase().includes(search.toLowerCase()) ||
						c.lastName?.toLowerCase().includes(search.toLowerCase()) ||
						c.email?.toLowerCase().includes(search.toLowerCase())
				)
			: customers
	);
</script>

<main class="flex-1 overflow-y-auto p-8 lg:p-10">
	<div class="mx-auto max-w-5xl space-y-6">
		<div class="flex items-center justify-between">
			<div>
				<h1 class="text-2xl font-bold tracking-tight text-foreground">Customers</h1>
				<p class="mt-1 text-sm text-muted-foreground">Manage customer accounts and photos</p>
			</div>
			{#if pendingPhotos.length > 0}
				<Badge variant="destructive">{pendingPhotos.length} photos pending</Badge>
			{/if}
		</div>

		<!-- Tabs -->
		<div class="flex gap-1 border-b border-border">
			{#each [['all', 'All Customers'], ['photos', 'Pending Photos']] as [key, label] (key)}
				<button
					onclick={() => (tab = key)}
					class="px-4 py-2 text-sm font-medium transition-colors
						{tab === key
						? 'border-b-2 border-primary text-foreground'
						: 'text-muted-foreground hover:text-foreground'}"
				>
					{label}
					{#if key === 'photos' && pendingPhotos.length > 0}
						<span class="ml-1 rounded-full bg-destructive px-1.5 py-0.5 text-xs text-white">
							{pendingPhotos.length}
						</span>
					{/if}
				</button>
			{/each}
		</div>

		{#if loading}
			<div class="space-y-3">
				{#each Array(5) as _, i (i)}
					<Skeleton class="h-16 rounded-xl" />
				{/each}
			</div>
		{:else if error}
			<p class="text-sm text-destructive">Failed to load customers.</p>
		{:else if tab === 'all'}
			<div class="flex gap-3">
				<Input
					placeholder="Search by name or email..."
					bind:value={search}
					class="max-w-sm"
					oninput={handleSearch}
				/>
			</div>

			<div class="rounded-xl border border-border bg-card">
				<div class="divide-y divide-border">
					{#if filteredCustomers.length === 0}
						<p class="px-6 py-8 text-center text-sm text-muted-foreground">No customers found</p>
					{:else}
						{#each filteredCustomers as c (c.id)}
							<div class="flex items-center justify-between gap-3 px-5 py-3">
								<div class="min-w-0">
									<p class="truncate text-sm font-medium text-foreground">
										{c.firstName ?? ''}
										{c.lastName ?? ''}
									</p>
									<p class="truncate text-xs text-muted-foreground">{c.email ?? '—'}</p>
								</div>
								<div class="flex shrink-0 items-center gap-3">
									{#if c.photoApprovalPending}
										<Badge variant="destructive" class="text-xs">Photo pending</Badge>
									{/if}
									<a
										href={resolve(`/staff/customers/${c.id}`)}
										class="text-xs font-medium text-primary hover:underline"
									>
										View
									</a>
								</div>
							</div>
						{/each}
					{/if}
				</div>
			</div>
		{:else if pendingPhotos.length === 0}
			<div class="rounded-xl border border-border bg-card p-10 text-center">
				<p class="text-sm text-muted-foreground">No pending photos</p>
			</div>
		{:else}
			<div class="space-y-3">
				{#each pendingPhotos as c (c.id)}
					<div
						class="flex flex-col gap-3 rounded-xl border border-border bg-card px-5 py-4 sm:flex-row sm:items-center sm:justify-between"
					>
						<div class="flex items-center gap-4">
							{#if c.profilePhotoPath}
								<img
									src={c.profilePhotoPath}
									alt="Profile"
									class="h-12 w-12 rounded-full border border-border object-cover"
								/>
							{/if}
							<div>
								<p class="text-sm font-medium text-foreground">
									{c.firstName ?? ''}
									{c.lastName ?? ''}
								</p>
								<p class="text-xs text-muted-foreground">{c.email ?? '—'}</p>
							</div>
						</div>
						<div class="flex gap-2">
							<Button
								size="sm"
								variant="outline"
								onclick={() => handleApprove(c.id)}
								disabled={!!actioning[c.id]}
							>
								{actioning[c.id] === 'approve' ? '...' : 'Approve'}
							</Button>
							<Button
								size="sm"
								variant="destructive"
								onclick={() => handleReject(c.id)}
								disabled={!!actioning[c.id]}
							>
								{actioning[c.id] === 'reject' ? '...' : 'Reject'}
							</Button>
						</div>
					</div>
				{/each}
			</div>
		{/if}
	</div>
</main>
