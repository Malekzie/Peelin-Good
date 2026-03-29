<script>
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

	function handleSignIn(event) {
		event.preventDefault();

		emailTouched = true;
		passwordTouched = true;

		emailError = validateEmail(email);
		passwordError = validatePassword(password);

		if (emailError || passwordError) return;

		alert('Signed in!');
	}
</script>

<main class="flex min-h-screen">
	<!-- Left Pane -->
	<section class="relative hidden items-center justify-center px-16 lg:flex lg:w-1/2">
		<!-- Background Image -->
		<img
			alt="Fresh Bread Background"
			class="absolute inset-0 h-full w-full object-cover"
			src="https://lh3.googleusercontent.com/aida-public/AB6AXuAp7DoGW1kgz-fo9vtN4Ruqxt4xlOaXS5mk-dSqcj0pi1Y_OQKr9CSZ7eddRTWiUnddslHrN2WrGUqxHXAvRUtXzUuLJZ5lxe0RFXrSfkJcTKc1CQEs9YcXQdL1-QWC8ZtCrvQQTQyQnDoyw-C4FefIsUEIyWDV1HboUpypXwSoEqRHif4fd8hnqRPxhi0jen_JY37Wb9_7yVaQHwULQuLXfO20DF3oTGx-wvZYeMmim9_oDJKuEz_bAI2mZ2MFWvP6Bqd_YwmLqRDR"
		/>

		<!-- Overlay -->
		<div class="absolute inset-0 bg-gradient-to-r from-black/40 via-black/5 to-transparent"></div>

		<!-- Soft glow circle -->
		<div class="absolute -bottom-20 -left-20 h-72 w-72 rounded-full bg-primary/20 blur-3xl"></div>

		<!-- Content -->
		<div class="relative z-10 w-full max-w-xl space-y-8 self-start pt-30 text-white">
			<!-- Brand -->
			<div class="flex items-center gap-4">
				<h1 class="text-7xl font-bold tracking-tight drop-shadow-lg">Peelin' Good</h1>
			</div>

			<!-- Headline -->
			<div class="space-y-4">
				<h2 class="text-4xl leading-tight font-extrabold">
					Fresh from our <span class="text-primary">hearth</span> to your home.
				</h2>
				<p class="max-w-md text-base text-white/80">
					Hand-kneaded, slow-proved, and crafted with artisanal precision.
				</p>
			</div>

			<!-- Social proof -->
			<div class="flex items-center gap-4 pt-2">
				<div class="flex -space-x-2"></div>
				<p class="text-sm font-medium text-white/90">5,000+ happy bakers</p>
			</div>
		</div>
	</section>

	<!-- Right Pane -->
	<section class="bg-surface flex flex-1 items-center justify-center px-6 py-12">
		<div class="w-full max-w-md space-y-8">
			<!-- Mobile Branding -->
			<div class="flex items-center gap-3 lg:hidden">
				<span class="material-symbols-outlined text-4xl text-primary">bakery_dining</span>
				<span class="text-2xl font-bold text-primary">Peelin' Good</span>
			</div>

			<!-- Header -->
			<div class="space-y-1">
				<h3 class="text-2xl font-bold">Welcome back</h3>
				<p class="text-sm text-muted-foreground">Sign in to your account</p>
			</div>

			<!-- OAuth -->
			<div class="grid gap-3 sm:grid-cols-2">
				<button class="btn-secondary flex items-center justify-center gap-2">Google</button>
				<button class="btn-secondary flex items-center justify-center gap-2">Microsoft</button>
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
							on:blur={() => {
								emailTouched = true;
								emailError = validateEmail(email);
							}}
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
							on:blur={() => {
								passwordTouched = true;
								passwordError = validatePassword(password);
							}}
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
