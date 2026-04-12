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
	import {
		getProfile,
		updateProfile,
		uploadProfilePhoto,
		deactivateAccount,
		deleteAccount
	} from '$lib/services/profile';
	import { logoutUser } from '$lib/services/auth';
	import { changePassword } from '$lib/services/account';
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { page } from '$app/stores';
	import { formatCanadianPostalInput } from '$lib/canadianPostalCode';

	const reason = $derived($page.url.searchParams.get('reason'));

	const initials = $derived(
		profile
			? [profile.firstName?.[0], profile.lastName?.[0]].filter(Boolean).join('').toUpperCase()
			: ''
	);

	let profile = $state(null);
	let loading = $state(true);
	let saving = $state(false);
	let error = $state(null);
	let success = $state(false);
	let photoFile = $state(null);
	let photoPreview = $state(null);
	let uploadingPhoto = $state(false);
	let showDeactivateConfirm = $state(false);
	let deactivating = $state(false);
	let deactivatePassword = $state('');
	let showDeleteConfirm = $state(false);
	let deleting = $state(false);

	let fields = $state({
		username: '',
		email: '',
		firstName: '',
		middleInitial: '',
		lastName: '',
		phone: '',
		businessPhone: '',
		addressLine1: '',
		addressLine2: '',
		city: '',
		province: '',
		postalCode: '',
		currentPassword: '',
		newPassword: '',
		confirmPassword: ''
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
				addressLine1: profile.address?.line1 ?? '',
				addressLine2: profile.address?.line2 ?? '',
				city: profile.address?.city ?? '',
				province: profile.address?.province ?? 'AB',
				postalCode: formatCanadianPostalInput(profile.address?.postalCode ?? ''),
				username: profile.username ?? '',
				email: profile.email ?? ''
			};
		} catch {
			error = true;
		} finally {
			loading = false;
		}
	});

	const missingFields = $derived(
		reason === 'checkout' && profile
			? {
					phone: !profile.phone,
					addressLine1: !profile.address?.line1,
					city: !profile.address?.city,
					province: !profile.address?.province,
					postalCode: !profile.address?.postalCode
				}
			: {}
	);

	async function handleDeactivate() {
		if (!deactivatePassword.trim()) {
			errors.deactivatePassword = 'Enter your password to confirm.';
			return;
		}
		deactivating = true;
		errors.deactivatePassword = '';
		try {
			await deactivateAccount(deactivatePassword);
			await logoutUser();
			goto(resolve('/'));
		} catch {
			showDeactivateConfirm = false;
			errors.general = 'Failed to deactivate account. Check your password and try again.';
		} finally {
			deactivating = false;
			deactivatePassword = '';
		}
	}

	async function handleDelete() {
		deleting = true;
		try {
			await deleteAccount();
			await logoutUser();
			goto(resolve('/login'));
		} catch {
			showDeleteConfirm = false;
			errors.general = 'Failed to delete account. Please try again.';
		} finally {
			deleting = false;
		}
	}

	function validate() {
		const e = {};
		if (fields.newPassword || fields.currentPassword) {
			if (!fields.currentPassword) e.currentPassword = 'Current password is required.';
			if (!fields.newPassword) e.newPassword = 'New password is required.';
			else if (fields.newPassword.length < 8) e.newPassword = 'Must be at least 8 characters.';
			else if (fields.newPassword === fields.currentPassword)
				e.newPassword = 'New password must be different from your current password.';
			if (fields.newPassword !== fields.confirmPassword)
				e.confirmPassword = 'Passwords do not match.';
		}
		if (!fields.firstName.trim()) e.firstName = 'First name is required.';
		if (!fields.lastName.trim()) e.lastName = 'Last name is required.';
		if (!fields.phone.trim()) e.phone = 'Phone is required.';
		if (!fields.addressLine1.trim()) e.addressLine1 = 'Address is required.';
		if (!fields.city.trim()) e.city = 'City is required.';
		if (!fields.province) e.province = 'Province is required.';
		if (!fields.postalCode.trim()) e.postalCode = 'Postal code is required.';
		else if (!/^[A-Za-z]\d[A-Za-z][ -]?\d[A-Za-z]\d$/.test(fields.postalCode.trim()))
			e.postalCode = 'Enter a valid Canadian postal code.';
		if (!fields.username.trim()) e.username = 'Username is required.';
		if (!fields.email.trim()) e.email = 'Email is required.';
		else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(fields.email.trim()))
			e.email = 'Enter a valid email address.';
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

	function formatPostalCode(value) {
		const cleaned = value
			.replace(/[^a-zA-Z0-9]/g, '')
			.toUpperCase()
			.substring(0, 6);
		if (cleaned.length > 3) {
			return cleaned.substring(0, 3) + ' ' + cleaned.substring(3);
		}
		return cleaned;
	}

	async function handleSave(event) {
		event.preventDefault();
		if (!validate()) return;

		saving = true;
		success = false;

		if (fields.currentPassword && fields.newPassword) {
			try {
				await changePassword(fields.currentPassword, fields.newPassword);
			} catch (e) {
				errors.general = e.message ?? 'Failed to change password.';
				saving = false;
				return;
			}
		}

		const usernameChanged = fields.username.trim() !== profile.username;

		try {
			await updateProfile({
				username: fields.username.trim(),
				email: fields.email.trim(),
				firstName: fields.firstName.trim(),
				middleInitial: fields.middleInitial.trim() || null,
				lastName: fields.lastName.trim(),
				phone: fields.phone.trim(),
				businessPhone: fields.businessPhone.trim() || null,
				address: {
					line1: fields.addressLine1.trim(),
					line2: fields.addressLine2.trim() || null,
					city: fields.city.trim(),
					province: fields.province,
					postalCode: fields.postalCode.trim().toUpperCase()
				}
			});

			success = true;

			if (usernameChanged) {
				await logoutUser();
				goto(resolve('/login'));
			} else {
				setTimeout(() => goto(resolve(reason === 'checkout' ? '/checkout' : '/profile')), 1200);
			}
		} catch (e) {
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
			{#if reason === 'checkout'}
				<div
					class="mb-6 rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800"
				>
					<p class="font-semibold">Please complete your profile before placing an order.</p>
					{#if Object.values(missingFields).some(Boolean)}
						<p class="mt-1">The following fields are required:</p>
						<ul class="mt-1 list-disc pl-5">
							{#if missingFields.phone}<li>Phone number</li>{/if}
							{#if missingFields.addressLine1}<li>Address line 1</li>{/if}
							{#if missingFields.city}<li>City</li>{/if}
							{#if missingFields.province}<li>Province</li>{/if}
							{#if missingFields.postalCode}<li>Postal code</li>{/if}
						</ul>
					{/if}
				</div>
			{/if}

			<form onsubmit={handleSave} class="space-y-6">
				<!-- Profile Photo -->
				<Card>
					<CardHeader>
						<CardTitle>Profile Photo</CardTitle>
						<CardDescription>Upload a new profile photo</CardDescription>
					</CardHeader>
					<CardContent class="flex items-center gap-6">
						<!-- Preview -->
						<div
							class="h-20 w-20 shrink-0 overflow-hidden rounded-full border-2 border-border bg-muted"
						>
							{#if photoPreview}
								<img src={photoPreview} alt="" class="h-full w-full object-cover" />
							{:else if profile?.profilePhotoPath}
								<img
									src={profile.profilePhotoPath}
									alt=""
									class="h-full w-full object-cover {profile.photoApprovalPending
										? 'opacity-60 grayscale'
										: ''}"
								/>
							{:else}
								<div
									class="flex h-full w-full items-center justify-center text-2xl font-bold text-muted-foreground"
								>
									{initials}
								</div>
							{/if}
						</div>

						<div class="flex flex-col gap-2">
							<input
								type="file"
								accept="image/jpeg,image/png"
								class="hidden"
								id="photo-input"
								onchange={(e) => {
									const file = e.target.files?.[0];
									if (!file) return;
									photoFile = file;
									photoPreview = URL.createObjectURL(file);
								}}
							/>
							<label
								for="photo-input"
								class="cursor-pointer rounded-lg border border-border px-4 py-2 text-sm font-medium transition hover:bg-muted"
							>
								Choose photo
							</label>
							{#if photoFile}
								<Button
									type="button"
									disabled={uploadingPhoto}
									onclick={async () => {
										uploadingPhoto = true;
										try {
											const updated = await uploadProfilePhoto(photoFile);
											profile = { ...profile, profilePhotoPath: updated.profilePhotoPath };
											photoFile = null;
											photoPreview = null;
										} catch {
											errors.general = 'Failed to upload photo. Please try again.';
										} finally {
											uploadingPhoto = false;
										}
									}}
								>
									{uploadingPhoto ? 'Uploading...' : 'Upload photo'}
								</Button>
							{/if}
							{#if profile?.photoApprovalPending}
								<p class="text-xs text-amber-500">Your photo is pending review.</p>
							{/if}
						</div>
					</CardContent>
				</Card>

				<!-- Personal Info -->
				<Card>
					<CardHeader>
						<CardTitle>Personal Information</CardTitle>
						<CardDescription>Your name and contact details</CardDescription>
					</CardHeader>
					<CardContent class="space-y-4">
						<div class="space-y-1.5">
							<label
								for="usernameInput"
								class="text-xs font-semibold tracking-widest text-muted-foreground uppercase"
								>Username</label
							>
							<input
								id="usernameInput"
								type="text"
								bind:value={fields.username}
								class="w-full rounded-lg border border-border bg-background px-4 py-2.5 text-sm transition focus:ring-2 focus:ring-primary focus:outline-none
            {errors.username ? 'border-destructive ring-1 ring-destructive' : ''}"
							/>
							{#if errors.username}
								<p class="text-xs text-destructive">{errors.username}</p>
							{:else if profile && fields.username.trim() !== profile.username}
								<p class="text-xs text-amber-500">
									You'll need to log in again after changing your username.
								</p>
							{/if}
						</div>

						<div class="space-y-1.5">
							<label
								for="emailInput"
								class="text-xs font-semibold tracking-widest text-muted-foreground uppercase"
								>Email</label
							>
							<input
								id="emailInput"
								type="email"
								bind:value={fields.email}
								class="w-full rounded-lg border border-border bg-background px-4 py-2.5 text-sm transition focus:ring-2 focus:ring-primary focus:outline-none
            {errors.email ? 'border-destructive ring-1 ring-destructive' : ''}"
							/>
							{#if errors.email}<p class="text-xs text-destructive">{errors.email}</p>{/if}
						</div>
						<!-- First / Last Name -->
						<div class="grid grid-cols-2 gap-4">
							<div class="space-y-1.5">
								<label
									for="firstNameInput"
									class="text-xs font-semibold tracking-widest text-muted-foreground uppercase"
									>First Name</label
								>
								<input
									id="firstNameInput"
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
								<label
									for="lastNameInput"
									class="text-xs font-semibold tracking-widest text-muted-foreground uppercase"
									>Last Name</label
								>
								<input
									id="lastNameInput"
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
							<label
								for="middleInitialInput"
								class="text-xs font-semibold tracking-widest text-muted-foreground uppercase"
							>
								Middle Initial <span class="font-normal normal-case">(optional)</span>
							</label>
							<input
								id="middleInitialInput"
								type="text"
								maxlength="1"
								bind:value={fields.middleInitial}
								class="w-full rounded-lg border border-border bg-background px-4 py-2.5 text-sm transition focus:ring-2 focus:ring-primary focus:outline-none"
							/>
						</div>

						<!-- Phone -->
						<div class="space-y-1.5">
							<label
								for="phoneInput2"
								class="text-xs font-semibold tracking-widest text-muted-foreground uppercase"
								>Phone</label
							>
							<input
								id="phoneInput2"
								type="tel"
								bind:value={fields.phone}
								oninput={(e) => {
									fields.phone = formatPhone(e.target.value);
								}}
								class="w-full rounded-lg border border-border bg-background px-4 py-2.5 text-sm transition focus:ring-2 focus:ring-primary focus:outline-none
									{errors.phone
									? 'border-destructive ring-1 ring-destructive'
									: missingFields.phone
										? 'border-amber-400 ring-1 ring-amber-400'
										: ''}"
							/>
							{#if errors.phone}<p class="text-xs text-destructive">{errors.phone}</p>{/if}
						</div>

						<!-- Business Phone -->
						<div class="space-y-1.5">
							<label
								for="businessPhoneInput"
								class="text-xs font-semibold tracking-widest text-muted-foreground uppercase"
							>
								Business Phone <span class="font-normal normal-case">(optional)</span>
							</label>
							<input
								id="businessPhoneInput"
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
							<label
								for="addressLine1Input2"
								class="text-xs font-semibold tracking-widest text-muted-foreground uppercase"
								>Address Line 1</label
							>
							<input
								id="addressLine1Input2"
								type="text"
								bind:value={fields.addressLine1}
								class="w-full rounded-lg border border-border bg-background px-4 py-2.5 text-sm transition focus:ring-2 focus:ring-primary focus:outline-none
                {errors.addressLine1
									? 'border-destructive ring-1 ring-destructive'
									: missingFields.addressLine1
										? 'border-amber-400 ring-1 ring-amber-400'
										: ''}"
							/>
							{#if errors.addressLine1}<p class="text-xs text-destructive">
									{errors.addressLine1}
								</p>{/if}
						</div>

						<div class="space-y-1.5">
							<label
								for="addressLine2Input"
								class="text-xs font-semibold tracking-widest text-muted-foreground uppercase"
							>
								Address Line 2 <span class="font-normal normal-case">(optional)</span>
							</label>
							<input
								id="addressLine2Input"
								type="text"
								bind:value={fields.addressLine2}
								class="w-full rounded-lg border border-border bg-background px-4 py-2.5 text-sm transition focus:ring-2 focus:ring-primary focus:outline-none"
							/>
						</div>

						<div class="grid grid-cols-3 gap-4">
							<div class="space-y-1.5">
								<label
									for="cityInput"
									class="text-xs font-semibold tracking-widest text-muted-foreground uppercase"
									>City</label
								>
								<input
									id="cityInput"
									type="text"
									bind:value={fields.city}
									class="w-full rounded-lg border border-border bg-background px-4 py-2.5 text-sm transition focus:ring-2 focus:ring-primary focus:outline-none
                    {errors.city
										? 'border-destructive ring-1 ring-destructive'
										: missingFields.city
											? 'border-amber-400 ring-1 ring-amber-400'
											: ''}"
								/>
								{#if errors.city}<p class="text-xs text-destructive">{errors.city}</p>{/if}
							</div>
							<div class="space-y-1.5">
								<label
									for="provinceInput"
									class="text-xs font-semibold tracking-widest text-muted-foreground uppercase"
									>Province</label
								>
								<select
									id="provinceInput"
									bind:value={fields.province}
									class="w-full rounded-lg border border-border bg-background px-4 py-2.5 text-sm transition focus:ring-2 focus:ring-primary focus:outline-none
                    {errors.province
										? 'border-destructive ring-1 ring-destructive'
										: missingFields.province
											? 'border-amber-400 ring-1 ring-amber-400'
											: ''}"
								>
									{#each provinces as p}
										<option value={p.value}>{p.label}</option>
									{/each}
								</select>
								{#if errors.province}<p class="text-xs text-destructive">{errors.province}</p>{/if}
							</div>
							<div class="space-y-1.5">
								<label
									for="postalCodeInput"
									class="text-xs font-semibold tracking-widest text-muted-foreground uppercase"
									>Postal Code</label
								>
								<input
									id="postalCodeInput"
									type="text"
									inputmode="text"
									autocomplete="postal-code"
									placeholder="A1A 1A1"
									maxlength="7"
									value={fields.postalCode}
									oninput={(e) => {
										fields.postalCode = formatCanadianPostalInput(e.currentTarget.value);
									}}
									class="w-full rounded-lg border border-border bg-background px-4 py-2.5 text-sm transition focus:ring-2 focus:ring-primary focus:outline-none
                    {errors.postalCode
										? 'border-destructive ring-1 ring-destructive'
										: missingFields.postalCode
											? 'border-amber-400 ring-1 ring-amber-400'
											: ''}"
								/>
								{#if errors.postalCode}<p class="text-xs text-destructive">
										{errors.postalCode}
									</p>{/if}
							</div>
						</div>
					</CardContent>
				</Card>

				<!-- Change Password -->
				<Card>
					<CardHeader>
						<CardTitle>Change Password</CardTitle>
						<CardDescription>Leave blank to keep your current password</CardDescription>
					</CardHeader>
					<CardContent class="space-y-4">
						<div class="space-y-1.5">
							<label
								for="currentPassword"
								class="text-xs font-semibold tracking-widest text-muted-foreground uppercase"
							>
								Current Password
							</label>
							<input
								id="currentPassword"
								type="password"
								bind:value={fields.currentPassword}
								class="w-full rounded-lg border border-border bg-background px-4 py-2.5 text-sm transition focus:ring-2 focus:ring-primary focus:outline-none
                    {errors.currentPassword ? 'border-destructive ring-1 ring-destructive' : ''}"
								placeholder="••••••••"
							/>
							{#if errors.currentPassword}<p class="text-xs text-destructive">
									{errors.currentPassword}
								</p>{/if}
						</div>
						<div class="space-y-1.5">
							<label
								for="newPassword"
								class="text-xs font-semibold tracking-widest text-muted-foreground uppercase"
							>
								New Password
							</label>
							<input
								id="newPassword"
								type="password"
								bind:value={fields.newPassword}
								class="w-full rounded-lg border border-border bg-background px-4 py-2.5 text-sm transition focus:ring-2 focus:ring-primary focus:outline-none
                    {errors.newPassword ? 'border-destructive ring-1 ring-destructive' : ''}"
								placeholder="••••••••"
							/>
							{#if errors.newPassword}<p class="text-xs text-destructive">
									{errors.newPassword}
								</p>{/if}
						</div>
						<div class="space-y-1.5">
							<label
								for="confirmPassword"
								class="text-xs font-semibold tracking-widest text-muted-foreground uppercase"
							>
								Confirm New Password
							</label>
							<input
								id="confirmPassword"
								type="password"
								bind:value={fields.confirmPassword}
								class="w-full rounded-lg border border-border bg-background px-4 py-2.5 text-sm transition focus:ring-2 focus:ring-primary focus:outline-none
                    {errors.confirmPassword ? 'border-destructive ring-1 ring-destructive' : ''}"
								placeholder="••••••••"
							/>
							{#if errors.confirmPassword}<p class="text-xs text-destructive">
									{errors.confirmPassword}
								</p>{/if}
						</div>
					</CardContent>
				</Card>

				{#if errors.general}
					<p class="text-sm text-destructive">{errors.general}</p>
				{/if}

				{#if success}
					{#if fields.username.trim() !== profile.username}
						<p class="text-sm text-green-600">Username changed. Redirecting to login...</p>
					{:else}
						<p class="text-sm text-green-600">Profile updated successfully! Redirecting...</p>
					{/if}
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

			<div class="mt-5 rounded-xl border border-amber-300/50 p-6">
				<h2 class="text-sm font-semibold text-amber-600">Deactivate Account</h2>
				<p class="mt-1 text-sm text-muted-foreground">
					Temporarily deactivate your account. You can reactivate it by contacting support.
				</p>
				<Button
					class="mt-4 bg-amber-500 text-white hover:cursor-pointer hover:bg-amber-600"
					onclick={() => (showDeactivateConfirm = true)}
				>
					Deactivate Account
				</Button>
			</div>

			<!-- <div class="mt-5 rounded-xl border border-red-300/50 p-6">
				<h2 class="text-sm font-semibold text-red-600">Delete Account</h2>
				<p class="mt-1 text-sm text-muted-foreground">
					Permanently delete your account and all associated data. This action cannot be undone.
				</p>
				<Button
					class="mt-4 bg-red-500 text-white hover:cursor-pointer hover:bg-red-600"
					onclick={() => (showDeleteConfirm = true)}
				>
					Delete Account
				</Button>
			</div> -->
		{/if}

		{#if showDeactivateConfirm}
			<div class="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
				<div class="mx-4 w-full max-w-md rounded-xl border border-amber-500 bg-card p-6 shadow-xl">
					<h2 class="text-lg font-bold text-amber-600">Deactivate Account</h2>
					<p class="mt-2 text-sm text-muted-foreground">
						Your account will be deactivated and you will be logged out. Contact support to
						reactivate it. Enter your password to confirm.
					</p>
					<label class="mt-4 block text-sm font-medium">Password</label>
					<input
						type="password"
						class="mt-1 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
						bind:value={deactivatePassword}
						autocomplete="current-password"
					/>
					{#if errors.deactivatePassword}
						<p class="mt-1 text-sm text-destructive">{errors.deactivatePassword}</p>
					{/if}
					<div class="mt-6 flex justify-end gap-3">
						<Button
							variant="outline"
							onclick={() => {
								showDeactivateConfirm = false;
								deactivatePassword = '';
								errors.deactivatePassword = '';
							}}>Cancel</Button
						>
						<Button
							class="bg-amber-500 text-white hover:bg-amber-600"
							disabled={deactivating}
							onclick={handleDeactivate}
						>
							{deactivating ? 'Deactivating...' : 'Yes, deactivate my account'}
						</Button>
					</div>
				</div>
			</div>
		{/if}

		{#if showDeleteConfirm}
			<div class="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
				<div class="mx-4 w-full max-w-md rounded-xl border border-red-500 bg-card p-6 shadow-xl">
					<h2 class="text-lg font-bold text-red-600">Delete Account</h2>
					<p class="mt-2 text-sm text-muted-foreground">
						This will permanently delete your account and all associated data. This cannot be
						undone.
					</p>
					<div class="mt-6 flex justify-end gap-3">
						<Button variant="outline" onclick={() => (showDeleteConfirm = false)}>Cancel</Button>
						<Button
							class="bg-red-500 text-white hover:bg-red-600"
							disabled={deleting}
							onclick={handleDelete}
						>
							{deleting ? 'Deleting...' : 'Yes, delete my account'}
						</Button>
					</div>
				</div>
			</div>
		{/if}
	</main>
</div>
