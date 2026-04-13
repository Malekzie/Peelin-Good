<script lang="ts">
	import { Skeleton } from '$lib/components/ui/skeleton';
	import * as Chart from '$lib/components/ui/chart/index.js';
	import { BarChart, LineChart } from 'layerchart';
	import { scaleBand } from 'd3-scale';
	import KpiCard from '$lib/components/staff/KpiCard.svelte';
	import {
		getTotalRevenue,
		getAverageOrderValue,
		getCompletionRate,
		getRevenueOverTime,
		getRevenueByBakery,
		getTopProducts,
		getSalesByEmployee
	} from '$lib/services/analytics';
	import { formatPriceCad } from '$lib/utils/money';

	type AnalyticsPoint = { label: string; value: number };
	type AnalyticsData = {
		bakeryNames: Promise<string[]>;
		initialData: { startDate: string; endDate: string };
		totalRevenue: Promise<number>;
		aov: Promise<number>;
		completionRate: Promise<number>;
		revenueOverTime: Promise<AnalyticsPoint[]>;
		revenueByBakery: Promise<AnalyticsPoint[]>;
		topProducts: Promise<AnalyticsPoint[]>;
		salesByEmployee: Promise<AnalyticsPoint[]>;
	};

	interface Props {
		data: AnalyticsData;
	}

	let { data }: Props = $props();
	const initialStartDate = () => data.initialData.startDate;
	const initialEndDate = () => data.initialData.endDate;
	const initialData = () => data;

	let startDate = $state(initialStartDate());
	let endDate = $state(initialEndDate());
	let selectedBakery = $state('');
	let filteredData = $state<AnalyticsData>(initialData());

	const chartConfig = {
		value: { label: 'Value', color: '#C25F1A' }
	} satisfies Chart.ChartConfig;

	function loadData() {
		const bakery = selectedBakery || undefined;
		filteredData = {
			bakeryNames: data.bakeryNames,
			initialData: data.initialData,
			totalRevenue: getTotalRevenue(startDate, endDate, bakery),
			aov: getAverageOrderValue(startDate, endDate, bakery),
			completionRate: getCompletionRate(startDate, endDate, bakery),
			revenueOverTime: getRevenueOverTime(startDate, endDate, bakery),
			revenueByBakery: getRevenueByBakery(startDate, endDate),
			topProducts: getTopProducts(startDate, endDate, bakery),
			salesByEmployee: getSalesByEmployee(startDate, endDate, bakery)
		};
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
				{#await filteredData.bakeryNames}
					<Skeleton class="h-10 w-32 rounded-md" />
				{:then bakeryNames}
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
				{:catch}
					<p class="text-xs text-destructive">Failed to load bakeries</p>
				{/await}
			</div>
			<button
				onclick={loadData}
				class="rounded-md bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground hover:opacity-90"
			>
				Apply
			</button>
		</div>

		<div class="grid grid-cols-1 gap-4 sm:grid-cols-3">
			{#await Promise.all( [filteredData.totalRevenue, filteredData.aov, filteredData.completionRate] )}
				{#each [0, 1, 2] as i (i)}
					<Skeleton class="h-28 rounded-xl" />
				{/each}
			{:then [totalRevenue, aov, completionRate]}
				<KpiCard label="Total Revenue" value={formatCurrency(totalRevenue)} />
				<KpiCard label="Avg Order Value" value={formatCurrency(aov)} />
				<KpiCard label="Completion Rate" value={formatPercent(completionRate)} />
			{:catch}
				<p class="text-sm text-destructive">Failed to load KPI metrics.</p>
			{/await}
		</div>

		<div class="grid grid-cols-1 gap-6 lg:grid-cols-2">
			<div class="rounded-xl border border-border bg-card p-5">
				<p class="mb-4 text-sm font-semibold text-foreground">Revenue Over Time</p>
				{#await filteredData.revenueOverTime}
					<Skeleton class="h-48 rounded-xl" />
				{:then revenueOverTime}
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
				{:catch}
					<p class="py-4 text-center text-xs text-destructive">Failed to load chart</p>
				{/await}
			</div>

			<div class="rounded-xl border border-border bg-card p-5">
				<p class="mb-4 text-sm font-semibold text-foreground">Revenue by Bakery</p>
				{#await filteredData.revenueByBakery}
					<Skeleton class="h-48 rounded-xl" />
				{:then revenueByBakery}
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
				{:catch}
					<p class="py-4 text-center text-xs text-destructive">Failed to load chart</p>
				{/await}
			</div>

			<div class="rounded-xl border border-border bg-card p-5">
				<p class="mb-4 text-sm font-semibold text-foreground">Top Products</p>
				{#await filteredData.topProducts}
					<Skeleton class="h-48 rounded-xl" />
				{:then topProducts}
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
				{:catch}
					<p class="py-4 text-center text-xs text-destructive">Failed to load chart</p>
				{/await}
			</div>

			<div class="rounded-xl border border-border bg-card p-5">
				<p class="mb-4 text-sm font-semibold text-foreground">Sales by Employee</p>
				{#await filteredData.salesByEmployee}
					<Skeleton class="h-48 rounded-xl" />
				{:then salesByEmployee}
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
				{:catch}
					<p class="py-4 text-center text-xs text-destructive">Failed to load chart</p>
				{/await}
			</div>
		</div>
	</div>
</main>
