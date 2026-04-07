<script>
	import {
		Card,
		CardContent,
		CardHeader,
		CardTitle,
		CardDescription
	} from '$lib/components/ui/card';
	import { Separator } from '$lib/components/ui/separator';
	import { Button } from '$lib/components/ui/button';
	import { Star } from '@lucide/svelte';

	let { profile } = $props();

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
		{ label: 'Phone', value: profile.phone ?? '—' }
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

	<!-- Loyalty Points -->
	{#if profile.rewardBalance != null}
		<Card class="bg-[#FAF7F2]">
			<CardContent class="pt-6">
				<div class="flex items-center justify-between">
					<div class="flex items-center gap-3">
						<div class="flex h-10 w-10 items-center justify-center rounded-full bg-primary/10">
							<Star class="h-5 w-5 text-primary" />
						</div>
						<div>
							<p class="text-sm font-semibold text-foreground">Loyalty Points</p>
							<p class="text-2xl font-bold text-primary">
								{profile.rewardBalance.toLocaleString()}
								<span class="text-sm font-normal text-muted-foreground">pts</span>
							</p>
						</div>
					</div>
					<Button variant="outline" size="sm">View rewards</Button>
				</div>

				<Separator class="my-4" />

				<p class="text-xs text-muted-foreground">
					Earn points with every order and redeem them for discounts on future purchases.
				</p>
			</CardContent>
		</Card>
	{/if}
</div>
