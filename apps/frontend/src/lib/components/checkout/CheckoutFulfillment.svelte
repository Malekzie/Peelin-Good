<script lang="ts">
	import { formatBakeryOption, distanceLabel, type Bakery } from '$lib/services/checkout';
	import type { SavedAddress, CustomerProfile } from '$lib/services/checkout';

	interface Props {
		bakeries: Bakery[];
		selectedBakeryId: number | null;
		orderMethod: 'pickup' | 'delivery';
		userLat: number | null;
		userLng: number | null;
		isGuest: boolean;
		customer: CustomerProfile | null;
		useCustomAddress: boolean;
		deliveryLine1: string;
		deliveryLine2: string;
		deliveryCity: string;
		deliveryProvince: string;
		deliveryPostal: string;
		onBakeryChange: (id: number) => void;
		onMethodChange: (method: 'pickup' | 'delivery') => void;
		onUseCustomAddress: () => void;
		onUseSavedAddress: () => void;
	}

	let {
		bakeries,
		selectedBakeryId = $bindable(),
		orderMethod = $bindable(),
		userLat,
		userLng,
		isGuest,
		customer,
		useCustomAddress = $bindable(),
		deliveryLine1,
		deliveryLine2,
		deliveryCity,
		deliveryProvince,
		deliveryPostal,
		onBakeryChange,
		onMethodChange,
		onUseCustomAddress,
		onUseSavedAddress
	}: Props = $props();

	const selectedBakery = $derived(bakeries.find((b) => b.id === selectedBakeryId) ?? null);
</script>

<section class="rounded-xl border border-border bg-card p-6 shadow-sm">
	<h2 class="mb-4 text-lg font-semibold text-foreground">Fulfillment</h2>

	<!-- Pickup / Delivery toggle -->
	<div class="mb-5 flex gap-3">
		{#each [['pickup', 'Pickup'], ['delivery', 'Delivery']] as [val, label] (val)}
			<button
				type="button"
				onclick={() => {
					orderMethod = val as 'pickup' | 'delivery';
					onMethodChange(val as 'pickup' | 'delivery');
				}}
				class="flex-1 rounded-lg border py-2 text-sm font-medium transition-colors {orderMethod ===
				val
					? 'border-primary bg-primary text-primary-foreground'
					: 'border-border bg-background text-foreground hover:bg-muted'}"
			>
				{label}
			</button>
		{/each}
	</div>

	<!-- Bakery selector -->
	<div class="mb-4 flex flex-col gap-1">
		<label for="bakery" class="text-sm font-medium text-foreground">
			{orderMethod === 'pickup' ? 'Pickup Location' : 'Nearest Bakery'}
			<span class="text-destructive">*</span>
		</label>
		{#if userLat !== null}
			<p class="mb-1 text-xs text-muted-foreground">Sorted by distance from your location</p>
		{/if}
		<select
			id="bakery"
			bind:value={selectedBakeryId}
			onchange={() => onBakeryChange(selectedBakeryId ?? 0)}
			required
			class="rounded-lg border border-input bg-background px-3 py-2 text-sm text-foreground focus:ring-2 focus:ring-ring focus:outline-none"
		>
			{#each bakeries as b (b.id)}
				<option value={b.id}>{formatBakeryOption(b, userLat, userLng)}</option>
			{/each}
		</select>
	</div>

	<!-- Selected bakery address -->
	{#if selectedBakery?.address}
		<div class="mb-4 rounded-lg border border-border bg-muted/50 p-3 text-sm">
			<p class="mb-1 text-xs font-semibold tracking-wider text-muted-foreground uppercase">
				{orderMethod === 'pickup' ? 'Pickup address' : 'Bakery address'}
			</p>
			<p class="text-foreground">
				{selectedBakery.address.line1}{selectedBakery.address.line2
					? `, ${selectedBakery.address.line2}`
					: ''}
			</p>
			<p class="text-foreground">
				{selectedBakery.address.city}, {selectedBakery.address.province}
				{selectedBakery.address.postalCode}
			</p>
			{#if selectedBakery.latitude && selectedBakery.longitude && userLat !== null}
				<p class="mt-1 text-xs font-medium text-primary">
					{distanceLabel(selectedBakery, userLat, userLng)}
				</p>
			{/if}
		</div>
	{/if}

	<!-- Delivery address (logged-in with saved address) -->
	{#if orderMethod === 'delivery' && !isGuest && customer?.address}
		{#if !useCustomAddress}
			<div class="rounded-lg border border-border bg-muted/50 p-3 text-sm">
				<div class="flex items-start justify-between gap-2">
					<div>
						<p class="mb-1 text-xs font-semibold tracking-wider text-muted-foreground uppercase">
							Delivering to
						</p>
						<p class="text-foreground">
							{customer.address.line1}{customer.address.line2 ? `, ${customer.address.line2}` : ''}
						</p>
						<p class="text-foreground">
							{customer.address.city}, {customer.address.province}
							{customer.address.postalCode}
						</p>
					</div>
					<button
						type="button"
						onclick={onUseCustomAddress}
						class="shrink-0 rounded-lg border border-border px-3 py-1.5 text-xs font-medium text-foreground transition-colors hover:bg-muted"
					>
						Use different address
					</button>
				</div>
			</div>
		{:else}
			<div class="mb-2 flex items-center justify-between">
				<p class="text-sm font-medium text-foreground">Delivery address</p>
				<button
					type="button"
					onclick={onUseSavedAddress}
					class="text-xs text-primary hover:underline"
				>
					Use saved address
				</button>
			</div>
		{/if}
	{/if}
</section>
