<script>
	import ProfileSidebar from '$lib/components/profile/ProfileSidebar.svelte';
	import ProfileDetails from '$lib/components/profile/ProfileDetails.svelte';
	import ProfileRecomendations from '$lib/components/profile/ProfileRecomendations.svelte';
	import { Avatar, AvatarImage, AvatarFallback } from '$lib/components/ui/avatar';
	import { Badge } from '$lib/components/ui/badge';
	import { Button } from '$lib/components/ui/button';
	import { Separator } from '$lib/components/ui/separator';
	import { Skeleton } from '$lib/components/ui/skeleton';
	import { isProfileComplete } from '$lib/utils/profile';
	import { onMount } from 'svelte';
	import { getProfile } from '$lib/services/profile';
	import { logoutUser } from '$lib/services/auth';
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';

	let profile = $state(null);
	let loading = $state(true);
	let error = $state(null);

	onMount(async () => {
		try {
			profile = await getProfile();
		} catch {
			error = true;
		} finally {
			loading = false;
		}
	});

	const initials = $derived(
		profile
			? [profile.firstName?.[0], profile.lastName?.[0]].filter(Boolean).join('').toUpperCase()
			: ''
	);

	function onEditProfile() {
		goto(resolve('/profile/edit'));
	}
</script>

{#if loading}
	<div class="flex h-[calc(100dvh-var(--app-navbar-height))] overflow-hidden">
		<div class="hidden w-72 border-r border-border bg-card md:block"></div>
		<main class="flex-1 overflow-y-auto p-10">
			<div class="mx-auto max-w-5xl space-y-8">
				<div class="flex items-center gap-6 pb-6">
					<Skeleton class="h-24 w-24 rounded-full" />
					<div class="space-y-3">
						<Skeleton class="h-7 w-48" />
						<Skeleton class="h-4 w-32" />
						<Skeleton class="h-5 w-24 rounded-full" />
					</div>
				</div>
				<Skeleton class="h-56 w-full rounded-xl" />
			</div>
		</main>
	</div>
{:else if error}
	<div
		class="flex h-[calc(100dvh-var(--app-navbar-height))] items-center justify-center overflow-hidden bg-background px-6"
	>
		<div
			class="w-full max-w-md rounded-2xl border border-border bg-card p-10 text-center shadow-sm"
		>
			<h1 class="mb-2 text-2xl font-bold text-foreground">Profile unavailable</h1>
			<p class="mb-8 text-sm text-muted-foreground">
				We couldn't load your profile. This section may not be available for your account type.
			</p>
			<div class="flex flex-col gap-3">
				<Button href={resolve('/')} class="w-full">Go to homepage</Button>
				<Button
					variant="outline"
					class="w-full"
					onclick={async () => {
						await logoutUser();
						goto(resolve('/'));
					}}
				>
					Log out
				</Button>
			</div>
		</div>
	</div>
{:else}
	<div class="flex h-[calc(100dvh-var(--app-navbar-height))] overflow-hidden bg-background">
		<ProfileSidebar />

		<main class="flex-1 overflow-y-auto p-8 lg:p-10">
			{#if profile && !isProfileComplete(profile)}
				<div
					class="mb-6 rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800"
				>
					Your profile is incomplete. Add your address and phone number to place orders.
					<a href={resolve('/profile/edit')} class="ml-2 font-semibold underline"
						>Complete profile</a
					>
				</div>
			{/if}

			<div class="mx-auto max-w-5xl space-y-8">
				<!-- Profile header -->
				<div class="flex flex-col gap-6 sm:flex-row sm:items-center sm:justify-between">
					<div class="flex items-center gap-5">
						<div class="relative">
							<Avatar class="h-24 w-24 ring-2 ring-border ring-offset-2">
								<AvatarImage
									src={profile.profilePhotoPath}
									alt="{profile.firstName} {profile.lastName}"
									class={profile.photoApprovalPending ? 'opacity-60 grayscale' : ''}
								/>
								<AvatarFallback class="bg-primary text-2xl font-bold text-primary-foreground">
									{initials}
								</AvatarFallback>
							</Avatar>
							{#if profile.photoApprovalPending}
								<div
									class="absolute -bottom-1 left-1/2 -translate-x-1/2 rounded-full bg-amber-500 px-2 py-0.5 text-[10px] font-semibold whitespace-nowrap text-white"
								>
									Pending approval
								</div>
							{/if}
						</div>

						<div class="space-y-2">
							<h1 class="text-3xl font-bold tracking-tight text-foreground">
								{profile.firstName}
								{profile.lastName}
							</h1>
							<p class="text-sm text-muted-foreground">{profile.email}</p>
							<div class="flex flex-wrap gap-2">
								{#if profile.loyaltyTier}
									<Badge>{profile.loyaltyTier}</Badge>
								{:else}
									<Badge variant="secondary">No loyalty tier</Badge>
								{/if}
								{#if profile.rewardBalance != null && profile.rewardBalance > 0}
									<Badge variant="outline">{profile.rewardBalance.toLocaleString()} pts</Badge>
								{/if}
							</div>
						</div>
					</div>

					<Button onclick={onEditProfile} variant="outline">Edit Profile</Button>
				</div>

				<Separator />

				<!-- Main grid -->
				<div class="grid grid-cols-1 gap-6 md:grid-cols-12">
					<ProfileDetails {profile} />
					<ProfileRecomendations />
				</div>
			</div>
		</main>
	</div>
{/if}
