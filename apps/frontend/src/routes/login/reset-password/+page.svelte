<script>
	import { page } from '$app/stores';
	import { goto } from '$app/navigation';
	import { Eye, EyeOff } from '@lucide/svelte';

	let password = '';
	let confirmPassword = '';
	let passwordError = '';
	let confirmError = '';
	let touchedPassword = false;
	let touchedConfirm = false;
	let loading = false;
	let success = false;
	let serverError = '';
	let showPassword = false;
	let showConfirm = false;

	const token = $page.url.searchParams.get('token');

	function validatePassword(value) {
		if (!value.trim()) return 'Password is required.';
		if (value.length < 8) return 'Must be at least 8 characters.';
		if (!/[A-Z]/.test(value)) return 'Must include an uppercase letter.';
		if (!/[0-9]/.test(value)) return 'Must include a number.';
		if (!/[^a-zA-Z0-9]/.test(value)) return 'Must include a special character.';
		return '';
	}

	function validateConfirm(value) {
		if (!value.trim()) return 'Please confirm your password.';
		if (value !== password) return 'Passwords do not match.';
		return '';
	}

	function handlePasswordBlur() {
		touchedPassword = true;
		passwordError = validatePassword(password);
	}

	function handleConfirmBlur() {
		touchedConfirm = true;
		confirmError = validateConfirm(confirmPassword);
	}

	async function handleSubmit(event) {
		event.preventDefault();
		touchedPassword = true;
		touchedConfirm = true;
		passwordError = validatePassword(password);
		confirmError = validateConfirm(confirmPassword);
		if (passwordError || confirmError) return;

		if (!token) {
			serverError = 'Invalid or missing reset token.';
			return;
		}

		loading = true;
		serverError = '';

		try {
			const res = await fetch('/api/v1/auth/reset-password', {
				method: 'POST',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({ token, newPassword: password })
			});

			if (!res.ok) {
				const data = await res.json().catch(() => ({}));
				serverError = data.message ?? 'Reset link is invalid or has expired.';
				return;
			}

			success = true;
			setTimeout(() => goto('/login'), 2500);
		} catch {
			serverError = 'Could not reach the server. Try again later.';
		} finally {
			loading = false;
		}
	}
</script>

<div class="bg-surface flex min-h-screen flex-col p-6 md:p-10 lg:p-16">
	<div class="my-1/2 mx-auto w-full max-w-md">
		<header class="mb-6 text-center">
			<h2 class="font-headline mb-3 text-4xl font-bold text-primary">Reset Password</h2>
			<p class="text-on-surface-variant font-medium">Enter your new password below.</p>
		</header>

		<div
			class="bg-surface-container border-outline/30 rounded-3xl border p-8 shadow-[0_8px_30px_rgba(0,0,0,0.08)]"
		>
			<div class="bg-outline/20 mb-6 h-px w-full"></div>

			{#if success}
				<p class="text-center font-medium text-green-600">
					Password reset successfully! Redirecting you to login...
				</p>
			{:else}
				{#if serverError}
					<p
						class="mb-4 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-center text-sm text-red-800"
					>
						{serverError}
					</p>
				{/if}

				<form class="space-y-6" onsubmit={handleSubmit}>
					<div class="space-y-1.5">
						<label for="passwordInput" class="text-on-surface-variant px-1 text-sm font-bold">
							New Password
						</label>
						<div class="relative mt-1">
							<input
								id="passwordInput"
								type={showPassword ? 'text' : 'password'}
								placeholder="At least 8 characters"
								bind:value={password}
								onblur={handlePasswordBlur}
								oninput={() => {
									if (touchedPassword) passwordError = validatePassword(password);
								}}
								class="bg-surface-container-highest w-full rounded-xl px-4 py-3 pr-12 font-medium ring-1 ring-border transition
									{passwordError && touchedPassword ? 'ring-2 ring-red-400' : ''}"
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
						{#if passwordError && touchedPassword}
							<p class="px-1 text-xs text-red-500">{passwordError}</p>
						{/if}
					</div>

					<div class="space-y-1.5">
						<label for="confirmInput" class="text-on-surface-variant px-1 text-sm font-bold">
							Confirm Password
						</label>
						<div class="relative mt-1">
							<input
								id="confirmInput"
								type={showConfirm ? 'text' : 'password'}
								placeholder="Repeat your new password"
								bind:value={confirmPassword}
								onblur={handleConfirmBlur}
								oninput={() => {
									if (touchedConfirm) confirmError = validateConfirm(confirmPassword);
								}}
								class="bg-surface-container-highest w-full rounded-xl px-4 py-3 pr-12 font-medium ring-1 ring-border transition
									{confirmError && touchedConfirm ? 'ring-2 ring-red-400' : ''}"
							/>
							<button
								type="button"
								onclick={() => (showConfirm = !showConfirm)}
								class="absolute top-1/2 right-3 -translate-y-1/2 text-muted-foreground transition hover:text-foreground"
								aria-label={showConfirm ? 'Hide password' : 'Show password'}
							>
								{#if showConfirm}
									<EyeOff size={18} />
								{:else}
									<Eye size={18} />
								{/if}
							</button>
						</div>
						{#if confirmError && touchedConfirm}
							<p class="px-1 text-xs text-red-500">{confirmError}</p>
						{/if}
					</div>

					<button
						type="submit"
						disabled={loading}
						class="text-on-primary mt-4 w-full rounded-full bg-primary py-3.5 text-base font-bold transition hover:cursor-pointer hover:opacity-90 disabled:opacity-60"
					>
						{loading ? 'Resetting...' : 'Reset Password'}
					</button>
				</form>
			{/if}
		</div>
	</div>
</div>
