<script lang="ts">
	import {
		formatTimeHM,
		getAsapEstimateLabel,
		minScheduleDate,
		maxScheduleDate,
		isOpenNow,
		nextOpenStr,
		scheduledDayClosedNotice,
		type BakeryHour
	} from '$lib/services/checkout';

	interface Props {
		scheduleEnabled: boolean;
		scheduleDate: string;
		scheduleTime: string;
		bakeryHours: BakeryHour[];
		bakeryHoursLoading: boolean;
		availableTimeSlots: string[];
		onScheduleToggle: () => void;
		onDateChange: (date: string) => void;
		onTimeChange: (time: string) => void;
	}

	let {
		scheduleEnabled = $bindable(),
		scheduleDate = $bindable(),
		scheduleTime = $bindable(),
		bakeryHours,
		bakeryHoursLoading,
		availableTimeSlots,
		onScheduleToggle,
		onDateChange,
		onTimeChange
	}: Props = $props();

	function handleScheduleToggle() {
		scheduleEnabled = !scheduleEnabled;
		onScheduleToggle();
	}

	function handleDateChange(e: Event) {
		const target = e.target as HTMLInputElement;
		scheduleDate = target.value;
		onDateChange(target.value);
	}

	function handleTimeChange(e: Event) {
		const target = e.target as HTMLSelectElement;
		scheduleTime = target.value;
		onTimeChange(target.value);
	}

	const bakeryIsOpenNow = $derived(isOpenNow(bakeryHours));
	const nextOpenTime = $derived(nextOpenStr(bakeryHours));
	const closedNotice = $derived(
		scheduledDayClosedNotice(scheduleDate, bakeryHours, availableTimeSlots)
	);
</script>

<section class="rounded-xl border border-border bg-card p-6 shadow-sm">
	<div class="flex items-center justify-between">
		<h2 class="text-lg font-semibold text-foreground">Schedule</h2>
		<label class="flex cursor-pointer items-center gap-2 text-sm text-muted-foreground">
			<input
				type="checkbox"
				checked={scheduleEnabled}
				onchange={handleScheduleToggle}
				class="accent-primary"
			/>
			Schedule for later
		</label>
	</div>

	{#if !scheduleEnabled}
		<!-- ASAP mode -->
		{#if bakeryHours.length > 0}
			{#if bakeryIsOpenNow}
				<p class="mt-2 text-sm text-muted-foreground">
					Order will be prepared as soon as possible. Est. ready by
					<span class="font-medium text-foreground">{getAsapEstimateLabel()}</span>.
				</p>
			{:else if nextOpenTime}
				<div
					class="mt-3 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2.5 text-sm text-amber-800"
				>
					This location is currently closed. It will open <span class="font-medium"
						>{nextOpenTime}</span
					>. Your order will be received but won't be started until then.
				</div>
			{:else}
				<p class="mt-2 text-sm text-muted-foreground">
					This location appears to be closed for the near future.
				</p>
			{/if}
		{:else}
			<p class="mt-2 text-sm text-muted-foreground">Order will be prepared as soon as possible.</p>
		{/if}
	{:else}
		<!-- Scheduled mode -->
		<div class="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2">
			<div class="flex flex-col gap-1">
				<label for="schedDate" class="text-sm font-medium text-foreground">Date</label>
				<input
					id="schedDate"
					type="date"
					value={scheduleDate}
					onchange={handleDateChange}
					min={minScheduleDate()}
					max={maxScheduleDate()}
					class="rounded-lg border border-input bg-background px-3 py-2 text-sm text-foreground focus:ring-2 focus:ring-ring focus:outline-none"
				/>
			</div>
			<div class="flex flex-col gap-1">
				<label for="schedTime" class="text-sm font-medium text-foreground">Time</label>
				{#if bakeryHoursLoading}
					<p
						class="rounded-lg border border-border bg-muted/50 px-3 py-2 text-sm text-muted-foreground"
					>
						Loading...
					</p>
				{:else if availableTimeSlots.length > 0}
					<select
						id="schedTime"
						value={scheduleTime}
						onchange={handleTimeChange}
						class="rounded-lg border border-input bg-background px-3 py-2 text-sm text-foreground focus:ring-2 focus:ring-ring focus:outline-none"
					>
						{#each availableTimeSlots as slot (slot)}
							<option value={slot}>{slot}</option>
						{/each}
					</select>
				{:else if scheduleDate}
					<p
						class="rounded-lg border border-border bg-muted/50 px-3 py-2 text-sm text-muted-foreground"
					>
						No available slots
					</p>
				{:else}
					<p
						class="rounded-lg border border-border bg-muted/50 px-3 py-2 text-sm text-muted-foreground"
					>
						Select a date first
					</p>
				{/if}
			</div>
		</div>

		{#if closedNotice}
			<div
				class="mt-3 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2.5 text-sm text-amber-800"
			>
				{closedNotice}
			</div>
		{/if}
	{/if}
</section>
