<script>
	import { registerUser } from '$lib/services/auth.js';

	let fields = {
		firstName: '',
		lastName: '',
		email: '',
		username: '',
		password: '',
		phone: ''
	};

	let errors = {
		firstName: '',
		lastName: '',
		email: '',
		username: '',
		password: '',
		phone: ''
	};

	let touched = {
		firstName: false,
		lastName: false,
		email: false,
		username: false,
		password: false,
		phone: false
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

		Object.keys(fields).forEach((name) => {
			touched[name] = true;
			errors[name] = validateField(name, fields[name]);
		});

		const hasErrors = Object.values(errors).some((e) => e !== '');
		if (hasErrors) return;

		const { ok, data } = await registerUser(fields);

		if (!ok) {
			if (data.field) errors[data.field] = data.message;
			return;
		}

		window.location.href = '/login';
	}
</script>

<div class="bg-surface flex min-h-screen flex-col p-6 md:p-10 lg:p-16">
	<div class="mx-auto my-auto w-full max-w-md">
		<header class="mb-6 text-center">
			<h2 class="font-headline mb-3 text-4xl font-bold text-primary">Create Account</h2>
			<!-- <p class="text-on-surface-variant font-medium">Start your artisanal experience.</p> -->
		</header>

		<div
			class="bg-surface-container border-outline/30 rounded-3xl border p-8 shadow-[0_8px_30px_rgba(0,0,0,0.08)]"
		>
			<div class="bg-outline/20 mb-6 h-px w-full"></div>

			<form class="space-y-6" on:submit={handleRegister}>
				<!-- First + Last Name -->
				<div class="grid grid-cols-2 gap-4">
					<div class="space-y-1.5">
						<label class="text-on-surface-variant px-1 text-sm font-bold">First Name</label>
						<input
							type="text"
							placeholder="John"
							bind:value={fields.firstName}
							on:blur={() => handleBlur('firstName')}
							on:input={() => handleInput('firstName')}
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
							on:blur={() => handleBlur('lastName')}
							on:input={() => handleInput('lastName')}
							class="bg-surface-container-highest mt-1 w-full rounded-xl px-6 py-3 font-medium ring-1 ring-border transition
								{errors.lastName && touched.lastName ? 'ring-2 ring-red-400' : ''}"
						/>
						{#if errors.lastName && touched.lastName}
							<p class="px-1 text-xs text-red-500">{errors.lastName}</p>
						{/if}
					</div>
				</div>

				<!-- Email -->
				<div class="space-y-1.5">
					<label class="text-on-surface-variant px-1 text-sm font-bold">Email Address</label>
					<input
						type="email"
						placeholder="email@example.com"
						bind:value={fields.email}
						on:blur={() => handleBlur('email')}
						on:input={() => handleInput('email')}
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
						placeholder="Username"
						bind:value={fields.username}
						on:blur={() => handleBlur('username')}
						on:input={() => handleInput('username')}
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
						on:blur={() => handleBlur('password')}
						on:input={() => handleInput('password')}
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
						on:input={(e) => {
							fields.phone = formatPhone(e.target.value);
							handleInput('phone');
						}}
						on:blur={() => handleBlur('phone')}
						class="bg-surface-container-highest mt-1 w-full rounded-xl px-6 py-3 font-medium ring-1 ring-border transition
							{errors.phone && touched.phone ? 'ring-2 ring-red-400' : ''}"
					/>
					{#if errors.phone && touched.phone}
						<p class="px-1 text-xs text-red-500">{errors.phone}</p>
					{/if}
				</div>

				<!-- Submit -->
				<button
					type="submit"
					class="text-on-primary mt-4 w-full rounded-full bg-primary py-3.5 text-base font-bold transition hover:cursor-pointer hover:opacity-90"
				>
					Create Account
				</button>
			</form>
		</div>
	</div>
</div>
