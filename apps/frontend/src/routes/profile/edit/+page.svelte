<script>
	import {
		Card,
		CardContent,
		CardHeader,
		CardTitle,
		CardDescription
	} from '$lib/components/ui/card';
	import { Button } from '$lib/components/ui/button';
	import { Separator } from '$lib/components/ui/separator';
	import { onMount } from 'svelte';
	import { getProfile } from '$lib/services/profile';
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';

	let profile = $state(null);
	let loading = $state(true);
	let saving = $state(false);
	let error = $state(null);
	let success = $state(false);

	let fields = $state({
		firstName: '',
		middleInitial: '',
		lastName: '',
		phone: '',
		businessPhone: '',
		addressLine1: '',
		addressLine2: '',
		city: '',
		province: '',
		postalCode: ''
	});

	let errors = $state({});

	const provinces = [
		{ value: 'AB', label: 'Alberta' },
		{ value: 'BC', label: 'British Columbia' },
		{ value: 'MB', label: 'Manitoba' },
		{ value: 'NB', label: 'New Brunswick' },
		{ value: 'NL', label: 'Newfoundland and Labrador' },
		{ value: 'NS', label: 'Nova Scotia' },
		{ value: 'NT', label: 'Northwest Territories' },
		{ value: 'NU', label: 'Nunavut' },
		{ value: 'ON', label: 'Ontario' },
		{ value: 'PE', label: 'Prince Edward Island' },
		{ value: 'QC', label: 'Quebec' },
		{ value: 'SK', label: 'Saskatchewan' },
		{ value: 'YT', label: 'Yukon' }
	];

	onMount(async () => {
		try {
			profile = await getProfile();
			fields = {
				firstName: profile.firstName ?? '',
				middleInitial: profile.middleInitial ?? '',
				lastName: profile.lastName ?? '',
				phone: profile.phone ?? '',
				businessPhone: profile.businessPhone ?? '',
				addressLine1: profile.address?.addressLine1 ?? '',
				addressLine2: profile.address?.addressLine2 ?? '',
				city: profile.address?.addressCity ?? '',
				province: profile.address?.addressProvince ?? 'AB',
				postalCode: profile.address?.addressPostalCode ?? ''
			};
		} catch {
			error = true;
		} finally {
			loading = false;
		}
	});

	function validate() {
		const e = {};
		if (!fields.firstName.trim()) e.firstName = 'First name is required.';
		if (!fields.lastName.trim()) e.lastName = 'Last name is required.';
		if (!fields.phone.trim()) e.phone = 'Phone is required.';
		if (!fields.addressLine1.trim()) e.addressLine1 = 'Address is required.';
		if (!fields.city.trim()) e.city = 'City is required.';
		if (!fields.province) e.province = 'Province is required.';
		if (!fields.postalCode.trim()) e.postalCode = 'Postal code is required.';
		else if (!/^[A-Za-z]\d[A-Za-z][ -]?\d[A-Za-z]\d$/.test(fields.postalCode.trim()))
			e.postalCode = 'Enter a valid Canadian postal code.';
		errors = e;
		return Object.keys(e).length === 0;
	}

	function formatPhone(value) {
		const digits = value.replace(/\D/g, '').substring(0, 10);
		const parts = [];
		if (digits.length > 0) parts.push('(' + digits.substring(0, 3));
		if (digits.length >= 4) parts.push(') ' + digits.substring(3, 6));
		if (digits.length >= 7) parts.push('-' + digits.substring(6, 10));
		return parts.join('');
	}

	async function handleSave(event) {
		event.preventDefault();
		if (!validate()) return;

		saving = true;
		success = false;

		try {
			const res = await fetch('/api/v1/customers/me', {
				method: 'PATCH',
				credentials: 'include',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({
					firstName: fields.firstName.trim(),
					middleInitial: fields.middleInitial.trim() || null,
					lastName: fields.lastName.trim(),
					phone: fields.phone.replace(/\D/g, ''),
					businessPhone: fields.businessPhone.trim() || null,
					addressLine1: fields.addressLine1.trim(),
					addressLine2: fields.addressLine2.trim() || null,
					city: fields.city.trim(),
					province: fields.province,
					postalCode: fields.postalCode.trim().toUpperCase()
				})
			});

			if (!res.ok) throw new Error('Failed to save');
			success = true;
			setTimeout(() => goto(resolve('/profile')), 1200);
		} catch {
			errors.general = 'Failed to save changes. Please try again.';
		} finally {
			saving = false;
		}
	}
