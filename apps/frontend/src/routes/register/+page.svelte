<script>
	import { fetchRegisterAvailability, registerUser } from '$lib/services/auth';
	import { updateProfile } from '$lib/services/profile';
	import { resolve } from '$app/paths';
	import { goto } from '$app/navigation';
	import { user } from '$lib/stores/authStore';
	import { Eye, EyeOff } from '@lucide/svelte';
	import { formatCanadianPostalInput } from '$lib/utils/canadianPostalCode';
	import { FormValidationUtil } from '$lib/utils/formValidation';

	let fields = {
		firstName: '',
		middleInitial: '',
		lastName: '',
		email: '',
		username: '',
		password: '',
		employeeLinkPassword: '',
		employeeLinkPasswordConfirm: '',
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
		employeeLinkPassword: '',
		employeeLinkPasswordConfirm: '',
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
		employeeLinkPassword: false,
		employeeLinkPasswordConfirm: false,
		phone: false,
		addressLine1: false,
		city: false,
		province: false,
		postalCode: false
	};

	let step = 1;
	let showPassword = false;
	let showEmployeeLinkPassword = false;
	let showEmployeeLinkPasswordConfirm = false;
	/** True while calling register/availability before advancing to step 2 */
	let step1Checking = false;
	/** From availability API: email matches one unlinked employee work email */
	let employeeLinkOffered = false;

	const stepOneFields = ['firstName', 'lastName', 'email', 'username', 'password'];
	const stepTwoProfileFields = ['phone', 'addressLine1', 'city', 'province', 'postalCode'];
	const employeeLinkStepFields = ['employeeLinkPassword', 'employeeLinkPasswordConfirm'];
	let submitError = '';
	let registrationSuccess = false;

	function validateField(name, value) {
		switch (name) {
			case 'firstName':
			case 'lastName':
				if (!value.trim()) return 'This field is required.';
				if (value.trim().length < 2) return 'Must be at least 2 characters.';
				return '';
			case 'email':
				if (!value.trim()) return 'Email is required.';
				if (!FormValidationUtil.isValidEmail(value)) return 'Enter a valid email address.';
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
			case 'employeeLinkPassword':
				if (!value.trim()) return 'Employee account password is required.';
				return '';
			case 'employeeLinkPasswordConfirm':
				if (!value.trim()) return 'Confirm your employee password.';
				if (value !== fields.employeeLinkPassword)
					return 'The two employee password fields do not match.';
				return '';
			case 'phone':
				if (!value.trim()) return 'Phone number is required.';
				if (!FormValidationUtil.isValidPhone(value)) return 'Enter a valid phone number.';
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
				if (!FormValidationUtil.isValidCanadianPostalCode(value))
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

	function validateStep(fieldNames) {
		fieldNames.forEach((name) => {
			touched[name] = true;
			errors[name] = validateField(name, fields[name]);
		});

		return !fieldNames.some((name) => errors[name]);
	}

	async function nextStep() {
		if (!validateStep(stepOneFields)) return;
		step1Checking = true;
		submitError = '';
		try {
			const result = await fetchRegisterAvailability(fields.username, fields.email);
			if (!result.ok) {
				submitError = result.message;
				return;
			}
			if (!result.usernameAvailable) {
				errors.username = 'That username is already taken.';
				touched.username = true;
				return;
			}
			if (!result.emailAvailable) {
				errors.email = 'That email is already registered.';
				touched.email = true;
				return;
			}
			employeeLinkOffered = result.employeeLinkOffered;
			fields.employeeLinkPassword = '';
			fields.employeeLinkPasswordConfirm = '';
			errors.employeeLinkPassword = '';
			errors.employeeLinkPasswordConfirm = '';
			touched.employeeLinkPassword = false;
			touched.employeeLinkPasswordConfirm = false;
			step = 2;
		} finally {
			step1Checking = false;
		}
	}

	function prevStep() {
		step = 1;
		submitError = '';
	}

	function getDefaultPostAuthRoute(role) {
		const normalizedRole = (role ?? '').toLowerCase();
		return normalizedRole === 'admin' ||
			normalizedRole === 'employee' ||
			normalizedRole.endsWith('_admin') ||
			normalizedRole.endsWith('_employee')
			? '/staff/dashboard'
			: '/profile';
	}

	async function handleRegister(event) {
		event.preventDefault();
		submitError = '';

		const fieldsToValidate = employeeLinkOffered
			? [...stepOneFields, ...employeeLinkStepFields]
			: [...stepOneFields, ...stepTwoProfileFields];
		if (!validateStep(fieldsToValidate)) return;

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

		const registrationPayload =
			employeeLinkOffered ?
				{
					username: payload.username,
					email: payload.email,
					password: payload.password,
					phone: null,
					employeeLinkPassword: fields.employeeLinkPassword
				}
			:	{
					username: payload.username,
					email: payload.email,
					password: payload.password,
					phone: payload.phone || null
				};

		const registerResult = await registerUser(registrationPayload);

		if (!registerResult.ok) {
			const { message, status } = registerResult;
			if (employeeLinkOffered && status === 401) {
				errors.employeeLinkPassword =
					message?.trim() ||
					'That password does not match the staff account for this email.';
				touched.employeeLinkPassword = true;
				submitError = '';
				return;
			}
			if (
				employeeLinkOffered &&
				message &&
				message.includes('Employee password')
			) {
				errors.employeeLinkPassword = message;
				touched.employeeLinkPassword = true;
				submitError = '';
				return;
			}

			const msg =
				message ?? 'Registration failed. Please check your details and try again.';
			submitError = msg;
			const lower = msg.toLowerCase();
			if (lower.includes('username')) {
				errors.username = msg;
				touched.username = true;
				step = 1;
			} else if (lower.includes('email')) {
				errors.email = msg;
				touched.email = true;
				step = 1;
			} else if (status === 409) {
				step = 1;
			} else {
				errors.email = msg;
				touched.email = true;
			}
			return;
		}

		const { employeeDiscountLinkEstablished, employeeDiscountLinkMessage } = registerResult;

		if (employeeDiscountLinkEstablished) {
			const msg =
				employeeDiscountLinkMessage?.trim() ||
				'Your account is linked to your employee profile. You get 20% off eligible orders at checkout (after today’s specials and your loyalty tier).';
			alert(msg);
		}

		const skipProfilePatch =
			employeeDiscountLinkEstablished === true || employeeLinkOffered === true;

		if (!skipProfilePatch) {
			try {
				await updateProfile({
					firstName: payload.firstName,
					middleInitial: payload.middleInitial,
					lastName: payload.lastName,
					phone: payload.phone,
					businessPhone: payload.businessPhone,
					email: payload.email,
					username: payload.username,
					address: {
						line1: payload.addressLine1,
						line2: payload.addressLine2,
						city: payload.city,
						province: payload.province,
						postalCode: payload.postalCode
					}
				});
			} catch (error) {
				submitError =
					'Account created, but profile setup failed. Please sign in and complete your profile.';
				return;
			}
		}

		registrationSuccess = true;
		await new Promise((r) => setTimeout(r, 2000));
		goto(resolve(getDefaultPostAuthRoute($user?.role)));
	}
</script>

<div class="bg-surface flex min-h-screen flex-col p-6 md:p-10 lg:p-16">
	<div class="mx-auto my-auto w-full max-w-2xl">
		<header class="mb-6 text-center">
			<h2 class="font-headline mb-3 text-4xl font-bold text-primary">Create Account</h2>
			<p class="text-sm text-muted-foreground">
				Step {step} of 2
				{#if step === 1}
					· Account details
				{:else if employeeLinkOffered}
					· Link staff account
				{:else}
					· Contact and address
				{/if}
			</p>
		</header>

		<div
			class="bg-surface-container border-outline/30 rounded-3xl border p-8 shadow-[0_8px_30px_rgba(0,0,0,0.08)]"
		>
			<div class="mb-6 flex items-center gap-2">
				<div class="h-2 flex-1 rounded-full {step >= 1 ? 'bg-primary' : 'bg-muted'}"></div>
				<div class="h-2 flex-1 rounded-full {step >= 2 ? 'bg-primary' : 'bg-muted'}"></div>
			</div>

			<form class="space-y-6" onsubmit={handleRegister}>
				{#if submitError}
					<p
						class="rounded-lg border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive"
					>
						{submitError}
					</p>
				{/if}

				{#if step === 1}
					<div class="grid gap-5 md:grid-cols-2">
						<div class="space-y-1.5">
							<label for="register-firstName" class="text-on-surface-variant px-1 text-sm font-bold"
								>First Name</label
							>
							<input
								id="register-firstName"
								type="text"
								placeholder="John"
								bind:value={fields.firstName}
								onblur={() => handleBlur('firstName')}
								oninput={() => handleInput('firstName')}
								class="bg-surface-container-highest mt-1 w-full rounded-xl px-4 py-3 font-medium ring-1 ring-border transition
									{errors.firstName && touched.firstName ? 'ring-2 ring-red-400' : ''}"
							/>
							{#if errors.firstName && touched.firstName}
								<p class="px-1 text-xs text-red-500">{errors.firstName}</p>
							{/if}
						</div>
						<div class="space-y-1.5">
							<label for="register-lastName" class="text-on-surface-variant px-1 text-sm font-bold"
								>Last Name</label
							>
							<input
								id="register-lastName"
								type="text"
								placeholder="Smith"
								bind:value={fields.lastName}
								onblur={() => handleBlur('lastName')}
								oninput={() => handleInput('lastName')}
								class="bg-surface-container-highest mt-1 w-full rounded-xl px-4 py-3 font-medium ring-1 ring-border transition
									{errors.lastName && touched.lastName ? 'ring-2 ring-red-400' : ''}"
							/>
							{#if errors.lastName && touched.lastName}
								<p class="px-1 text-xs text-red-500">{errors.lastName}</p>
							{/if}
						</div>

						<div class="space-y-1.5 md:col-span-2">
							<label
								for="register-middleInitial"
								class="text-on-surface-variant px-1 text-sm font-bold"
							>
								Middle Initial <span class="text-outline font-normal">(optional)</span>
							</label>
							<input
								id="register-middleInitial"
								type="text"
								placeholder="J"
								maxlength="1"
								bind:value={fields.middleInitial}
								class="bg-surface-container-highest mt-1 w-full rounded-xl px-4 py-3 font-medium ring-1 ring-border transition"
							/>
						</div>

						<div class="space-y-1.5 md:col-span-2">
							<label for="register-email" class="text-on-surface-variant px-1 text-sm font-bold"
								>Email Address</label
							>
							<input
								id="register-email"
								type="email"
								placeholder="email@example.com"
								bind:value={fields.email}
								onblur={() => handleBlur('email')}
								oninput={() => handleInput('email')}
								class="bg-surface-container-highest mt-1 w-full rounded-xl px-4 py-3 font-medium ring-1 ring-border transition
									{errors.email && touched.email ? 'ring-2 ring-red-400' : ''}"
							/>
							{#if errors.email && touched.email}
								<p class="px-1 text-xs text-red-500">{errors.email}</p>
							{/if}
						</div>

						<div class="space-y-1.5">
							<label for="register-username" class="text-on-surface-variant px-1 text-sm font-bold"
								>Username</label
							>
							<input
								id="register-username"
								type="text"
								placeholder="username"
								bind:value={fields.username}
								onblur={() => handleBlur('username')}
								oninput={() => handleInput('username')}
								class="bg-surface-container-highest mt-1 w-full rounded-xl px-4 py-3 font-medium ring-1 ring-border transition
									{errors.username && touched.username ? 'ring-2 ring-red-400' : ''}"
							/>
							{#if errors.username && touched.username}
								<p class="px-1 text-xs text-red-500">{errors.username}</p>
							{/if}
						</div>

						<div class="space-y-1.5">
							<label for="register-password" class="text-on-surface-variant px-1 text-sm font-bold"
								>Password</label
							>
							<div class="relative mt-1">
								<input
									id="register-password"
									type={showPassword ? 'text' : 'password'}
									placeholder="Password"
									bind:value={fields.password}
									onblur={() => handleBlur('password')}
									oninput={() => handleInput('password')}
									class="bg-surface-container-highest w-full rounded-xl px-4 py-3 pr-12 font-medium ring-1 ring-border transition
										{errors.password && touched.password ? 'ring-2 ring-red-400' : ''}"
								/>
								<button
									type="button"
									onclick={() => (showPassword = !showPassword)}
									class="absolute top-1/2 right-3 -translate-y-1/2 text-muted-foreground transition hover:text-foreground"
									aria-label={showPassword ? 'Hide password' : 'Show password'}
								>
									{#if showPassword}
										<EyeOff size={18} />
									{:else}
										<Eye size={18} />
									{/if}
								</button>
							</div>
							{#if errors.password && touched.password}
								<p class="px-1 text-xs text-red-500">{errors.password}</p>
							{/if}
						</div>
					</div>

					<div class="flex items-center justify-between pt-2">
						<a href={resolve('/login')} class="text-sm font-semibold text-primary hover:underline">
							Already have an account?
						</a>
						<button
							type="button"
							disabled={step1Checking}
							onclick={() => void nextStep()}
							class="text-on-primary rounded-full bg-primary px-6 py-2.5 text-sm font-bold transition hover:cursor-pointer hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-60"
						>
							{step1Checking ? 'Checking…' : 'Continue'}
						</button>
					</div>
				{:else}
					{#if employeeLinkOffered}
					<div class="space-y-5">
						<p class="text-on-surface-variant text-sm leading-relaxed">
							This matches a staff work email. Enter your <strong>employee</strong> password twice to
							link. We copy your name, phone, and address from staff records.
						</p>

						<div class="space-y-1.5">
							<label
								for="register-employee-link-password"
								class="text-on-surface-variant px-1 text-sm font-bold">Employee account password</label
							>
							<div class="relative mt-1">
								<input
									id="register-employee-link-password"
									type={showEmployeeLinkPassword ? 'text' : 'password'}
									autocomplete="current-password"
									placeholder="Staff sign-in password"
									bind:value={fields.employeeLinkPassword}
									onblur={() => handleBlur('employeeLinkPassword')}
									oninput={() => handleInput('employeeLinkPassword')}
									class="bg-surface-container-highest w-full rounded-xl px-4 py-3 pr-12 font-medium ring-1 ring-border transition
										{errors.employeeLinkPassword && touched.employeeLinkPassword
											? 'ring-2 ring-red-400'
											: ''}"
								/>
								<button
									type="button"
									onclick={() => (showEmployeeLinkPassword = !showEmployeeLinkPassword)}
									class="absolute top-1/2 right-3 -translate-y-1/2 text-muted-foreground transition hover:text-foreground"
									aria-label={showEmployeeLinkPassword ? 'Hide password' : 'Show password'}
								>
									{#if showEmployeeLinkPassword}
										<EyeOff size={18} />
									{:else}
										<Eye size={18} />
									{/if}
								</button>
							</div>
							{#if errors.employeeLinkPassword && touched.employeeLinkPassword}
								<p class="px-1 text-xs text-red-500">{errors.employeeLinkPassword}</p>
							{/if}
						</div>

						<div class="space-y-1.5">
							<label
								for="register-employee-link-confirm"
								class="text-on-surface-variant px-1 text-sm font-bold">Confirm employee password</label
							>
							<div class="relative mt-1">
								<input
									id="register-employee-link-confirm"
									type={showEmployeeLinkPasswordConfirm ? 'text' : 'password'}
									autocomplete="new-password"
									placeholder="Re-enter staff password"
									bind:value={fields.employeeLinkPasswordConfirm}
									onblur={() => handleBlur('employeeLinkPasswordConfirm')}
									oninput={() => handleInput('employeeLinkPasswordConfirm')}
									class="bg-surface-container-highest w-full rounded-xl px-4 py-3 pr-12 font-medium ring-1 ring-border transition
										{errors.employeeLinkPasswordConfirm && touched.employeeLinkPasswordConfirm
											? 'ring-2 ring-red-400'
											: ''}"
								/>
								<button
									type="button"
									onclick={() =>
										(showEmployeeLinkPasswordConfirm = !showEmployeeLinkPasswordConfirm)}
									class="absolute top-1/2 right-3 -translate-y-1/2 text-muted-foreground transition hover:text-foreground"
									aria-label={showEmployeeLinkPasswordConfirm ? 'Hide password' : 'Show password'}
								>
									{#if showEmployeeLinkPasswordConfirm}
										<EyeOff size={18} />
									{:else}
										<Eye size={18} />
									{/if}
								</button>
							</div>
							{#if errors.employeeLinkPasswordConfirm && touched.employeeLinkPasswordConfirm}
								<p class="px-1 text-xs text-red-500">{errors.employeeLinkPasswordConfirm}</p>
							{/if}
						</div>
					</div>
					{:else}
					<div class="grid gap-5 md:grid-cols-2">
						<div class="space-y-1.5 md:col-span-2">
							<label for="register-phone" class="text-on-surface-variant px-1 text-sm font-bold"
								>Phone Number</label
							>
							<input
								id="register-phone"
								type="tel"
								placeholder="(403) 555-0100"
								bind:value={fields.phone}
								oninput={(e) => {
									fields.phone = FormValidationUtil.formatPhone(e.target.value);
									handleInput('phone');
								}}
								onblur={() => handleBlur('phone')}
								class="bg-surface-container-highest mt-1 w-full rounded-xl px-4 py-3 font-medium ring-1 ring-border transition
									{errors.phone && touched.phone ? 'ring-2 ring-red-400' : ''}"
							/>
							{#if errors.phone && touched.phone}
								<p class="px-1 text-xs text-red-500">{errors.phone}</p>
							{/if}
						</div>

						<div class="space-y-1.5 md:col-span-2">
							<label
								for="register-businessPhone"
								class="text-on-surface-variant px-1 text-sm font-bold"
							>
								Business Phone <span class="text-outline font-normal">(optional)</span>
							</label>
							<input
								id="register-businessPhone"
								type="tel"
								placeholder="(403) 555-0100"
								bind:value={fields.businessPhone}
								oninput={(e) => {
									fields.businessPhone = FormValidationUtil.formatPhone(e.target.value);
								}}
								class="bg-surface-container-highest mt-1 w-full rounded-xl px-4 py-3 font-medium ring-1 ring-border transition"
							/>
						</div>

						<div class="space-y-1.5 md:col-span-2">
							<label
								for="register-addressLine1"
								class="text-on-surface-variant px-1 text-sm font-bold">Address</label
							>
							<input
								id="register-addressLine1"
								type="text"
								placeholder="123 Main St"
								bind:value={fields.addressLine1}
								onblur={() => handleBlur('addressLine1')}
								oninput={() => handleInput('addressLine1')}
								class="bg-surface-container-highest mt-1 w-full rounded-xl px-4 py-3 font-medium ring-1 ring-border transition
        {errors.addressLine1 && touched.addressLine1 ? 'ring-2 ring-red-400' : ''}"
							/>
							{#if errors.addressLine1 && touched.addressLine1}
								<p class="px-1 text-xs text-red-500">{errors.addressLine1}</p>
							{/if}
						</div>

						<div class="space-y-1.5 md:col-span-2">
							<label
								for="register-addressLine2"
								class="text-on-surface-variant px-1 text-sm font-bold"
							>
								Address Line 2 <span class="text-outline font-normal">(optional)</span>
							</label>
							<input
								id="register-addressLine2"
								type="text"
								placeholder="Apt, suite, unit..."
								bind:value={fields.addressLine2}
								class="bg-surface-container-highest mt-1 w-full rounded-xl px-4 py-3 font-medium ring-1 ring-border transition"
							/>
						</div>

						<div class="space-y-1.5">
							<label for="register-city" class="text-on-surface-variant px-1 text-sm font-bold"
								>City</label
							>
							<input
								id="register-city"
								type="text"
								placeholder="Calgary"
								bind:value={fields.city}
								onblur={() => handleBlur('city')}
								oninput={() => handleInput('city')}
								class="bg-surface-container-highest mt-1 w-full rounded-xl px-4 py-3 font-medium ring-1 ring-border transition
									{errors.city && touched.city ? 'ring-2 ring-red-400' : ''}"
							/>
							{#if errors.city && touched.city}
								<p class="px-1 text-xs text-red-500">{errors.city}</p>
							{/if}
						</div>

						<div class="space-y-1.5">
							<label for="register-province" class="text-on-surface-variant px-1 text-sm font-bold"
								>Province</label
							>
							<select
								id="register-province"
								bind:value={fields.province}
								onblur={() => handleBlur('province')}
								onchange={() => handleInput('province')}
								class="mt-1 w-full rounded-xl bg-background px-4 py-3 font-medium text-foreground ring-1 ring-border transition
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

						<div class="space-y-1.5 md:col-span-2">
							<label
								for="register-postalCode"
								class="text-on-surface-variant px-1 text-sm font-bold">Postal Code</label
							>
							<input
								id="register-postalCode"
								type="text"
								inputmode="text"
								autocomplete="postal-code"
								placeholder="T2P 1J9"
								maxlength="7"
								value={fields.postalCode}
								oninput={(e) => {
									fields.postalCode = formatCanadianPostalInput(e.currentTarget.value);
									handleInput('postalCode');
								}}
								onblur={() => handleBlur('postalCode')}
								class="bg-surface-container-highest mt-1 w-full rounded-xl px-4 py-3 font-medium ring-1 ring-border transition
									{errors.postalCode && touched.postalCode ? 'ring-2 ring-red-400' : ''}"
							/>
							{#if errors.postalCode && touched.postalCode}
								<p class="px-1 text-xs text-red-500">{errors.postalCode}</p>
							{/if}
						</div>
					</div>
					{/if}

					{#if registrationSuccess}
						<div
							class="rounded-lg border border-green-200 bg-green-50 px-4 py-3 text-sm text-green-800"
						>
							Account created! A welcome email has been sent to {fields.email}.
						</div>
					{/if}

					<div class="flex items-center justify-between gap-3 pt-2">
						<button
							type="button"
							onclick={prevStep}
							class="rounded-full border border-border px-6 py-2.5 text-sm font-semibold transition hover:bg-muted"
						>
							Back
						</button>
						<button
							type="submit"
							class="text-on-primary rounded-full bg-primary px-6 py-2.5 text-sm font-bold transition hover:cursor-pointer hover:opacity-90"
						>
							Create Account
						</button>
					</div>

					<p class="text-center text-sm">
						Already have an account?
						<a href={resolve('/login')} class="font-semibold text-primary hover:underline"
							>Sign in</a
						>
					</p>
				{/if}
			</form>
		</div>
	</div>
</div>
