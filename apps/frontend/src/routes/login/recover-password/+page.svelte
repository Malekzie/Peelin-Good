<script>
	let email = '';
	let error = '';
	let touched = false;
	let success = false;

	function validateEmail(value) {
		if (!value.trim()) return 'Email is required.';
		if (!/^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(value)) return 'Enter a valid email address.';
		return '';
	}

	function handleBlur() {
		touched = true;
		error = validateEmail(email);
	}

	function handleInput() {
		if (touched) error = validateEmail(email);
	}

	async function handleSubmit(event) {
		event.preventDefault();

		touched = true;
		error = validateEmail(email);

		if (error) return;

		// MOCK backend call
		await new Promise((resolve) => setTimeout(resolve, 500));

		// Always show success message, even if email doesn't exist
		success = true;
	}
</script>

<div class="bg-surface flex min-h-screen flex-col p-6 md:p-10 lg:p-16">
	<div class="my-1/2 mx-auto w-full max-w-md">
		<header class="mb-6 text-center">
			<h2 class="font-headline mb-3 text-4xl font-bold text-primary">Forgot Password</h2>
			<p class="text-on-surface-variant font-medium">
				Enter your email and we'll send you a recovery link if it exists.
			</p>
		</header>

		<div
			class="bg-surface-container border-outline/30 rounded-3xl border p-8 shadow-[0_8px_30px_rgba(0,0,0,0.08)]"
		>
			<div class="bg-outline/20 mb-6 h-px w-full"></div>

			{#if success}
				<p class="text-center font-medium text-green-600">
					If an account exists with that email, a recovery link has been sent.
				</p>
			{:else}
				<form class="space-y-6" on:submit={handleSubmit}>
					<div class="space-y-1.5">
						<label for="recoveryEmailInput" class="text-on-surface-variant px-1 text-sm font-bold"
							>Email Address</label
						>
						<input
							id="recoveryEmailInput"
							type="email"
							placeholder="email@example.com"
							bind:value={email}
							on:blur={handleBlur}
							on:input={handleInput}
							class="bg-surface-container-highest mt-1 w-full rounded-xl px-6 py-3 font-medium ring-1 ring-border transition
								{error && touched ? 'ring-2 ring-red-400' : ''}"
						/>
						{#if error && touched}
							<p class="px-1 text-xs text-red-500">{error}</p>
						{/if}
					</div>

					<button
						type="submit"
						class="text-on-primary mt-4 w-full rounded-full bg-primary py-3.5 text-base font-bold transition hover:cursor-pointer hover:opacity-90"
					>
						Send Recovery Email
					</button>
				</form>
			{/if}
		</div>
	</div>
</div>
