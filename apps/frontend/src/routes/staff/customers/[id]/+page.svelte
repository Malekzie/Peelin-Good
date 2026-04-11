<script>
	import { onMount } from 'svelte';
	import { page } from '$app/state';
	import { resolve } from '$app/paths';
	import { user } from '$lib/stores/authStore';
	import { Skeleton } from '$lib/components/ui/skeleton';
	import { Button } from '$lib/components/ui/button';
	import { Input } from '$lib/components/ui/input';
	import { Separator } from '$lib/components/ui/separator';
	import {
		getCustomer,
		patchCustomer,
		approvePhoto,
		rejectPhoto
	} from '$lib/services/staff-customers.js';
	import { listUsers, setUserActive } from '$lib/services/staff-users.js';

	const id = page.params.id;
	const isAdmin = $derived($user?.role === 'admin');

	let customer = $state(null);
	let loading = $state(true);
	let error = $state(null);
	let editing = $state(false);
	let saving = $state(false);
	let draft = $state({});
	let confirmingDeactivate = $state(false);

	onMount(async () => {
		try {
			const [customerData, users] = await Promise.all([getCustomer(id), listUsers()]);
			customer = customerData;
			const linkedUser = users.find((u) => String(u.id) === String(customer.userId));
			if (linkedUser) customer = { ...customer, active: linkedUser.active };
			resetDraft();
		} catch {
			error = true;
		} finally {
			loading = false;
		}
	});

	async function handleToggleActive() {
		if (customer.active !== false) {
			confirmingDeactivate = true;
			return;
		}
		await doToggleActive();
	}

	async function doToggleActive() {
		try {
			await setUserActive(customer.userId, customer.active === false);
			customer = { ...customer, active: !customer.active };
		} catch {
			//
		}
	}

	function resetDraft() {
		draft = {
			firstName: customer.firstName ?? '',
			lastName: customer.lastName ?? '',
			phone: customer.phone ?? '',
			email: customer.email ?? ''
		};
	}

	async function handleSave() {
		saving = true;
		try {
			customer = await patchCustomer(id, draft);
			editing = false;
		} catch {
			// leave form open
		} finally {
			saving = false;
		}
	}

	async function handleApprovePhoto() {
		await approvePhoto(id);
		customer = { ...customer, photoApprovalPending: false };
	}

	async function handleRejectPhoto() {
		await rejectPhoto(id);
		customer = { ...customer, photoApprovalPending: false, profilePhotoPath: null };
	}
</script>

