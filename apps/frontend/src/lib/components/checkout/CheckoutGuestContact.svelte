<script lang="ts">
	import { resolve } from '$app/paths';
	import { validateField, formatPhone, type ErrorKey } from '$lib/services/checkout';
	import { FormValidationUtil } from '$lib/utils/formValidation';

	interface Props {
		firstName: string;
		lastName: string;
		email: string;
		phone: string;
		errors: Record<ErrorKey, string>;
		touched: Record<ErrorKey, boolean>;
		onBlur: (name: ErrorKey) => void;
		onInput: (name: ErrorKey) => void;
		onPhoneInput: (value: string) => void;
	}

	let {
		firstName = $bindable(),
		lastName = $bindable(),
		email = $bindable(),
		phone = $bindable(),
		errors,
		touched,
		onBlur,
		onInput,
		onPhoneInput
	}: Props = $props();

	function handlePhoneInput(e: Event) {
		const target = e.target as HTMLInputElement;
		const formatted = FormValidationUtil.formatPhone(target.value);
		target.value = formatted;
		phone = formatted;
		onPhoneInput(formatted);
		if (touched.guestPhone) {
			onInput('guestPhone');
		}
	}
</script>

<section class="rounded-xl border border-border bg-card p-6 shadow-sm">
	<h2 class="mb-1 text-lg font-semibold text-foreground">Contact</h2>
	<p class="mb-4 text-sm text-muted-foreground">
		Already have an account?
		<a href={resolve('/login')} class="text-primary hover:underline">Sign in</a>
	</p>
	<div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
		<div class="flex flex-col gap-1">
			<label for="guestFirst" class="text-sm font-medium text-foreground"
				>First Name <span class="text-red-500">*</span></label
			>
			<input
				id="guestFirst"
				type="text"
				bind:value={firstName}
				onblur={() => onBlur('guestFirstName')}
				oninput={() => onInput('guestFirstName')}
				class="rounded-lg border border-input bg-background px-3 py-2 text-sm text-foreground focus:ring-2 focus:ring-ring focus:outline-none {errors.guestFirstName &&
				touched.guestFirstName
					? 'ring-2 ring-red-400'
					: ''}"
			/>
			{#if errors.guestFirstName && touched.guestFirstName}
				<p class="px-1 text-xs text-red-500">{errors.guestFirstName}</p>
			{/if}
		</div>
		<div class="flex flex-col gap-1">
			<label for="guestLast" class="text-sm font-medium text-foreground"
				>Last Name <span class="text-red-500">*</span></label
			>
			<input
				id="guestLast"
				type="text"
				bind:value={lastName}
				onblur={() => onBlur('guestLastName')}
				oninput={() => onInput('guestLastName')}
				class="rounded-lg border border-input bg-background px-3 py-2 text-sm text-foreground focus:ring-2 focus:ring-ring focus:outline-none {errors.guestLastName &&
				touched.guestLastName
					? 'ring-2 ring-red-400'
					: ''}"
			/>
			{#if errors.guestLastName && touched.guestLastName}
				<p class="px-1 text-xs text-red-500">{errors.guestLastName}</p>
			{/if}
		</div>
		<div class="flex flex-col gap-1">
			<label for="guestEmail" class="text-sm font-medium text-foreground">
				Email <span class="text-red-500">*</span>
			</label>
			<input
				id="guestEmail"
				type="email"
				bind:value={email}
				onblur={() => onBlur('guestEmail')}
				oninput={() => onInput('guestEmail')}
				class="rounded-lg border border-input bg-background px-3 py-2 text-sm text-foreground focus:ring-2 focus:ring-ring focus:outline-none {errors.guestEmail &&
				touched.guestEmail
					? 'ring-2 ring-red-400'
					: ''}"
			/>
			{#if errors.guestEmail && touched.guestEmail}
				<p class="px-1 text-xs text-red-500">{errors.guestEmail}</p>
			{/if}
		</div>
		<div class="flex flex-col gap-1">
			<label for="guestPhone" class="text-sm font-medium text-foreground"> Phone </label>
			<input
				id="guestPhone"
				type="tel"
				value={phone}
				onblur={() => onBlur('guestPhone')}
				oninput={handlePhoneInput}
				class="rounded-lg border border-input bg-background px-3 py-2 text-sm text-foreground focus:ring-2 focus:ring-ring focus:outline-none {errors.guestPhone &&
				touched.guestPhone
					? 'ring-2 ring-red-400'
					: ''}"
			/>
			{#if errors.guestPhone && touched.guestPhone}
				<p class="px-1 text-xs text-red-500">{errors.guestPhone}</p>
			{/if}
		</div>
	</div>
</section>
