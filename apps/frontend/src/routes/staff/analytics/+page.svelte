<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { user } from '$lib/stores/authStore';
	import { Skeleton } from '$lib/components/ui/skeleton';
	import * as Chart from '$lib/components/ui/chart/index.js';
	import { BarChart, LineChart } from 'layerchart';
	import { scaleBand } from 'd3-scale';
	import KpiCard from '$lib/components/staff/KpiCard.svelte';
	import {
		getBakeryNames,
		getTotalRevenue,
		getAverageOrderValue,
		getCompletionRate,
		getRevenueOverTime,
		getRevenueByBakery,
		getTopProducts,
		getSalesByEmployee
	} from '$lib/services/analytics.js';
	import { formatPriceCad } from '$lib/utils/money';

	const today = new Date().toISOString().split('T')[0];
	const thirtyDaysAgo = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0];

	let startDate = $state(thirtyDaysAgo);
	let endDate = $state(today);
	let selectedBakery = $state('');
	let bakeryNames = $state<string[]>([]);

	let totalRevenue = $state<number | null>(null);
	let aov = $state<number | null>(null);
	let completionRate = $state<number | null>(null);
	let revenueOverTime = $state<{ label: string; value: number }[]>([]);
	let revenueByBakery = $state<{ label: string; value: number }[]>([]);
	let topProducts = $state<{ label: string; value: number }[]>([]);
	let salesByEmployee = $state<{ label: string; value: number }[]>([]);

	let loading = $state(true);
	let error = $state(false);

	const chartConfig = {
		value: { label: 'Value', color: '#C25F1A' }
	} satisfies Chart.ChartConfig;

	onMount(async () => {
		if ($user?.role !== 'admin') {
			goto(resolve('/staff/dashboard'), { replaceState: true });
			return;
		}
		await Promise.all([getBakeryNames().then((n) => (bakeryNames = n)), loadData()]);
	});

	async function loadData() {
		loading = true;
		error = false;
		const bakery = selectedBakery || undefined;
		try {
			[
				totalRevenue,
				aov,
				completionRate,
				revenueOverTime,
				revenueByBakery,
				topProducts,
				salesByEmployee
			] = await Promise.all([
				getTotalRevenue(startDate, endDate, bakery),
				getAverageOrderValue(startDate, endDate, bakery),
				getCompletionRate(startDate, endDate, bakery),
				getRevenueOverTime(startDate, endDate, bakery),
				getRevenueByBakery(startDate, endDate),
				getTopProducts(startDate, endDate, bakery),
				getSalesByEmployee(startDate, endDate, bakery)
			]);
		} catch {
			error = true;
		} finally {
			loading = false;
		}
	}

	function formatCurrency(val: number | null) {
		if (val == null) return '—';
		return formatPriceCad(val);
	}

	function formatPercent(val: number | null) {
		if (val == null) return '—';
		return `${(Number(val) * 100).toFixed(1)}%`;
	}
</script>

