<script lang="ts">
	import { formatCanadianPostalInput } from '$lib/utils/canadianPostalCode';
	import { validateField, type ErrorKey } from '$lib/services/checkout';

	interface Props {
		line1: string;
		line2: string;
		city: string;
		province: string;
		postal: string;
		errors: Record<ErrorKey, string>;
		touched: Record<ErrorKey, boolean>;
		onBlur: (name: ErrorKey) => void;
		onInput: (name: ErrorKey) => void;
		onPostalInput: (value: string) => void;
	}

	let {
		line1 = $bindable(),
		line2 = $bindable(),
		city = $bindable(),
		province = $bindable(),
		postal = $bindable(),
		errors,
		touched,
		onBlur,
		onInput,
		onPostalInput
	}: Props = $props();

	function handlePostalInput(e: Event) {
		const target = e.target as HTMLInputElement;
		const formatted = formatCanadianPostalInput(target.value);
		postal = formatted;
		onPostalInput(formatted);
		if (touched.deliveryPostal) {
			onInput('deliveryPostal');
		}
	}
</script>

<div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
	<div class="flex flex-col gap-1 sm:col-span-2">
		<label for="dLine1" class="text-sm font-medium text-foreground">
			Address Line 1 <span class="text-destructive">*</span>
		</label>
		<input
			id="dLine1"
			type="text"
			bind:value={line1}
			onblur={() => onBlur('deliveryLine1')}
			oninput={() => onInput('deliveryLine1')}
			class="rounded-lg border border-input bg-background px-3 py-2 text-sm text-foreground focus:ring-2 focus:ring-ring focus:outline-none {errors.deliveryLine1 &&
			touched.deliveryLine1
				? 'ring-2 ring-red-400'
				: ''}"
		/>
		{#if errors.deliveryLine1 && touched.deliveryLine1}
			<p class="px-1 text-xs text-red-500">{errors.deliveryLine1}</p>
		{/if}
	</div>
	<div class="flex flex-col gap-1 sm:col-span-2">
		<label for="dLine2" class="text-sm font-medium text-foreground">Address Line 2</label>
		<input
			id="dLine2"
			type="text"
			bind:value={line2}
			class="rounded-lg border border-input bg-background px-3 py-2 text-sm text-foreground focus:ring-2 focus:ring-ring focus:outline-none"
		/>
	</div>
	<div class="flex flex-col gap-1">
		<label for="dCity" class="text-sm font-medium text-foreground">
			City <span class="text-destructive">*</span>
		</label>
		<input
			id="dCity"
			type="text"
			bind:value={city}
			maxlength={20}
			onblur={() => onBlur('deliveryCity')}
			oninput={() => onInput('deliveryCity')}
			class="rounded-lg border border-input bg-background px-3 py-2 text-sm text-foreground focus:ring-2 focus:ring-ring focus:outline-none {errors.deliveryCity &&
			touched.deliveryCity
				? 'ring-2 ring-red-400'
				: ''}"
		/>
		{#if errors.deliveryCity && touched.deliveryCity}
			<p class="px-1 text-xs text-red-500">{errors.deliveryCity}</p>
		{/if}
	</div>
	<div class="flex flex-col gap-1">
		<label for="dProvince" class="text-sm font-medium text-foreground">
			Province <span class="text-destructive">*</span>
		</label>
		<select
			id="dProvince"
			bind:value={province}
			onblur={() => onBlur('deliveryProvince')}
			class="rounded-lg border border-input bg-background px-3 py-2 text-sm text-foreground focus:ring-2 focus:ring-ring focus:outline-none {errors.deliveryProvince &&
			touched.deliveryProvince
				? 'ring-2 ring-red-400'
				: ''}"
		>
			<option value="AB">Alberta</option>
			<option value="BC">British Columbia</option>
			<option value="MB">Manitoba</option>
			<option value="NB">New Brunswick</option>
			<option value="NL">Newfoundland and Labrador</option>
			<option value="NS">Nova Scotia</option>
			<option value="NT">Northwest Territories</option>
			<option value="NU">Nunavut</option>
			<option value="ON">Ontario</option>
			<option value="PE">Prince Edward Island</option>
			<option value="QC">Quebec</option>
			<option value="SK">Saskatchewan</option>
			<option value="YT">Yukon</option>
		</select>
		{#if errors.deliveryProvince && touched.deliveryProvince}
			<p class="px-1 text-xs text-red-500">{errors.deliveryProvince}</p>
		{/if}
	</div>
	<div class="flex flex-col gap-1">
		<label for="dPostal" class="text-sm font-medium text-foreground">
			Postal Code <span class="text-destructive">*</span>
		</label>
		<input
			id="dPostal"
			type="text"
			value={postal}
			maxlength={7}
			inputmode="text"
			autocomplete="postal-code"
			placeholder="T2X 1Y4"
			onblur={() => onBlur('deliveryPostal')}
			oninput={handlePostalInput}
			class="rounded-lg border border-input bg-background px-3 py-2 text-sm text-foreground focus:ring-2 focus:ring-ring focus:outline-none {errors.deliveryPostal &&
			touched.deliveryPostal
				? 'ring-2 ring-red-400'
				: ''}"
		/>
		{#if errors.deliveryPostal && touched.deliveryPostal}
			<p class="px-1 text-xs text-red-500">{errors.deliveryPostal}</p>
		{/if}
	</div>
</div>
