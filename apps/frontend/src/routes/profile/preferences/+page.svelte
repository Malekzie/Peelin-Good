<script>
	import { Skeleton } from '$lib/components/ui/skeleton';
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { user } from '$lib/stores/authStore';
	import { getTags } from '$lib/services/tags';
	import { getMyPreferences, saveMyPreferences } from '$lib/services/preferences';

	let tags = $state([]);
	let preferences = $state({});
	let loading = $state(true);
	let saving = $state(false);
	let success = $state(false);
	let error = $state(null);

	// Dietary tags get all 3 options including allergic
	const TYPES_DIETARY = [
		{ value: 'like', label: '👍 Like', color: 'bg-green-100 text-green-700 border-green-300' },
		{ value: 'dislike', label: '👎 Dislike', color: 'bg-red-100 text-red-700 border-red-300' },
		{
			value: 'allergic',
			label: '⚠️ Allergic',
			color: 'bg-amber-100 text-amber-700 border-amber-300'
		}
	];

	// Category tags only get like/dislike
	const TYPES_CATEGORY = [
		{ value: 'like', label: '👍 Like', color: 'bg-green-100 text-green-700 border-green-300' },
		{ value: 'dislike', label: '👎 Dislike', color: 'bg-red-100 text-red-700 border-red-300' }
	];

	// Driven by the dietary field from the API — no hardcoded IDs
	const dietaryTags = $derived(tags.filter((t) => t.dietary));
	const categoryTags = $derived(tags.filter((t) => !t.dietary));

	onMount(async () => {
		if ($user?.role !== 'customer') {
			goto(resolve('/profile'));
			return;
		}
		try {
			const [tagsData, prefsData] = await Promise.all([getTags(), getMyPreferences()]);
			tags = tagsData;
			preferences = Object.fromEntries(prefsData.map((p) => [p.tagId, p.preferenceType]));
		} catch {
			error = true;
		} finally {
			loading = false;
		}
	});

	function setPreference(tagId, type) {
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
</script>

<main class="flex-1 overflow-y-auto p-8 lg:p-10">
		<div class="mx-auto max-w-3xl space-y-8">
			<div>
				<h1 class="text-2xl font-bold tracking-tight text-foreground">Preferences</h1>
				<p class="mt-1 text-sm text-muted-foreground">
					Tell us what you like and dislike. We'll use this to personalize your recommendations.
				</p>
			</div>

			{#if loading}
				<div class="space-y-8">
					<div class="space-y-3">
						<Skeleton class="h-4 w-32" />
						{#each Array(4) as _, i (i)}
							<Skeleton class="h-14 w-full rounded-xl" />
						{/each}
					</div>
					<div class="space-y-3">
						<Skeleton class="h-4 w-32" />
						{#each Array(5) as _, i (i)}
							<Skeleton class="h-14 w-full rounded-xl" />
						{/each}
					</div>
				</div>
			{:else if error}
				<p class="text-sm text-destructive">Failed to load preferences.</p>
			{:else}
				<div class="space-y-8">
					<!-- Dietary Needs -->
					<div>
						<h2 class="mb-1 text-sm font-semibold tracking-widest text-muted-foreground uppercase">
							Dietary Needs
						</h2>
						<p class="mb-4 text-xs text-muted-foreground">
							These apply to ingredients and dietary options. You can mark them as a preference or
							an allergy.
						</p>
						<div class="space-y-3">
							{#each dietaryTags as tag (tag.id)}
								<div
									class="flex items-center justify-between rounded-xl border border-border bg-card px-5 py-4"
								>
									<p class="text-sm font-medium text-foreground">{tag.name}</p>
									<div class="flex gap-2">
										{#each TYPES_DIETARY as type (type.value)}
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
					</div>

					<!-- Favourite Categories -->
					<div>
						<h2 class="mb-1 text-sm font-semibold tracking-widest text-muted-foreground uppercase">
							Favourite Categories
						</h2>
						<p class="mb-4 text-xs text-muted-foreground">
							Tell us what kinds of baked goods you enjoy most.
						</p>
						<div class="space-y-3">
							{#each categoryTags as tag (tag.id)}
								<div
									class="flex items-center justify-between rounded-xl border border-border bg-card px-5 py-4"
								>
									<p class="text-sm font-medium text-foreground">{tag.name}</p>
									<div class="flex gap-2">
										{#each TYPES_CATEGORY as type (type.value)}
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
					</div>
				</div>

				{#if success}
					<p class="mt-4 text-sm text-green-600">Preferences saved!</p>
				{/if}

				<div class="flex justify-end pt-4">
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