<main class="flex-1 overflow-y-auto p-8 lg:p-10">
	<div class="mx-auto max-w-3xl space-y-6">
		<div class="flex items-center gap-2 text-sm">
			<a href={resolve('/staff/customers')} class="text-primary hover:underline">Customers</a>
			<span class="text-muted-foreground">/</span>
			<span class="text-muted-foreground">Detail</span>
		</div>

		{#if loading}
			<Skeleton class="h-48 rounded-xl" />
		{:else if error}
			<p class="text-sm text-destructive">Customer not found.</p>
		{:else}
			<div class="space-y-4 rounded-xl border border-border bg-card p-6">
				<div class="flex items-start justify-between">
					<h1 class="text-xl font-bold text-foreground">
						{customer.firstName ?? ''}
						{customer.lastName ?? ''}
					</h1>
					{#if isAdmin && !editing}
						<div class="flex gap-2">
							<Button size="sm" variant="outline" onclick={() => (editing = true)}>Edit</Button>
							<Button
								size="sm"
								variant={customer.active === false ? 'outline' : 'destructive'}
								onclick={handleToggleActive}
							>
								{customer.active === false ? 'Reactivate' : 'Deactivate'}
							</Button>
						</div>
					{/if}
				</div>

				{#if editing}
					<form
						class="space-y-3"
						onsubmit={(e) => {
							e.preventDefault();
							handleSave();
						}}
					>
						<div class="grid grid-cols-2 gap-3">
							<Input bind:value={draft.firstName} placeholder="First name" />
							<Input bind:value={draft.lastName} placeholder="Last name" />
							<Input bind:value={draft.phone} placeholder="Phone" />
							<Input bind:value={draft.email} placeholder="Email" type="email" />
						</div>
						<div class="flex gap-2">
							<Button type="submit" size="sm" disabled={saving}>
								{saving ? 'Saving...' : 'Save'}
							</Button>
							<Button
								type="button"
								size="sm"
								variant="ghost"
								onclick={() => {
									editing = false;
									resetDraft();
								}}
							>
								Cancel
							</Button>
						</div>
					</form>
				{:else}
					<Separator />
					<dl class="grid grid-cols-2 gap-4 text-sm">
						<div>
							<dt class="text-xs font-semibold tracking-widest text-muted-foreground uppercase">
								Email
							</dt>
							<dd class="mt-1 text-foreground">{customer.email ?? '—'}</dd>
						</div>
						<div>
							<dt class="text-xs font-semibold tracking-widest text-muted-foreground uppercase">
								Phone
							</dt>
							<dd class="mt-1 text-foreground">{customer.phone ?? '—'}</dd>
						</div>
						<div>
							<dt class="text-xs font-semibold tracking-widest text-muted-foreground uppercase">
								Reward tier
							</dt>
							<dd class="mt-1 text-foreground">{customer.rewardTierName ?? '—'}</dd>
						</div>
						<div>
							<dt class="text-xs font-semibold tracking-widest text-muted-foreground uppercase">
								Reward Balance
							</dt>
							<dd class="mt-1 text-foreground">{customer.rewardBalance ?? 0} pts</dd>
						</div>
						<div>
							<dt class="text-xs font-semibold tracking-widest text-muted-foreground uppercase">
								Address
							</dt>
							<dd class="mt-1 text-foreground">
								{#if customer.address}
									{customer.address.line1}, {customer.address.city}
								{:else}
									—
								{/if}
							</dd>
						</div>
						<div>
							<dt class="text-xs font-semibold tracking-widest text-muted-foreground uppercase">
								Status
							</dt>
							<dd class="mt-1">
								{#if customer.active === false}
									<span class="text-xs font-medium text-destructive">Deactivated</span>
								{:else}
									<span class="text-xs font-medium text-green-600">Active</span>
								{/if}
							</dd>
						</div>
					</dl>
				{/if}
			</div>

			{#if customer.photoApprovalPending}
				<div class="space-y-3 rounded-xl border border-destructive bg-destructive/5 p-5">
					<p class="text-sm font-semibold text-foreground">Photo Pending Approval</p>
					{#if customer.profilePhotoPath}
						<img
							src={customer.profilePhotoPath}
							alt=""
							class="h-24 w-24 rounded-full border border-border object-cover"
						/>
					{/if}
					<div class="flex gap-2">
						<Button size="sm" variant="outline" onclick={handleApprovePhoto}>Approve</Button>
						<Button size="sm" variant="destructive" onclick={handleRejectPhoto}>Reject</Button>
					</div>
				</div>
			{/if}

			{#if confirmingDeactivate}
				<div class="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
					<div class="w-full max-w-sm rounded-xl border border-border bg-card p-6 shadow-lg">
						<h2 class="text-base font-semibold text-foreground">Deactivate account?</h2>
						<p class="mt-2 text-sm text-muted-foreground">
							{customer.firstName}'s account will be deactivated and they will no longer be able to
							log in.
						</p>
						<div class="mt-6 flex justify-end gap-2">
							<Button size="sm" variant="outline" onclick={() => (confirmingDeactivate = false)}>
								Cancel
							</Button>
							<Button
								size="sm"
								variant="destructive"
								onclick={async () => {
									confirmingDeactivate = false;
									await doToggleActive();
								}}
							>
								Deactivate
							</Button>
						</div>
					</div>
				</div>
			{/if}
		{/if}
	</div>
</main>
