<script>
	import ProfilePreferencesCards from '$lib/components/ProfilePreferencesCards.svelte';
	import ProfileSidebar from '$lib/components/ProfileSidebar.svelte';

	let preferences = {
		emailNotifications: true,
		smsNotifications: false,
		veganOnly: false,
		glutenFree: false,
		preferredDeliveryTime: 'morning',

		nuts: false,
		dairy: false,
		eggs: false,
		soy: false,
		otherAllergies: ''
	};

	function toggle(key) {
		preferences[key] = !preferences[key];
		preferences = { ...preferences };
	}

	function handleSave() {
		console.log('Saved preferences:', preferences);
	}
</script>

<div class="flex min-h-screen">
	<!-- Sidebar -->
	<ProfileSidebar />

	<!-- Main Content -->
	<main class="flex-1 px-6 py-10">
		<div class="mx-auto max-w-5xl space-y-12">
			<section class="border-outline-variant border-b pb-6">
				<h1 class="font-headline text-on-surface mt-5 text-3xl font-black">Preferences</h1>
			</section>

			<ProfilePreferencesCards {preferences} {toggle} />

			<div class="mb-5 flex justify-end">
				<button
					on:click={handleSave}
					class="rounded-full bg-primary px-8 py-3 text-xs font-bold text-white uppercase hover:bg-primary/90"
				>
					Save Preferences
				</button>
			</div>
		</div>
	</main>
</div>
