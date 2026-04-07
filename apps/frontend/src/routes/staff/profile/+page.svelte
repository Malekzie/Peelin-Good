<script>
	import { onMount } from 'svelte';
	import { Skeleton } from '$lib/components/ui/skeleton';
	import { Button } from '$lib/components/ui/button';
	import { Input } from '$lib/components/ui/input';
	import { Separator } from '$lib/components/ui/separator';
	import { Avatar, AvatarFallback } from '$lib/components/ui/avatar';
	import { getProfile } from '$lib/services/profile.js';

	let profile = $state(null);
	let loading = $state(true);
	let error = $state(null);
	let editing = $state(false);
	let saving = $state(false);
	let draft = $state({});

	const initials = $derived(
		profile
			? `${profile.firstName?.[0] ?? ''}${profile.lastName?.[0] ?? ''}`.toUpperCase() || '?'
			: '?'
	);

	onMount(async () => {
		try {
			profile = await getProfile();
			resetDraft();
		} catch {
			error = true;
		} finally {
			loading = false;
		}
	});

	function resetDraft() {
		draft = {
			firstName: profile.firstName ?? '',
			lastName: profile.lastName ?? '',
			phone: profile.phone ?? '',
			workEmail: profile.workEmail ?? ''
		};
	}

	async function handleSave() {
		saving = true;
		try {
			const res = await fetch('/api/v1/employee/me', {
				method: 'PATCH',
				headers: { 'Content-Type': 'application/json' },
				credentials: 'include',
				body: JSON.stringify(draft)
			});
			if (!res.ok) throw new Error('Failed to save');
			profile = await res.json();
			editing = false;
		} catch {
			// leave form open
		} finally {
			saving = false;
		}
	}
</script>

<main class="flex-1 overflow-y-auto p-8 lg:p-10">
	<div class="mx-auto max-w-3xl space-y-6">
		<div>
			<h1 class="text-2xl font-bold tracking-tight text-foreground">My Profile</h1>
			<p class="mt-1 text-sm text-muted-foreground">Your staff account details</p>
		</div>

		{#if loading}
			<Skeleton class="h-48 rounded-xl" />
		{:else if error}
			<p class="text-sm text-destructive">Failed to load profile.</p>
		{:else}
			<div class="space-y-6 rounded-xl border border-border bg-card p-6">
				<div class="flex items-center gap-5">
					<Avatar class="h-16 w-16">
						<AvatarFallback class="bg-primary text-xl font-bold text-primary-foreground">
							{initials}
						</AvatarFallback>
					</Avatar>
					<div>
						<p class="text-xl font-bold text-foreground">
							{profile.firstName ?? ''} {profile.lastName ?? ''}
						</p>
						<p class="text-sm capitalize text-muted-foreground">{profile.position ?? '—'}</p>
					</div>
				</div>

				<Separator />

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
							<Input bind:value={draft.workEmail} placeholder="Work email" type="email" />
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
					<dl class="grid grid-cols-2 gap-4 text-sm">
						<div>
							<dt class="text-xs font-semibold uppercase tracking-widest text-muted-foreground">
								Work Email
							</dt>
							<dd class="mt-1 text-foreground">{profile.workEmail ?? '—'}</dd>
						</div>
						<div>
							<dt class="text-xs font-semibold uppercase tracking-widest text-muted-foreground">
								Phone
							</dt>
							<dd class="mt-1 text-foreground">{profile.phone ?? '—'}</dd>
						</div>
						<div>
							<dt class="text-xs font-semibold uppercase tracking-widest text-muted-foreground">
								Position
							</dt>
							<dd class="mt-1 text-foreground">{profile.position ?? '—'}</dd>
						</div>
						<div>
							<dt class="text-xs font-semibold uppercase tracking-widest text-muted-foreground">
								Username
							</dt>
							<dd class="mt-1 text-foreground">{profile.username ?? '—'}</dd>
						</div>
					</dl>
					<Button size="sm" variant="outline" onclick={() => (editing = true)}>Edit</Button>
				{/if}
			</div>
		{/if}
	</div>
</main>
