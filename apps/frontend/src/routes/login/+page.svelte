<script>
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { page } from '$app/state';
	import { Eye, EyeOff } from '@lucide/svelte';
	import { loginUser } from '$lib/services/auth.js';
	import { user } from '$lib/stores/authStore';

	let identifier = '';
	let password = '';

	let emailError = '';
	let passwordError = '';

	let emailTouched = false;
	let passwordTouched = false;
	let showPassword = false;

	function validateIdentifier(value) {
		if (!value.trim()) return 'Email or username is required.';
		return '';
	}

	function validatePassword(value) {
		if (!value) return 'Password is required.';
		return '';
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

	async function handleSignIn(event) {
		event.preventDefault();

		emailTouched = true;
		passwordTouched = true;

		emailError = validateIdentifier(identifier);
		passwordError = validatePassword(password);

		if (emailError || passwordError) return;

		const { ok, message } = await loginUser(identifier, password);

		if (!ok) {
			emailError = message ?? 'Invalid email or password.';
			passwordError = ' ';
			return;
		}

		const redirectTo = page.url.searchParams.get('redirectTo');
		goto(resolve(redirectTo ?? getDefaultPostAuthRoute($user?.role)));
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
				<a
					href={resolve('/oauth2/authorization/google')}
					class="flex items-center justify-center gap-3 rounded-full border border-border bg-white px-4 py-3 text-sm font-semibold text-gray-700 shadow-sm transition hover:cursor-pointer hover:bg-gray-50 hover:shadow-md active:scale-[0.98]"
				>
					<img src="/images/google_logo.svg" alt="Google" class="h-5 w-5" />
					Google
				</a>
				<a
					href={resolve('/oauth2/authorization/microsoft')}
					class="flex items-center justify-center gap-3 rounded-full border border-border bg-white px-4 py-3 text-sm font-semibold text-gray-700 shadow-sm transition hover:cursor-pointer hover:bg-gray-50 hover:shadow-md active:scale-[0.98]"
				>
					<img src="/images/microsoft-icon.svg" alt="Microsoft" class="h-5 w-5" />
					Microsoft
				</a>
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
					<!-- Email or Username -->
					<div class="space-y-2">
						<label
							for="login-email"
							class="block text-xs font-semibold tracking-wide text-primary uppercase"
						>
							Email or Username
						</label>
						<input
							id="login-email"
							class="input w-full rounded-md border border-border p-3 transition focus:border-primary focus:ring-2 focus:ring-primary/40 focus:outline-none
								{emailError && emailTouched ? 'border-red-400 ring-2 ring-red-400' : ''}"
							type="text"
							placeholder="you@example.com or username"
							bind:value={identifier}
							oninput={() => {
								if (emailTouched) emailError = validateIdentifier(identifier);
							}}
						/>
						{#if emailError && emailTouched}
							<p class="text-xs text-red-500">{emailError}</p>
						{/if}
					</div>

					<!-- Password -->
					<div class="space-y-2">
						<div class="flex items-center justify-between">
							<label
								for="login-password"
								class="block text-xs font-semibold tracking-wide text-primary uppercase"
							>
								Password
							</label>
						</div>
						<div class="relative">
							<input
								id="login-password"
								class="input w-full rounded-md border border-border p-3 pr-12 transition focus:border-primary focus:ring-2 focus:ring-primary/40 focus:outline-none
									{passwordError && passwordTouched ? 'border-red-400 ring-2 ring-red-400' : ''}"
								type={showPassword ? 'text' : 'password'}
								placeholder="••••••••"
								bind:value={password}
								oninput={() => {
									if (passwordTouched) passwordError = validatePassword(password);
								}}
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
						<a
							class="text-xs text-primary hover:underline"
							href={resolve('/login/recover-password')}>Forgot?</a
						>
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
