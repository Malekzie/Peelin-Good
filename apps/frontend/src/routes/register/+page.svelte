<script>
	function handleRegister(event) {
		event.preventDefault();

		alert('Created');
	}
</script>

<div class="bg-surface flex min-h-screen flex-col p-6 md:p-12 lg:p-20">
	<div class="mx-auto my-auto w-full max-w-md">
		<header class="mb-6 text-center">
			<h2 class="font-headline mb-3 text-4xl font-bold text-primary">Create Account</h2>
			<p class="text-on-surface-variant font-medium">Start your artisanal experience.</p>
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
							class="bg-surface-container-highest w-full rounded-xl px-6 py-4 font-medium"
						/>
					</div>

					<div class="space-y-1.5">
						<label class="text-on-surface-variant px-1 text-sm font-bold">Last Name</label>
						<input
							type="text"
							placeholder="Smith"
							class="bg-surface-container-highest w-full rounded-xl px-6 py-4 font-medium"
						/>
					</div>
				</div>

				<!-- Email -->
				<div class="space-y-1.5">
					<label class="text-on-surface-variant px-1 text-sm font-bold">Email Address</label>
					<input
						type="email"
						placeholder="email@example.com"
						class="bg-surface-container-highest w-full rounded-xl px-6 py-4 font-medium"
					/>
				</div>

				<!-- Username -->
				<div class="space-y-1.5">
					<label class="text-on-surface-variant px-1 text-sm font-bold">Username</label>
					<input
						type="text"
						placeholder="username"
						class="bg-surface-container-highest w-full rounded-xl px-6 py-4 font-medium"
					/>
				</div>

				<!-- Password -->
				<div class="space-y-1.5">
					<label class="text-on-surface-variant px-1 text-sm font-bold">Password</label>
					<input
						type="password"
						placeholder="password"
						class="bg-surface-container-highest w-full rounded-xl px-6 py-4 font-medium"
					/>
				</div>

				<!-- Phone -->
				<div class="space-y-1.5">
					<label class="text-on-surface-variant px-1 text-sm font-bold">Phone Number</label>
					<input
						type="tel"
						placeholder="403-555-0100"
						class="bg-surface-container-highest w-full rounded-xl px-6 py-4 font-medium"
					/>
				</div>

				<!-- Submit -->
				<button
					type="submit"
					class="text-on-primary mt-4 w-full rounded-full bg-primary py-5 text-lg font-bold hover:cursor-pointer"
				>
					Create Account
				</button>
			</form>
		</div>
	</div>
</div>
