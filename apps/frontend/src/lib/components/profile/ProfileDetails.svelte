<script>
	import {
		Card,
		CardContent,
		CardHeader,
		CardTitle,
		CardDescription
	} from '$lib/components/ui/card';
	import { Separator } from '$lib/components/ui/separator';
	import { Star } from '@lucide/svelte';

	let { profile } = $props();

	const tierLabel = $derived(profile.loyaltyTier ?? profile.rewardTierName ?? null);

	/** API: rewardTierDiscountPercent — whole percent e.g. 5 for 5% */
	const tierDiscountLabel = $derived.by(() => {
		const raw = profile.rewardTierDiscountPercent;
		if (raw == null || raw === '') return null;
		const n = Number(raw);
		if (!Number.isFinite(n) || n <= 0) return null;
		const rounded = Number.isInteger(n) ? String(n) : n.toFixed(1).replace(/\.0$/, '');
		return `${rounded}% off eligible orders`;
	});

	const fields = $derived([
		{
			label: 'Full Name',
			value: [
				profile.firstName,
				profile.middleInitial ? profile.middleInitial + '.' : null,
				profile.lastName
			]
				.filter(Boolean)
				.join(' ')
		},
		{ label: 'Username', value: profile.username },
		{ label: 'Email', value: profile.email },
		{
			label: 'Phone',
			value:
				profile.phone && !profile.phone.toUpperCase().startsWith('OAUTH-') ? profile.phone : '—'
		}
	]);
</script>

<div class="flex flex-col gap-6 md:col-span-8">
	<!-- Personal Info -->
	<Card>
		<CardHeader>
			<CardTitle>Personal Information</CardTitle>
			<CardDescription>Your account details and contact info</CardDescription>
		</CardHeader>
		<CardContent>
			<dl class="grid grid-cols-1 gap-x-8 gap-y-5 sm:grid-cols-2">
				{#each fields as field (field.label)}
					<div class="flex flex-col gap-1">
						<dt class="text-[11px] font-semibold tracking-widest text-muted-foreground uppercase">
							{field.label}
						</dt>
						<dd class="text-sm font-medium text-foreground">{field.value}</dd>
					</div>
				{/each}
			</dl>
		</CardContent>
	</Card>

	{#if profile.employeeDiscountEligible}
		<Card class="bg-[#FAF7F2]">
			<CardContent class="pt-6">
				<div class="flex items-start gap-3">
					<div
						class="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-primary/10"
					>
						<Star class="h-5 w-5 text-primary" />
					</div>
					<div>
						<p class="text-xs font-semibold tracking-wide text-muted-foreground uppercase">
							Employee benefit
						</p>
						<p class="text-lg font-bold text-foreground">20% employee discount</p>
						<p class="mt-1 text-sm text-muted-foreground">
							You are eligible at checkout. This applies after today&apos;s specials and your
							loyalty tier discount.
						</p>
					</div>
				</div>
			</CardContent>
		</Card>
	{/if}

	<!-- Loyalty -->
	{#if profile.rewardBalance != null || tierLabel}
		<Card class="bg-[#FAF7F2]">
			<CardContent class="pt-6">
				<div class="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
					<div class="flex items-start gap-3">
						<div
							class="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-primary/10"
						>
							<Star class="h-5 w-5 text-primary" />
						</div>
						<div>
							{#if tierLabel}
								<p class="text-xs font-semibold tracking-wide text-muted-foreground uppercase">
									Reward tier
								</p>
								<p class="text-lg font-bold text-foreground">{tierLabel}</p>
							{/if}
							{#if tierDiscountLabel}
								<p class="mt-1 text-sm font-medium text-primary">{tierDiscountLabel}</p>
							{:else if tierLabel}
								<p class="mt-1 text-sm text-muted-foreground">
									No order discount configured for this tier.
								</p>
							{/if}
							<p class="text-sm font-semibold text-foreground {tierLabel ? 'mt-3' : ''}">
								Loyalty points
							</p>
							<p class="text-2xl font-bold text-primary">
								{Number(profile.rewardBalance ?? 0).toLocaleString()}
								<span class="text-sm font-normal text-muted-foreground">pts</span>
							</p>
						</div>
					</div>
				</div>

				<Separator class="my-4" />

				<p class="text-xs text-muted-foreground">
					Earn points with every order. Your tier discount applies automatically at checkout when
					available.
				</p>
			</CardContent>
		</Card>
	{/if}
</div>
