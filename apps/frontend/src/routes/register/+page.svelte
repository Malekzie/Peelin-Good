<script>
	import { registerUser } from '$lib/services/auth.js';

	let fields = {
		firstName: '',
		middleInitial: '',
		lastName: '',
		email: '',
		username: '',
		password: '',
		phone: '',
		businessPhone: '',
		addressLine1: '',
		addressLine2: '',
		city: '',
		province: 'AB',
		postalCode: ''
	};

	let errors = {
		firstName: '',
		lastName: '',
		email: '',
		username: '',
		password: '',
		phone: '',
		addressLine1: '',
		city: '',
		province: '',
		postalCode: ''
	};

	let touched = {
		firstName: false,
		lastName: false,
		email: false,
		username: false,
		password: false,
		phone: false,
		addressLine1: false,
		city: false,
		province: false,
		postalCode: false
	};

	function validateField(name, value) {
		switch (name) {
			case 'firstName':
			case 'lastName':
				if (!value.trim()) return 'This field is required.';
				if (value.trim().length < 2) return 'Must be at least 2 characters.';
				return '';
			case 'email':
				if (!value.trim()) return 'Email is required.';
				if (!/^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(value)) return 'Enter a valid email address.';
				return '';
			case 'username':
				if (!value.trim()) return 'Username is required.';
				if (value.trim().length < 3) return 'Must be at least 3 characters.';
				if (!/^[a-zA-Z0-9_]+$/.test(value)) return 'Only letters, numbers, and underscores.';
				return '';
			case 'password':
				if (!value) return 'Password is required.';
				if (value.length < 8) return 'Must be at least 8 characters.';
				if (!/[A-Z]/.test(value)) return 'Must include an uppercase letter.';
				if (!/[0-9]/.test(value)) return 'Must include a number.';
				if (!/[^a-zA-Z0-9]/.test(value)) return 'Must include a special character.';
				return '';
			case 'phone':
				if (!value.trim()) return 'Phone number is required.';
				if (!/^\+?[\d\s\-().]{7,15}$/.test(value)) return 'Enter a valid phone number.';
				return '';
			case 'addressLine1':
				if (!value.trim()) return 'Address is required.';
				return '';
			case 'city':
				if (!value.trim()) return 'City is required.';
				return '';
			case 'province':
				if (!value) return 'Please select a province.';
				return '';
			case 'postalCode':
				if (!value.trim()) return 'Postal code is required.';
				if (!/^[A-Za-z]\d[A-Za-z][ -]?\d[A-Za-z]\d$/.test(value.trim()))
					return 'Enter a valid Canadian postal code (e.g. T2P 1J9).';
				return '';
			default:
				return '';
		}
	}

	function handleBlur(name) {
		touched[name] = true;
		errors[name] = validateField(name, fields[name]);
	}

	function handleInput(name) {
		if (touched[name]) {
			errors[name] = validateField(name, fields[name]);
		}
	}

	function formatPhone(value) {
		const digits = value.replace(/\D/g, '').substring(0, 10);
		const parts = [];
		if (digits.length > 0) parts.push('(' + digits.substring(0, 3));
		if (digits.length >= 4) parts.push(') ' + digits.substring(3, 6));
		if (digits.length >= 7) parts.push('-' + digits.substring(6, 10));
		return parts.join('');
	}

	async function handleRegister(event) {
		event.preventDefault();

		Object.keys(touched).forEach((name) => {
			touched[name] = true;
			errors[name] = validateField(name, fields[name]);
		});

		const hasErrors = Object.values(errors).some((e) => e !== '');
		if (hasErrors) return;

		const payload = {
			firstName: fields.firstName.trim(),
			middleInitial: fields.middleInitial.trim() || null,
			lastName: fields.lastName.trim(),
			email: fields.email.trim(),
			username: fields.username.trim(),
			password: fields.password,
			phone: fields.phone.replace(/\D/g, ''),
			businessPhone: fields.businessPhone.trim() || null,
			addressLine1: fields.addressLine1.trim(),
			addressLine2: fields.addressLine2.trim() || null,
			city: fields.city.trim(),
			province: fields.province.trim(),
			postalCode: fields.postalCode.trim().toUpperCase()
		};

		const { ok, message } = await registerUser(payload);

		if (!ok) {
			errors.email = message ?? 'Registration failed.';
			return;
		}

		window.location.href = '/profile';
	}
</script>

