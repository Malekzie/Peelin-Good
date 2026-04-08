<script>
	export let preferences;
	export let toggle;
</script>

<div class="grid grid-cols-1 gap-8 md:grid-cols-2">
	<!-- Notifications -->
	<div class="bg-surface border-outline-variant space-y-6 rounded-xl border p-8 md:col-span-2">
		<h3 class="text-on-surface text-xl font-bold">Notifications</h3>

		<div class="space-y-5">
			{#each [{ key: 'emailNotifications', label: 'Email Notifications', desc: 'Order updates & promos' }, { key: 'smsNotifications', label: 'SMS Notifications', desc: 'Delivery alerts' }] as item (item.key)}
				<div class="flex items-center justify-between">
					<div>
						<p class="text-on-surface font-medium">{item.label}</p>
						<p class="text-on-surface-variant text-sm">{item.desc}</p>
					</div>

					<button
						aria-label="Toggle {item.label}"
						on:click={() => toggle(item.key)}
						class={`relative h-6 w-11 rounded-full transition ${
							preferences[item.key] ? 'bg-primary/90' : 'bg-secondary/90'
						}`}
					>
						<div
							class={`h-6 w-6 transform rounded-full bg-white shadow-lg ring-2 ring-black/10 transition ${
								preferences[item.key] ? 'translate-x-5' : ''
							}`}
						></div>
					</button>
				</div>
			{/each}
		</div>
	</div>

	<!-- Dietary -->
	<div class="bg-surface border-outline-variant space-y-6 rounded-xl border p-8">
		<h3 class="text-on-surface text-xl font-bold">Dietary Preferences</h3>

		<div class="space-y-5">
			{#each [{ key: 'veganOnly', label: 'Vegan Only' }, { key: 'glutenFree', label: 'Gluten Free' }] as item (item.key)}
				<div class="flex items-center justify-between">
					<p class="text-on-surface font-medium">{item.label}</p>

					<button
						aria-label="Toggle {item.label}"
						on:click={() => toggle(item.key)}
						class={`relative h-6 w-11 rounded-full transition ${
							preferences[item.key] ? 'bg-primary/90' : 'bg-secondary/90'
						}`}
					>
						<div
							class={`h-6 w-6 transform rounded-full bg-white shadow-lg ring-2 ring-black/10 transition ${
								preferences[item.key] ? 'translate-x-5' : ''
							}`}
						></div>
					</button>
				</div>
			{/each}
		</div>
	</div>

	<!-- Allergies -->
	<div class="bg-surface border-outline-variant space-y-6 rounded-xl border p-8">
		<h3 class="text-on-surface text-xl font-bold">Allergies</h3>

		<div class="space-y-5">
			{#each [{ key: 'nuts', label: 'Nuts' }, { key: 'dairy', label: 'Dairy' }, { key: 'eggs', label: 'Eggs' }, { key: 'soy', label: 'Soy' }] as item (item.key)}
				<div class="flex items-center justify-between">
					<p class="text-on-surface font-medium">{item.label}</p>

					<button
						aria-label="Toggle {item.label} allergy"
						on:click={() => toggle(item.key)}
						class={`relative h-6 w-11 rounded-full transition ${
							preferences[item.key] ? 'bg-primary/90' : 'bg-secondary/90'
						}`}
					>
						<div
							class={`h-6 w-6 transform rounded-full bg-white shadow-lg ring-2 ring-black/10 transition ${
								preferences[item.key] ? 'translate-x-5' : ''
							}`}
						></div>
					</button>
				</div>
			{/each}
		</div>

		<div>
			<label for="otherAllergiesInput" class="text-outline text-[11px] font-bold uppercase">
				Other Allergies
			</label>
			<input
				id="otherAllergiesInput"
				type="text"
				bind:value={preferences.otherAllergies}
				class="bg-surface-container border-outline-variant mt-2 w-full rounded-lg border px-4 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
			/>
		</div>
	</div>

	<!-- Delivery -->
	<div class="bg-surface border-outline-variant space-y-6 rounded-xl border p-8 md:col-span-2">
		<h3 class="text-on-surface text-xl font-bold">Delivery Preferences</h3>

		<select
			bind:value={preferences.preferredDeliveryTime}
			class="bg-surface-container border-outline-variant mt-2 w-full rounded-lg border px-4 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
		>
			<option value="morning">Morning</option>
			<option value="afternoon">Afternoon</option>
			<option value="evening">Evening</option>
		</select>
	</div>
</div>
