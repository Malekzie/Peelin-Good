<script>
	import { onMount } from 'svelte';
	import { Skeleton } from '$lib/components/ui/skeleton';
	import { Button } from '$lib/components/ui/button';
	import { Input } from '$lib/components/ui/input';
	import { Separator } from '$lib/components/ui/separator';
	import { Badge } from '$lib/components/ui/badge';
	import { Avatar, AvatarImage, AvatarFallback } from '$lib/components/ui/avatar';
	import { getProfile, uploadProfilePhoto } from '$lib/services/profile.js';

	let profile = $state(null);
	let bakeryName = $state(null);
	let loading = $state(true);
	let error = $state(null);
	let editing = $state(false);
	let saving = $state(false);
	let uploadingPhoto = $state(false);
	let draft = $state({});

	let fileInput = $state(null);

	const initials = $derived(
		profile
			? [profile.firstName?.[0], profile.lastName?.[0]].filter(Boolean).join('').toUpperCase() ||
					'?'
			: '?'
	);

	const fullName = $derived(
		profile
			? [
					profile.firstName,
					profile.middleInitial ? `${profile.middleInitial}.` : null,
					profile.lastName
				]
					.filter(Boolean)
					.join(' ')
			: ''
	);

	const fullAddress = $derived(() => {
		const a = profile?.address;
		if (!a) return null;
		return [a.line1, a.line2, a.city, a.province, a.postalCode].filter(Boolean).join(', ');
	});

	onMount(async () => {
		try {
			profile = await getProfile();
			resetDraft();
			if (profile.bakeryId) {
				const res = await fetch(`/api/v1/bakeries/${profile.bakeryId}`);
				if (res.ok) {
					const bakery = await res.json();
					bakeryName = bakery.name ?? bakery.bakeryName ?? null;
				}
			}
		} catch {
			error = true;
		} finally {
			loading = false;
		}
	});

	function resetDraft() {
		draft = {
			firstName: profile.firstName ?? '',
			middleInitial: profile.middleInitial ?? '',
			lastName: profile.lastName ?? '',
			phone: profile.phone ?? '',
			businessPhone: profile.businessPhone ?? '',
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
				body: JSON.stringify({
					firstName: draft.firstName || undefined,
					middleInitial: draft.middleInitial || undefined,
					lastName: draft.lastName || undefined,
					phone: draft.phone || undefined,
					businessPhone: draft.businessPhone || undefined,
					workEmail: draft.workEmail || undefined
				})
			});
			if (!res.ok) throw new Error('Failed to save');
			profile = { ...profile, ...(await res.json()) };
			editing = false;
		} catch {
			// leave form open
		} finally {
			saving = false;
		}
	}

	async function handlePhotoUpload(e) {
		const file = e.target.files?.[0];
		if (!file) return;
		uploadingPhoto = true;
		try {
			const updated = await uploadProfilePhoto(file);
			profile = {
				...profile,
				profilePhotoPath: updated.profilePhotoPath,
				photoApprovalPending: true
			};
		} catch {
			// silent — photo stays unchanged
		} finally {
			uploadingPhoto = false;
			fileInput.value = '';
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
			<div class="space-y-4">
				<div class="flex items-center gap-5">
					<Skeleton class="h-20 w-20 rounded-full" />
					<div class="space-y-2">
						<Skeleton class="h-6 w-48" />
						<Skeleton class="h-4 w-24" />
					</div>
				</div>
				<Skeleton class="h-48 rounded-xl" />
				<Skeleton class="h-40 rounded-xl" />
			</div>
		{:else if error}
			<p class="text-sm text-destructive">Failed to load profile.</p>
		{:else}
			<!-- Header -->
			<div
				class="flex flex-col gap-4 rounded-xl border border-border bg-card p-6 sm:flex-row sm:items-center sm:gap-6"
			>
				<div class="relative">
					<Avatar class="h-20 w-20">
						<AvatarImage
							src={profile.profilePhotoPath}
							alt={fullName}
							class={profile.photoApprovalPending ? 'opacity-60 grayscale' : ''}
						/>
						<AvatarFallback class="bg-primary text-2xl font-bold text-primary-foreground">
							{initials}
						</AvatarFallback>
					</Avatar>
				</div>

				<div class="flex-1 space-y-1">
					<h2 class="text-xl font-bold text-foreground">{fullName}</h2>
					{#if profile.position}
						<Badge variant="outline">{profile.position}</Badge>
					{/if}
					{#if bakeryName}
						<p class="text-xs text-muted-foreground">{bakeryName}</p>
					{/if}
					{#if profile.photoApprovalPending}
						<p class="text-xs text-amber-600">Photo pending approval</p>
					{/if}
				</div>

				<div class="flex flex-row gap-2 sm:flex-col sm:items-end">
					<input
						bind:this={fileInput}
						type="file"
						accept="image/*"
						class="hidden"
						onchange={handlePhotoUpload}
					/>
					<Button
						size="sm"
						variant="outline"
						onclick={() => fileInput.click()}
						disabled={uploadingPhoto}
					>
						{uploadingPhoto ? 'Uploading...' : 'Change Photo'}
					</Button>
					{#if !editing}
						<Button size="sm" onclick={() => (editing = true)}>Edit Profile</Button>
					{/if}
				</div>
			</div>

			{#if editing}
				<!-- Edit form -->
				<form
					class="space-y-4 rounded-xl border border-border bg-card p-6"
					onsubmit={(e) => {
						e.preventDefault();
						handleSave();
					}}
				>
					<p class="text-sm font-semibold text-foreground">Edit Details</p>
					<div class="grid grid-cols-1 gap-3 sm:grid-cols-3">
						<Input bind:value={draft.firstName} placeholder="First name" />
						<Input bind:value={draft.middleInitial} placeholder="M.I." maxlength="1" />
						<Input bind:value={draft.lastName} placeholder="Last name" />
					</div>
					<Separator />
					<div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
						<Input bind:value={draft.workEmail} placeholder="Work email" type="email" />
						<Input bind:value={draft.phone} placeholder="Phone" />
						<Input bind:value={draft.businessPhone} placeholder="Business phone (optional)" />
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
				<!-- Contact section -->
				<div class="space-y-4 rounded-xl border border-border bg-card p-6">
					<p class="text-xs font-semibold tracking-widest text-muted-foreground uppercase">
						Contact
					</p>
					<div class="grid grid-cols-1 gap-4 text-sm sm:grid-cols-2">
						<div>
							<p class="text-xs font-semibold tracking-widest text-muted-foreground uppercase">
								Work Email
							</p>
							<p class="mt-1 text-foreground">{profile.workEmail ?? '—'}</p>
						</div>
						<div>
							<p class="text-xs font-semibold tracking-widest text-muted-foreground uppercase">
								Phone
							</p>
							<p class="mt-1 text-foreground">{profile.phone ?? '—'}</p>
						</div>
						{#if profile.businessPhone}
							<div>
								<p class="text-xs font-semibold tracking-widest text-muted-foreground uppercase">
									Business Phone
								</p>
								<p class="mt-1 text-foreground">{profile.businessPhone}</p>
							</div>
						{/if}
						<div>
							<p class="text-xs font-semibold tracking-widest text-muted-foreground uppercase">
								Username
							</p>
							<p class="mt-1 text-foreground">{profile.username ?? '—'}</p>
						</div>
					</div>
				</div>

				<!-- Assignment section -->
				<div class="space-y-4 rounded-xl border border-border bg-card p-6">
					<p class="text-xs font-semibold tracking-widest text-muted-foreground uppercase">
						Assignment
					</p>
					<div class="grid grid-cols-1 gap-4 text-sm sm:grid-cols-2">
						<div>
							<p class="text-xs font-semibold tracking-widest text-muted-foreground uppercase">
								Bakery
							</p>
							<p class="mt-1 text-foreground">{bakeryName ?? '—'}</p>
						</div>
						<div>
							<p class="text-xs font-semibold tracking-widest text-muted-foreground uppercase">
								Position
							</p>
							<p class="mt-1 text-foreground">{profile.position ?? '—'}</p>
						</div>
						{#if profile.address}
							<div class="col-span-2">
								<p class="text-xs font-semibold tracking-widest text-muted-foreground uppercase">
									Address
								</p>
								<p class="mt-1 text-foreground">
									{[
										profile.address.line1,
										profile.address.line2,
										profile.address.city,
										profile.address.province,
										profile.address.postalCode
									]
										.filter(Boolean)
										.join(', ')}
								</p>
							</div>
						{/if}
					</div>
				</div>
			{/if}
		{/if}
	</div>
</main>