</script>

<div class="flex min-h-screen bg-background">
	<main class="mx-auto w-full max-w-2xl px-6 py-10">
		<div class="mb-8 flex items-center justify-between">
			<div>
				<h1 class="text-2xl font-bold tracking-tight text-foreground">Edit Profile</h1>
				<p class="mt-1 text-sm text-muted-foreground">Update your personal information</p>
			</div>
			<Button variant="outline" onclick={() => goto(resolve('/profile'))}>Cancel</Button>
		</div>

		{#if loading}
			<div class="flex justify-center py-24">
				<div
					class="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent"
				></div>
			</div>
		{:else if error}
			<p class="text-center text-sm text-destructive">Failed to load profile.</p>
		{:else}
			<form onsubmit={handleSave} class="space-y-6">
				<!-- Personal Info -->
				<Card>
					<CardHeader>
						<CardTitle>Personal Information</CardTitle>
						<CardDescription>Your name and contact details</CardDescription>
					</CardHeader>
					<CardContent class="space-y-4">
						<!-- First / Last Name -->
						<div class="grid grid-cols-2 gap-4">
							<div class="space-y-1.5">
								<label class="text-xs font-semibold tracking-widest text-muted-foreground uppercase"
									>First Name</label
								>
								<input
									type="text"
									bind:value={fields.firstName}
									class="w-full rounded-lg border border-border bg-background px-4 py-2.5 text-sm transition focus:ring-2 focus:ring-primary focus:outline-none
										{errors.firstName ? 'border-destructive ring-1 ring-destructive' : ''}"
								/>
								{#if errors.firstName}<p class="text-xs text-destructive">
										{errors.firstName}
									</p>{/if}
							</div>
							<div class="space-y-1.5">
								<label class="text-xs font-semibold tracking-widest text-muted-foreground uppercase"
									>Last Name</label
								>
								<input
									type="text"
									bind:value={fields.lastName}
									class="w-full rounded-lg border border-border bg-background px-4 py-2.5 text-sm transition focus:ring-2 focus:ring-primary focus:outline-none
										{errors.lastName ? 'border-destructive ring-1 ring-destructive' : ''}"
								/>
								{#if errors.lastName}<p class="text-xs text-destructive">{errors.lastName}</p>{/if}
							</div>
						</div>

						<!-- Middle Initial -->
						<div class="space-y-1.5">
							<label class="text-xs font-semibold tracking-widest text-muted-foreground uppercase">
								Middle Initial <span class="font-normal normal-case">(optional)</span>
							</label>
							<input
								type="text"
								maxlength="1"
								bind:value={fields.middleInitial}
								class="w-full rounded-lg border border-border bg-background px-4 py-2.5 text-sm transition focus:ring-2 focus:ring-primary focus:outline-none"
							/>
						</div>

						<!-- Phone -->
						<div class="space-y-1.5">
							<label class="text-xs font-semibold tracking-widest text-muted-foreground uppercase"
								>Phone</label
							>
							<input
								type="tel"
								bind:value={fields.phone}
								oninput={(e) => {
									fields.phone = formatPhone(e.target.value);
								}}
								class="w-full rounded-lg border border-border bg-background px-4 py-2.5 text-sm transition focus:ring-2 focus:ring-primary focus:outline-none
									{errors.phone ? 'border-destructive ring-1 ring-destructive' : ''}"
							/>
							{#if errors.phone}<p class="text-xs text-destructive">{errors.phone}</p>{/if}
						</div>

						<!-- Business Phone -->
						<div class="space-y-1.5">
							<label class="text-xs font-semibold tracking-widest text-muted-foreground uppercase">
								Business Phone <span class="font-normal normal-case">(optional)</span>
							</label>
							<input
								type="tel"
								bind:value={fields.businessPhone}
								oninput={(e) => {
									fields.businessPhone = formatPhone(e.target.value);
								}}
								class="w-full rounded-lg border border-border bg-background px-4 py-2.5 text-sm transition focus:ring-2 focus:ring-primary focus:outline-none"
							/>
						</div>
					</CardContent>
				</Card>

				<!-- Address -->
				<Card>
					<CardHeader>
						<CardTitle>Address</CardTitle>
						<CardDescription>Your delivery address</CardDescription>
					</CardHeader>
					<CardContent class="space-y-4">
						<div class="space-y-1.5">
							<label class="text-xs font-semibold tracking-widest text-muted-foreground uppercase"
								>Address Line 1</label
							>
							<input
								type="text"
								bind:value={fields.addressLine1}
								class="w-full rounded-lg border border-border bg-background px-4 py-2.5 text-sm transition focus:ring-2 focus:ring-primary focus:outline-none
									{errors.addressLine1 ? 'border-destructive ring-1 ring-destructive' : ''}"
							/>
							{#if errors.addressLine1}<p class="text-xs text-destructive">
									{errors.addressLine1}
								</p>{/if}
						</div>

						<div class="space-y-1.5">
							<label class="text-xs font-semibold tracking-widest text-muted-foreground uppercase">
								Address Line 2 <span class="font-normal normal-case">(optional)</span>
							</label>
							<input
								type="text"
								bind:value={fields.addressLine2}
								class="w-full rounded-lg border border-border bg-background px-4 py-2.5 text-sm transition focus:ring-2 focus:ring-primary focus:outline-none"
							/>
						</div>

						<div class="grid grid-cols-3 gap-4">
							<div class="space-y-1.5">
								<label class="text-xs font-semibold tracking-widest text-muted-foreground uppercase"
									>City</label
								>
								<input
									type="text"
									bind:value={fields.city}
									class="w-full rounded-lg border border-border bg-background px-4 py-2.5 text-sm transition focus:ring-2 focus:ring-primary focus:outline-none
										{errors.city ? 'border-destructive ring-1 ring-destructive' : ''}"
								/>
								{#if errors.city}<p class="text-xs text-destructive">{errors.city}</p>{/if}
							</div>
							<div class="space-y-1.5">
								<label class="text-xs font-semibold tracking-widest text-muted-foreground uppercase"
									>Province</label
								>
								<select
									bind:value={fields.province}
									class="w-full rounded-lg border border-border bg-background px-4 py-2.5 text-sm transition focus:ring-2 focus:ring-primary focus:outline-none
										{errors.province ? 'border-destructive ring-1 ring-destructive' : ''}"
								>
									{#each provinces as p}
										<option value={p.value}>{p.label}</option>
									{/each}
								</select>
								{#if errors.province}<p class="text-xs text-destructive">{errors.province}</p>{/if}
							</div>
							<div class="space-y-1.5">
								<label class="text-xs font-semibold tracking-widest text-muted-foreground uppercase"
									>Postal Code</label
								>
								<input
									type="text"
									maxlength="7"
									bind:value={fields.postalCode}
									class="w-full rounded-lg border border-border bg-background px-4 py-2.5 text-sm transition focus:ring-2 focus:ring-primary focus:outline-none
										{errors.postalCode ? 'border-destructive ring-1 ring-destructive' : ''}"
								/>
								{#if errors.postalCode}<p class="text-xs text-destructive">
										{errors.postalCode}
									</p>{/if}
							</div>
						</div>
					</CardContent>
				</Card>

				{#if errors.general}
					<p class="text-sm text-destructive">{errors.general}</p>
				{/if}

				{#if success}
					<p class="text-sm text-green-600">Profile updated successfully! Redirecting...</p>
				{/if}

				<div class="flex justify-end gap-3">
					<Button variant="outline" type="button" onclick={() => goto(resolve('/profile'))}>
						Cancel
					</Button>
					<Button type="submit" disabled={saving}>
						{saving ? 'Saving...' : 'Save Changes'}
					</Button>
				</div>
			</form>
		{/if}
	</main>
</div>