<div class="bg-surface flex min-h-screen flex-col p-6 md:p-10 lg:p-16">
	<div class="mx-auto my-auto w-full max-w-lg">
		<header class="mb-6 text-center">
			<h2 class="font-headline mb-3 text-4xl font-bold text-primary">Create Account</h2>
		</header>

		<div
			class="bg-surface-container border-outline/30 rounded-3xl border p-8 shadow-[0_8px_30px_rgba(0,0,0,0.08)]"
		>
			<div class="bg-outline/20 mb-6 h-px w-full"></div>

			<form class="space-y-6" onsubmit={handleRegister}>
				<!-- Name Row -->
				<div class="grid grid-cols-2 gap-4">
					<div class="space-y-1.5">
						<label class="text-on-surface-variant px-1 text-sm font-bold">First Name</label>
						<input
							type="text"
							placeholder="John"
							bind:value={fields.firstName}
							onblur={() => handleBlur('firstName')}
							oninput={() => handleInput('firstName')}
							class="bg-surface-container-highest mt-1 w-full rounded-xl px-6 py-3 font-medium ring-1 ring-border transition
								{errors.firstName && touched.firstName ? 'ring-2 ring-red-400' : ''}"
						/>
						{#if errors.firstName && touched.firstName}
							<p class="px-1 text-xs text-red-500">{errors.firstName}</p>
						{/if}
					</div>
					<div class="space-y-1.5">
						<label class="text-on-surface-variant px-1 text-sm font-bold">Last Name</label>
						<input
							type="text"
							placeholder="Smith"
							bind:value={fields.lastName}
							onblur={() => handleBlur('lastName')}
							oninput={() => handleInput('lastName')}
							class="bg-surface-container-highest mt-1 w-full rounded-xl px-6 py-3 font-medium ring-1 ring-border transition
								{errors.lastName && touched.lastName ? 'ring-2 ring-red-400' : ''}"
						/>
						{#if errors.lastName && touched.lastName}
							<p class="px-1 text-xs text-red-500">{errors.lastName}</p>
						{/if}
					</div>
				</div>

				<!-- Middle Initial (optional) -->
				<div class="space-y-1.5">
					<label class="text-on-surface-variant px-1 text-sm font-bold">
						Middle Initial <span class="text-outline font-normal">(optional)</span>
					</label>
					<input
						type="text"
						placeholder="J"
						maxlength="1"
						bind:value={fields.middleInitial}
						class="bg-surface-container-highest mt-1 w-full rounded-xl px-6 py-3 font-medium ring-1 ring-border transition"
					/>
				</div>

				<!-- Email -->
				<div class="space-y-1.5">
					<label class="text-on-surface-variant px-1 text-sm font-bold">Email Address</label>
					<input
						type="email"
						placeholder="email@example.com"
						bind:value={fields.email}
						onblur={() => handleBlur('email')}
						oninput={() => handleInput('email')}
						class="bg-surface-container-highest mt-1 w-full rounded-xl px-6 py-3 font-medium ring-1 ring-border transition
							{errors.email && touched.email ? 'ring-2 ring-red-400' : ''}"
					/>
					{#if errors.email && touched.email}
						<p class="px-1 text-xs text-red-500">{errors.email}</p>
					{/if}
				</div>

				<!-- Username -->
				<div class="space-y-1.5">
					<label class="text-on-surface-variant px-1 text-sm font-bold">Username</label>
					<input
						type="text"
						placeholder="username"
						bind:value={fields.username}
						onblur={() => handleBlur('username')}
						oninput={() => handleInput('username')}
						class="bg-surface-container-highest mt-1 w-full rounded-xl px-6 py-3 font-medium ring-1 ring-border transition
							{errors.username && touched.username ? 'ring-2 ring-red-400' : ''}"
					/>
					{#if errors.username && touched.username}
						<p class="px-1 text-xs text-red-500">{errors.username}</p>
					{/if}
				</div>

				<!-- Password -->
				<div class="space-y-1.5">
					<label class="text-on-surface-variant px-1 text-sm font-bold">Password</label>
					<input
						type="password"
						placeholder="Password"
						bind:value={fields.password}
						onblur={() => handleBlur('password')}
						oninput={() => handleInput('password')}
						class="bg-surface-container-highest mt-1 w-full rounded-xl px-6 py-3 font-medium ring-1 ring-border transition
							{errors.password && touched.password ? 'ring-2 ring-red-400' : ''}"
					/>
					{#if errors.password && touched.password}
						<p class="px-1 text-xs text-red-500">{errors.password}</p>
					{/if}
				</div>

				<!-- Phone -->
				<div class="space-y-1.5">
					<label class="text-on-surface-variant px-1 text-sm font-bold">Phone Number</label>
					<input
						type="tel"
						placeholder="(403) 555-0100"
						bind:value={fields.phone}
						oninput={(e) => {
							fields.phone = formatPhone(e.target.value);
							handleInput('phone');
						}}
						onblur={() => handleBlur('phone')}
						class="bg-surface-container-highest mt-1 w-full rounded-xl px-6 py-3 font-medium ring-1 ring-border transition
							{errors.phone && touched.phone ? 'ring-2 ring-red-400' : ''}"
					/>
					{#if errors.phone && touched.phone}
						<p class="px-1 text-xs text-red-500">{errors.phone}</p>
					{/if}
				</div>

				<!-- Business Phone (optional) -->
				<div class="space-y-1.5">
					<label class="text-on-surface-variant px-1 text-sm font-bold">
						Business Phone <span class="text-outline font-normal">(optional)</span>
					</label>
					<input
						type="tel"
						placeholder="(403) 555-0100"
						bind:value={fields.businessPhone}
						oninput={(e) => {
							fields.businessPhone = formatPhone(e.target.value);
						}}
						class="bg-surface-container-highest mt-1 w-full rounded-xl px-6 py-3 font-medium ring-1 ring-border transition"
					/>
				</div>

				<!-- Address Line 1 -->
				<div class="space-y-1.5">
					<label class="text-on-surface-variant px-1 text-sm font-bold">Address</label>
					<input
						type="text"
						placeholder="123 Main St"
						bind:value={fields.addressLine1}
						onblur={() => handleBlur('addressLine1')}
						oninput={() => handleInput('addressLine1')}
						class="bg-surface-container-highest mt-1 w-full rounded-xl px-6 py-3 font-medium ring-1 ring-border transition
							{errors.addressLine1 && touched.addressLine1 ? 'ring-2 ring-red-400' : ''}"
					/>
					{#if errors.addressLine1 && touched.addressLine1}
						<p class="px-1 text-xs text-red-500">{errors.addressLine1}</p>
					{/if}
				</div>

				<!-- Address Line 2 (optional) -->
				<div class="space-y-1.5">
					<label class="text-on-surface-variant px-1 text-sm font-bold">
						Address Line 2 <span class="text-outline font-normal">(optional)</span>
					</label>
					<input
						type="text"
						placeholder="Apt, suite, unit..."
						bind:value={fields.addressLine2}
						class="bg-surface-container-highest mt-1 w-full rounded-xl px-6 py-3 font-medium ring-1 ring-border transition"
					/>
				</div>

				<!-- City / Province / Postal -->
				<div class="grid grid-cols-3 gap-4">
					<div class="space-y-1.5">
						<label class="text-on-surface-variant px-1 text-sm font-bold">City</label>
						<input
							type="text"
							placeholder="Calgary"
							bind:value={fields.city}
							onblur={() => handleBlur('city')}
							oninput={() => handleInput('city')}
							class="bg-surface-container-highest mt-1 w-full rounded-xl px-6 py-3 font-medium ring-1 ring-border transition
								{errors.city && touched.city ? 'ring-2 ring-red-400' : ''}"
						/>
						{#if errors.city && touched.city}
							<p class="px-1 text-xs text-red-500">{errors.city}</p>
						{/if}
					</div>
					<div class="space-y-1.5">
						<label class="text-on-surface-variant px-1 text-sm font-bold">Province</label>
						<select
							bind:value={fields.province}
							onblur={() => handleBlur('province')}
							onchange={() => handleInput('province')}
							class="bg-surface-container-highest mt-1 w-full rounded-xl px-6 py-3 font-medium ring-1 ring-border transition
            {errors.province && touched.province ? 'ring-2 ring-red-400' : ''}"
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
						{#if errors.province && touched.province}
							<p class="px-1 text-xs text-red-500">{errors.province}</p>
						{/if}
					</div>
					<div class="space-y-1.5">
						<label class="text-on-surface-variant px-1 text-sm font-bold">Postal Code</label>
						<input
							type="text"
							placeholder="T2P 1J9"
							maxlength="7"
							bind:value={fields.postalCode}
							onblur={() => handleBlur('postalCode')}
							oninput={() => handleInput('postalCode')}
							class="bg-surface-container-highest mt-1 w-full rounded-xl px-6 py-3 font-medium ring-1 ring-border transition
								{errors.postalCode && touched.postalCode ? 'ring-2 ring-red-400' : ''}"
						/>
						{#if errors.postalCode && touched.postalCode}
							<p class="px-1 text-xs text-red-500">{errors.postalCode}</p>
						{/if}
					</div>
				</div>

				<!-- Submit -->
				<button
					type="submit"
					class="text-on-primary mt-4 w-full rounded-full bg-primary py-3.5 text-base font-bold transition hover:cursor-pointer hover:opacity-90"
				>
					Create Account
				</button>

				<p class="text-center text-sm">
					Already have an account?
					<a href="/login" class="font-semibold text-primary hover:underline">Sign in</a>
				</p>
			</form>
		</div>
	</div>
</div>
