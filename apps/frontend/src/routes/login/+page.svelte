<script>
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { loginUser } from '$lib/services/auth.js';

	let email = '';
	let password = '';

	let emailError = '';
	let passwordError = '';

	let emailTouched = false;
	let passwordTouched = false;

	function validateEmail(value) {
		if (!value.trim()) return 'Email is required.';
		if (!/^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(value)) return 'Enter a valid email address.';
		return '';
	}

	function validatePassword(value) {
		if (!value) return 'Password is required.';
		if (value.length < 8) return 'Must be at least 8 characters.';
		return '';
	}

	async function handleSignIn(event) {
		event.preventDefault();

		emailTouched = true;
		passwordTouched = true;

		emailError = validateEmail(email);
		passwordError = validatePassword(password);

		if (emailError || passwordError) return;

		const { ok } = await loginUser(email, password);

		if (!ok) {
			emailError = 'Invalid email or password.';
			passwordError = ' ';
			return;
		}

		goto(resolve('/profile'));
	}
</script>

<main class="flex min-h-screen">
	<!-- Left Pane -->
	<section class="relative hidden items-center justify-center bg-[#f6efe7] px-16 lg:flex lg:w-1/2">
		<div class="absolute inset-0 bg-[radial-gradient(circle_at_top,#f3e2cf,transparent_60%)]"></div>

		<!-- Content -->
		<div class="relative z-10 max-w-md space-y-6 text-neutral-800">
			<h1 class="text-5xl font-bold tracking-tight">Peelin' Good</h1>

			<h2 class="text-2xl leading-snug font-semibold">
				Fresh from our <span class="text-primary">hearth</span> to your home.
			</h2>

			<p class="text-sm text-neutral-600">
				Hand-kneaded, slow-proved, and crafted with care. Sign in to see our handmade baked goods.
			</p>
		</div>
	</section>

	<!-- Right Pane -->
	<section class="bg-surface flex flex-1 items-center justify-center px-6 py-12">
		<div class="w-full max-w-md space-y-8">
			<!-- Mobile Branding -->
			<div class="flex items-center gap-3 lg:hidden">
				<span class="text-2xl font-bold text-primary">Peelin' Good</span>
			</div>

			<!-- Header -->
			<div class="space-y-1">
				<h3 class="text-2xl font-bold">Welcome back</h3>
				<p class="text-sm text-muted-foreground">Sign in to your account</p>
			</div>

			<!-- OAuth -->
			<div class="grid gap-3 sm:grid-cols-2">
				<button
					class="flex items-center justify-center gap-3 rounded-full border border-border bg-white px-4 py-3 text-sm font-semibold text-gray-700 shadow-sm transition hover:cursor-pointer hover:bg-gray-50 hover:shadow-md active:scale-[0.98]"
				>
					<img src="/images/google_logo.svg" alt="Google" class="h-5 w-5" />
					Google
				</button>
				<button
					class="flex items-center justify-center gap-3 rounded-full border border-border bg-white px-4 py-3 text-sm font-semibold text-gray-700 shadow-sm transition hover:cursor-pointer hover:bg-gray-50 hover:shadow-md active:scale-[0.98]"
				>
					<img src="/images/microsoft-icon.svg" alt="Microsoft" class="h-5 w-5" />
					Microsoft
				</button>
			</div>

			<!-- Divider -->
			<div class="flex items-center gap-4">
				<div class="h-px flex-1 bg-border"></div>
				<span class="text-xs tracking-widest text-muted-foreground uppercase">or</span>
				<div class="h-px flex-1 bg-border"></div>
			</div>

			<!-- Form Card -->
			<div class="bg-surface-container rounded-2xl border border-border p-6 shadow-sm">
				<form class="space-y-6" onsubmit={handleSignIn}>
					<!-- Email -->
					<div class="space-y-2">
						<label class="block text-xs font-semibold tracking-wide text-primary uppercase">
							Email Address
						</label>
						<input
							class="input w-full rounded-md border border-border p-3 transition focus:border-primary focus:ring-2 focus:ring-primary/40 focus:outline-none
								{emailError && emailTouched ? 'border-red-400 ring-2 ring-red-400' : ''}"
							type="email"
							placeholder="you@example.com"
							bind:value={email}
							oninput={() => {
								if (emailTouched) emailError = validateEmail(email);
							}}
						/>
						{#if emailError && emailTouched}
							<p class="text-xs text-red-500">{emailError}</p>
						{/if}
					</div>

					<!-- Password -->
					<div class="space-y-2">
						<div class="flex items-center justify-between">
							<label class="block text-xs font-semibold tracking-wide text-primary uppercase">
								Password
							</label>
							<a
								class="text-xs text-primary hover:underline"
								href={resolve('/login/recover-password')}>Forgot?</a
							>
						</div>
						<input
							class="input w-full rounded-md border border-border p-3 transition focus:border-primary focus:ring-2 focus:ring-primary/40 focus:outline-none
								{passwordError && passwordTouched ? 'border-red-400 ring-2 ring-red-400' : ''}"
							type="password"
							placeholder="••••••••"
							bind:value={password}
							oninput={() => {
								if (passwordTouched) passwordError = validatePassword(password);
							}}
						/>
						{#if passwordError && passwordTouched}
							<p class="text-xs text-red-500">{passwordError}</p>
						{/if}
					</div>

					<!-- Remember -->
					<div class="flex items-center justify-between text-sm">
						<label class="flex items-center gap-2">
							<input type="checkbox" />
							Keep me signed in
						</label>
					</div>

					<!-- Submit -->
					<button
						type="submit"
						class="text-on-primary mt-2 w-full rounded-full bg-primary py-4 text-base font-bold transition hover:scale-[1.01] hover:cursor-pointer active:scale-[0.99]"
					>
						Sign In
					</button>

					<!-- Signup -->
					<p class="text-center text-sm">
						No account?
						<a href={resolve('/register')} class="font-semibold text-primary hover:underline"
							>Sign up</a
						>
					</p>
				</form>
			</div>
		</div>
	</section>
</main>
