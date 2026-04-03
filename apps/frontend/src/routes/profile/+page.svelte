<script>
	import ProfileSidebar from '$lib/components/ProfileSidebar.svelte';
	import ProfileDetails from '$lib/components/ProfileDetails.svelte';
	import ProfileRecomendations from '$lib/components/ProfileRecomendations.svelte';
	import { onMount } from 'svelte';
	import { getProfile } from '$lib/services/profile';
	import { logoutUser } from '$lib/services/auth';
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';

	let profile = null;
	let loading = true;
	let error = null;

	onMount(async () => {
		try {
			profile = await getProfile();
		} catch {
			error = 'Failed to load profile. Please try again later.';
		} finally {
			loading = false;
		}
	});
</script>

{#if loading}
	<p>Loading...</p>
{:else if error}
	<div class="flex min-h-screen items-center justify-center bg-[#FAF7F2] px-6">
		<div class="w-full max-w-md rounded-2xl border border-border bg-white p-10 text-center shadow-sm">
			<h1 class="mb-2 font-serif text-2xl font-bold text-foreground">Profile unavailable</h1>
			<p class="mb-8 text-sm text-muted-foreground">
				We couldn't load your profile. This section may not be available for your account type.
			</p>
			<div class="flex flex-col gap-3">
				<a
					href={resolve('/')}
					class="w-full rounded-lg bg-primary py-3 text-sm font-semibold text-primary-foreground transition-colors hover:opacity-90"
				>
					Go to homepage
				</a>
				<button
					onclick={async () => { await logoutUser(); goto(resolve('/')); }}
					class="w-full rounded-lg border border-border py-3 text-sm font-semibold text-foreground transition-colors hover:bg-muted"
				>
					Log out
				</button>
			</div>
		</div>
	</div>
{:else}
	<div class="bg-surface-container-low flex min-h-screen">
		<!-- Sidebar -->
		<ProfileSidebar />

		<!-- Main Content -->
		<main class="flex-1 overflow-y-auto p-10">
			<div class="mx-auto max-w-5xl space-y-12">
				<!-- Header -->
				<section
					class="border-surface-container-high flex flex-col justify-between gap-8 border-b pb-6 md:flex-row md:items-center"
				>
					<div class="flex items-center gap-8">
						<div class="group relative">
							<div
								class="bg-primary-container h-32 w-32 overflow-hidden rounded-full shadow-xl ring-1 ring-white"
							>
								<img
									class="h-full w-full object-cover"
									alt="Customer profile picture"
									src={profile.profilePhotoPath ??
										'https://peelin-good-storage.tor1.cdn.digitaloceanspaces.com/bakery/default-profile.jpg'}
								/>
							</div>
							<button
								class="absolute right-1 bottom-1 transform rounded-full border border-stone-100 bg-white p-2.5 text-primary shadow-md transition-all duration-300 ease-in-out hover:cursor-pointer hover:bg-primary hover:text-white hover:shadow-lg"
							>
								Edit
							</button>
						</div>

						<div class="space-y-2">
							<h1 class="font-headline text-on-surface text-4xl font-black tracking-tight">
								{profile.firstName}
								{profile.lastName}
							</h1>
							<p class="text-on-surface-variant font-medium">{profile.email}</p>
							<div class="mt-3 flex flex-wrap gap-3">
								<span class="rounded-full bg-[#ffdbcd] px-4 py-1 text-[10px] font-bold uppercase">
									Loyalty Tier: {profile.loyaltyTier ?? 'None'}
								</span>
								<span
									class="bg-surface-container-highest rounded-full px-4 py-1 text-[10px] font-bold uppercase"
								>
									Frequent Buyer
								</span>
							</div>
						</div>
					</div>

					<div class="flex justify-end">
						<button
							class="rounded-full bg-primary px-6 py-2.5 text-sm font-semibold text-white hover:cursor-pointer hover:bg-primary/90"
						>
							Edit Profile
						</button>
					</div>
				</section>

				<!-- Grid -->
				<div class="grid grid-cols-1 gap-8 md:grid-cols-12">
					<!-- Profile Info -->
					{#if profile}
						<ProfileDetails {profile} />
					{/if}

					<!-- Recommended TODO ADD AI -->
					<ProfileRecomendations />
				</div>
			</div>
		</main>
	</div>
{/if}
