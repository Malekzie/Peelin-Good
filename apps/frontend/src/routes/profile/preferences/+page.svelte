<script>
	import ProfileSidebar from '$lib/components/profile/ProfileSidebar.svelte';
	import { onMount } from 'svelte';
	import { getTags } from '$lib/services/tags';
	import { getMyPreferences, saveMyPreferences } from '$lib/services/preferences';

	let tags = $state([]);
	let preferences = $state({});
	let loading = $state(true);
	let saving = $state(false);
	let success = $state(false);
	let error = $state(null);

	onMount(async () => {
		try {
			const [tagsData, prefsData] = await Promise.all([getTags(), getMyPreferences()]);
			tags = tagsData;
			// Build a map of tagId -> preferenceType
			preferences = Object.fromEntries(prefsData.map((p) => [p.tagId, p.preferenceType]));
		} catch {
			error = true;
		} finally {
			loading = false;
		}
	});

	function setPreference(tagId, type) {
		// Toggle off if already selected
		if (preferences[tagId] === type) {
			const { [tagId]: _, ...rest } = preferences;
			preferences = rest;
		} else {
			preferences = { ...preferences, [tagId]: type };
		}
	}

	async function handleSave() {
		saving = true;
		success = false;
		try {
			const payload = Object.entries(preferences).map(([tagId, preferenceType]) => ({
				tagId: parseInt(tagId),
				preferenceType
			}));
			await saveMyPreferences(payload);
			success = true;
			setTimeout(() => (success = false), 2000);
		} catch {
			error = true;
		} finally {
			saving = false;
		}
	}

	const TYPES = [
		{ value: 'like', label: '👍 Like', color: 'bg-green-100 text-green-700 border-green-300' },
		{ value: 'dislike', label: '👎 Dislike', color: 'bg-red-100 text-red-700 border-red-300' },
		{
			value: 'allergic',
			label: '⚠️ Allergic',
			color: 'bg-amber-100 text-amber-700 border-amber-300'
		}
	];
</script>

<div class="flex min-h-screen bg-background">
	<ProfileSidebar />

	<main class="flex-1 overflow-y-auto p-8 lg:p-10">
		<div class="mx-auto max-w-3xl space-y-8">
			<div>
				<h1 class="text-2xl font-bold tracking-tight text-foreground">Preferences</h1>
				<p class="mt-1 text-sm text-muted-foreground">
					Tell us what you like, dislike, or are allergic to — we'll use this to personalize your
					recommendations.
				</p>
			</div>

			{#if loading}
				<div class="flex justify-center py-24">
					<div
						class="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent"
					></div>
				</div>
			{:else if error}
				<p class="text-sm text-destructive">Failed to load preferences.</p>
			{:else}
				<div class="space-y-3">
					{#each tags as tag (tag.id)}
						<div
							class="flex items-center justify-between rounded-xl border border-border bg-card px-5 py-4"
						>
							<p class="text-sm font-medium text-foreground">{tag.name}</p>
							<div class="flex gap-2">
								{#each TYPES as type (type.value)}
									<button
										onclick={() => setPreference(tag.id, type.value)}
										class="rounded-full border px-3 py-1 text-xs font-semibold transition
                                            {preferences[tag.id] === type.value
											? type.color + ' border-2'
											: 'border-border bg-background text-muted-foreground hover:bg-muted'}"
									>
										{type.label}
									</button>
								{/each}
							</div>
						</div>
					{/each}
				</div>

				{#if success}
					<p class="text-sm text-green-600">Preferences saved!</p>
				{/if}

				<div class="flex justify-end">
					<button
						onclick={handleSave}
						disabled={saving}
						class="rounded-full bg-primary px-8 py-3 text-sm font-bold text-primary-foreground transition hover:opacity-90 disabled:opacity-50"
					>
						{saving ? 'Saving...' : 'Save Preferences'}
					</button>
				</div>
			{/if}
		</div>
	</main>
</div>
