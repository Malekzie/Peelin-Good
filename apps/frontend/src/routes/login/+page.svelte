<script>
	import { loginUser } from '$lib/services/auth.js';

	let email = '';
	let password = '';

	let emailError = '';
	let passwordError = '';

	let emailTouched = false;
	let passwordTouched = false;

	function validateEmail(value) {
		if (!value.trim()) return 'Email is required.';
		if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) return 'Enter a valid email address.';
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

		const { ok, data } = await loginUser(email, password);

		if (!ok) {
			emailError = 'Invalid email or password.';
			passwordError = ' ';
			return;
		}

		// localStorage.setItem('token', data.token);
		window.location.href = '/dashboard';
	}
</script>

<main class="flex min-h-screen">
	<!-- Left Pane -->
	<!-- <section class="relative hidden items-center justify-center px-16 lg:flex lg:w-1/2">
		<img
			alt="Fresh Bread Background"
			class="absolute inset-0 h-full w-full object-cover"
			src="/images/bread_background.png"
		/>
		<div class="absolute inset-0 bg-gradient-to-r from-black/40 via-black/5 to-transparent"></div>
		<div class="absolute -bottom-20 -left-20 h-72 w-72 rounded-full bg-primary/20 blur-3xl"></div>
		<div class="relative z-10 w-full max-w-xl space-y-8 self-start pt-30 text-white">
			<div class="flex items-center gap-4">
				<h1 class="text-7xl font-bold tracking-tight drop-shadow-lg">Peelin' Good</h1>
			</div>
			<div class="space-y-4">
				<h2 class="text-4xl leading-tight font-extrabold">
					Fresh from our <span class="text-primary">hearth</span> to your home.
				</h2>
				<p class="max-w-md text-base text-white/80">
					Hand-kneaded, slow-proved, and crafted with artisanal precision.
				</p>
			</div>
		</div>
	</section> -->

	<section class="relative hidden items-center justify-center bg-[#f6efe7] px-16 lg:flex lg:w-1/2">
		<div class="absolute inset-0 bg-[radial-gradient(circle_at_top,#f3e2cf,transparent_60%)]"></div>

		<!-- Content -->
		<div class="relative z-10 max-w-md space-y-6 text-neutral-800">
			<h1 class="text-5xl font-bold tracking-tight">Peelin' Good</h1>

			<h2 class="text-2xl leading-snug font-semibold">
				Fresh from our <span class="text-primary">hearth</span> to your home.
			</h2>

			<p class="text-sm text-neutral-600">
				Hand-kneaded, slow-proved, and crafted with care. Sign in to continue your daily bake
				ritual.
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
					<svg class="h-5 w-5 shrink-0" viewBox="0 0 24 24">
						<path
							fill="#4285F4"
							d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
						/>
						<path
							fill="#34A853"
							d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
						/>
						<path
							fill="#FBBC05"
							d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l3.66-2.84z"
						/>
						<path
							fill="#EA4335"
							d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
						/>
					</svg>
					Google
				</button>
				<button
					class="flex items-center justify-center gap-3 rounded-full border border-border bg-white px-4 py-3 text-sm font-semibold text-gray-700 shadow-sm transition hover:cursor-pointer hover:bg-gray-50 hover:shadow-md active:scale-[0.98]"
				>
					<svg class="h-5 w-5 shrink-0" viewBox="0 0 24 24">
						<path fill="#F25022" d="M1 1h10.5v10.5H1z" />
						<path fill="#7FBA00" d="M12.5 1H23v10.5H12.5z" />
						<path fill="#00A4EF" d="M1 12.5h10.5V23H1z" />
						<path fill="#FFB900" d="M12.5 12.5H23V23H12.5z" />
					</svg>
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
				<form class="space-y-6" on:submit={handleSignIn}>
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
							on:input={() => {
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
							<a class="text-xs text-primary hover:underline" href="#">Forgot?</a>
						</div>
						<input
							class="input w-full rounded-md border border-border p-3 transition focus:border-primary focus:ring-2 focus:ring-primary/40 focus:outline-none
								{passwordError && passwordTouched ? 'border-red-400 ring-2 ring-red-400' : ''}"
							type="password"
							placeholder="••••••••"
							bind:value={password}
							on:input={() => {
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
						<a href="/register" class="font-semibold text-primary hover:underline">Sign up</a>
					</p>
				</form>
			</div>
		</div>
	</section>
</main>