<main class="flex-1 overflow-y-auto p-8 lg:p-10">
	<div class="mx-auto max-w-5xl space-y-8">
		<div>
			<h1 class="text-2xl font-bold tracking-tight text-foreground">Analytics</h1>
			<p class="mt-1 text-sm text-muted-foreground">Revenue and performance metrics</p>
		</div>

		<!-- Filters -->
		<div class="flex flex-wrap items-end gap-3">
			<div class="flex flex-col gap-1">
				<label
					for="fromDateInput"
					class="text-xs font-semibold tracking-widest text-muted-foreground uppercase"
				>
					From
				</label>
				<input
					id="fromDateInput"
					type="date"
					bind:value={startDate}
					class="rounded-md border border-border bg-background px-3 py-2 text-sm text-foreground"
				/>
			</div>
			<div class="flex flex-col gap-1">
				<label
					for="toDateInput"
					class="text-xs font-semibold tracking-widest text-muted-foreground uppercase"
				>
					To
				</label>
				<input
					id="toDateInput"
					type="date"
					bind:value={endDate}
					class="rounded-md border border-border bg-background px-3 py-2 text-sm text-foreground"
				/>
			</div>
			<div class="flex flex-col gap-1">
				<label
					for="bakerySelectInput"
					class="text-xs font-semibold tracking-widest text-muted-foreground uppercase"
				>
					Bakery
				</label>
				<select
					id="bakerySelectInput"
					bind:value={selectedBakery}
					class="rounded-md border border-border bg-background px-3 py-2 text-sm text-foreground"
				>
					<option value="">All Bakeries</option>
					{#each bakeryNames as name (name)}
						<option value={name}>{name}</option>
					{/each}
				</select>
			</div>
			<button
				onclick={loadData}
				class="rounded-md bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground hover:opacity-90"
			>
				Apply
			</button>
		</div>

		{#if loading}
			<div class="grid grid-cols-1 gap-4 sm:grid-cols-3">
				{#each Array(3) as _, i (i)}
					<Skeleton class="h-28 rounded-xl" />
				{/each}
			</div>
			<div class="grid grid-cols-1 gap-6 lg:grid-cols-2">
				{#each Array(4) as _, i (i)}
					<Skeleton class="h-64 rounded-xl" />
				{/each}
			</div>
		{:else if error}
			<p class="text-sm text-destructive">Failed to load analytics.</p>
		{:else}
			<!-- KPI row -->
			<div class="grid grid-cols-1 gap-4 sm:grid-cols-3">
				<KpiCard label="Total Revenue" value={formatCurrency(totalRevenue)} />
				<KpiCard label="Avg Order Value" value={formatCurrency(aov)} />
				<KpiCard label="Completion Rate" value={formatPercent(completionRate)} />
			</div>

			<!-- Charts -->
			<div class="grid grid-cols-1 gap-6 lg:grid-cols-2">
				<!-- Revenue over time -->
				<div class="rounded-xl border border-border bg-card p-5">
					<p class="mb-4 text-sm font-semibold text-foreground">Revenue Over Time</p>
					{#if revenueOverTime.length === 0}
						<p class="py-8 text-center text-xs text-muted-foreground">No data</p>
					{:else}
						<Chart.Container config={chartConfig} class="h-48 w-full">
							<LineChart
								data={revenueOverTime}
								x="label"
								y="value"
								axis="x"
								series={[{ key: 'value', label: 'Revenue', color: '#C25F1A' }]}
							/>
						</Chart.Container>
					{/if}
				</div>

				<!-- Revenue by bakery -->
				<div class="rounded-xl border border-border bg-card p-5">
					<p class="mb-4 text-sm font-semibold text-foreground">Revenue by Bakery</p>
					{#if revenueByBakery.length === 0}
						<p class="py-8 text-center text-xs text-muted-foreground">No data</p>
					{:else}
						<Chart.Container config={chartConfig} class="h-48 w-full">
							<BarChart
								data={revenueByBakery}
								xScale={scaleBand().padding(0.3)}
								x="label"
								axis="x"
								series={[{ key: 'value', label: 'Revenue', color: '#C25F1A' }]}
							/>
						</Chart.Container>
					{/if}
				</div>

				<!-- Top products -->
				<div class="rounded-xl border border-border bg-card p-5">
					<p class="mb-4 text-sm font-semibold text-foreground">Top Products</p>
					{#if topProducts.length === 0}
						<p class="py-8 text-center text-xs text-muted-foreground">No data</p>
					{:else}
						<Chart.Container config={chartConfig} class="h-48 w-full">
							<BarChart
								data={topProducts}
								xScale={scaleBand().padding(0.3)}
								x="label"
								axis="x"
								series={[{ key: 'value', label: 'Units Sold', color: '#8A9E7F' }]}
							/>
						</Chart.Container>
					{/if}
				</div>

				<!-- Sales by employee -->
				<div class="rounded-xl border border-border bg-card p-5">
					<p class="mb-4 text-sm font-semibold text-foreground">Sales by Employee</p>
					{#if salesByEmployee.length === 0}
						<p class="py-8 text-center text-xs text-muted-foreground">No data</p>
					{:else}
						<Chart.Container config={chartConfig} class="h-48 w-full">
							<BarChart
								data={salesByEmployee}
								xScale={scaleBand().padding(0.3)}
								x="label"
								axis="x"
								series={[{ key: 'value', label: 'Sales ($)', color: '#2C1A0E' }]}
							/>
						</Chart.Container>
					{/if}
				</div>
			</div>
		{/if}
	</div>
</main>
