<div class="bg-surface-container-low flex min-h-screen">
	<!-- Sidebar -->
	<aside
		class="hidden w-80 flex-col gap-4 border-r bg-[#fcf9f4] p-6 pt-24 text-sm font-medium md:flex dark:bg-stone-950"
	>
		<div class="px-2">
			<div class="flex items-center space-x-4">
				<div
					class="flex h-11 w-11 items-center justify-center rounded-full bg-primary text-white shadow-md"
				>
					<span class="material-symbols-outlined">bakery_dining</span>
				</div>
				<div>
					<h2 class="text-lg font-bold text-[#703210]">My Kitchen</h2>
					<p class="text-xs text-stone-500">Manage your artisanal hearth</p>
				</div>
			</div>
		</div>

		<nav class="mt-4 flex-1 space-y-1">
			<a class="flex items-center gap-3 rounded-full bg-[#703210] px-4 py-2.5 text-white" href="#">
				<span class="material-symbols-outlined">person</span>
				<span>Profile</span>
			</a>
			<a
				class="flex items-center gap-3 rounded-full px-4 py-2.5 text-stone-600 hover:bg-[#8e4e14]/10"
				href="#"
			>
				<span class="material-symbols-outlined">home_pin</span>
				<span>Addresses</span>
			</a>
			<a
				class="flex items-center gap-3 rounded-full px-4 py-2.5 text-stone-600 hover:bg-[#8e4e14]/10"
				href="#"
			>
				<span class="material-symbols-outlined">shield</span>
				<span>Security</span>
			</a>
		</nav>

		<div class="mt-auto space-y-2 border-t pt-4">
			<button class="w-full rounded-full bg-primary py-2.5 text-sm font-semibold text-white">
				New Order
			</button>
			<a
				class="flex items-center space-x-3 rounded-full px-4 py-2 text-stone-500 hover:bg-[#8e4e14]/10"
				href="#"
			>
				<span class="material-symbols-outlined">contact_support</span>
				<span>Support</span>
			</a>
			<a
				class="flex items-center space-x-3 rounded-full px-4 py-2 text-stone-500 hover:bg-[#8e4e14]/10"
				href="#"
			>
				<span class="material-symbols-outlined">logout</span>
				<span>Logout</span>
			</a>
		</div>
	</aside>

	<!-- Main Content -->
	<main class="flex-1 overflow-y-auto p-10">
		<div class="mx-auto max-w-5xl space-y-12">
			<!-- Baker's Profile Header -->
			<section
				class="border-surface-container-high flex flex-col justify-between gap-8 border-b pb-6 md:flex-row md:items-center"
			>
				<div class="flex items-center gap-8">
					<div class="group relative">
						<div
							class="bg-primary-container h-32 w-32 overflow-hidden rounded-full shadow-xl ring-4 ring-white"
						>
							<img
								class="h-full w-full object-cover"
								alt="A friendly baker smiling in a rustic kitchen"
								src="https://lh3.googleusercontent.com/aida-public/AB6AXuBV2-AJN7walhHrRpvxlgjC9-jUe9pdWhh1rtVW5BR7Wkvy0SHg6_bxgTtsxy5UkS7JnDMlA1UaY1EZiIFmRW1iV6jwpEp83m7Zq_u-tIeAWCWQ_vtQDTQ_xDf502HzFyL6tRYCuxW1oJ0RFSoPjAPwQEqh0oHz-7oXghPLFAfdb4ZZt2BsEL_oulq3yQAbtxMBQcEfgNv7U5OqV7m1B1oGLzJc53k-hE6t15ZKHvtgRIGbcdOHtaeMFZECYbE4nLIW_aQQR0WZ3mxS"
							/>
						</div>
						<button
							class="absolute right-1 bottom-1 rounded-full border border-stone-100 bg-white p-2.5 text-primary shadow-md transition-all hover:bg-primary hover:text-white"
						>
							<span class="material-symbols-outlined text-sm">edit</span>
						</button>
					</div>

					<div class="space-y-2">
						<h1 class="font-headline text-on-surface text-4xl font-black tracking-tight">
							Alex Thompson
						</h1>
						<p class="text-on-surface-variant font-medium">alex.the.baker@peelingood.com</p>
						<div class="mt-3 flex flex-wrap gap-3">
							<span
								class="rounded-full bg-[#ffdbcd] px-4 py-1 text-[10px] font-bold tracking-wider text-[#360f00] uppercase"
								>Baker's Club Elite</span
							>
							<span
								class="bg-surface-container-highest text-on-surface-variant rounded-full px-4 py-1 text-[10px] font-bold tracking-wider uppercase"
								>Pro Kneader</span
							>
						</div>
					</div>
				</div>
			</section>

			<!-- Bento Grid Layout -->
			<div class="grid grid-cols-1 gap-8 md:grid-cols-12">
				<!-- Account Overview (Bento Large) -->
				<div
					class="bg-surface-container-lowest soft-shadow space-y-10 rounded-xl border border-stone-100 p-8 md:col-span-8"
				>
					<div class="flex items-start justify-between">
						<div>
							<h3 class="font-headline text-on-surface text-2xl font-bold">Kitchen Profile</h3>
							<p class="text-on-surface-variant mt-1 text-sm">
								Artisan details and subscription status
							</p>
						</div>
						<span class="material-symbols-outlined text-outline text-3xl">bakery_dining</span>
					</div>

					<div class="grid grid-cols-1 gap-x-12 gap-y-8 sm:grid-cols-2">
						<div>
							<label class="text-outline text-[11px] font-bold tracking-widest uppercase"
								>Baker's Name</label
							>
							<p class="text-on-surface text-lg font-semibold">Alexander James Thompson</p>
						</div>
						<div>
							<label class="text-outline text-[11px] font-bold tracking-widest uppercase"
								>Country</label
							>
							<p class="text-on-surface text-lg font-semibold">United Kingdom</p>
						</div>
						<div>
							<label class="text-outline text-[11px] font-bold tracking-widest uppercase"
								>Joined Hearth</label
							>
							<p class="text-on-surface text-lg font-semibold">October 14, 2021</p>
						</div>
						<div>
							<label class="text-outline text-[11px] font-bold tracking-widest uppercase"
								>Last Bake</label
							>
							<p class="text-on-surface text-lg font-semibold">Today, 09:42 AM</p>
						</div>
					</div>

					<div
						class="mt-12 flex items-center justify-between rounded-xl border border-stone-200 bg-[#fcf9f4] p-6"
					>
						<div class="flex items-center gap-4">
							<div
								class="bg-secondary-container flex h-14 w-14 items-center justify-center rounded-full text-secondary"
							>
								<span class="material-symbols-outlined text-2xl">redeem</span>
							</div>
							<div>
								<p class="text-on-surface text-base font-bold">Monthly Treats Subscription</p>
								<p class="text-on-surface-variant text-sm">The Daily Crumb • £29.99/mo</p>
							</div>
						</div>
						<button
							class="text-sm font-bold text-primary decoration-primary/30 underline-offset-4 hover:underline"
							>Modify Plan</button
						>
					</div>
				</div>

				<!-- Active Devices (Bento Side) -->
				<div
					class="soft-shadow flex flex-col space-y-6 rounded-xl border border-stone-100 bg-white p-8 md:col-span-4"
				>
					<h3 class="font-headline text-on-surface mb-4 text-xl font-bold">Suggested Items</h3>

					<div class="flex-1 space-y-4">
						<div
							class="hover:bg-surface-container-low flex items-center justify-between rounded-xl p-4 transition-all duration-300"
						>
							<div class="flex items-center gap-4"></div>
						</div>
						<div
							class="hover:bg-surface-container-low flex items-center justify-between rounded-xl p-4 transition-all duration-300"
						>
							<div class="flex items-center gap-4"></div>
						</div>
						<div
							class="hover:bg-surface-container-low flex items-center justify-between rounded-xl p-4 transition-all duration-300"
						>
							<div class="flex items-center gap-4"></div>
						</div>
					</div>

					<button
						class="border-outline-variant mt-6 w-full rounded-full border py-3.5 text-xs font-bold tracking-widest text-secondary uppercase transition-colors hover:bg-secondary/5"
					>
						All Products
					</button>
				</div>

				<!-- Security & Tools -->
				<div class="grid grid-cols-1 gap-8 md:col-span-12 md:grid-cols-3">
					<!-- Security Card -->
					<div class="soft-shadow space-y-4 rounded-xl border border-stone-100 bg-white p-7">
						<div
							class="bg-secondary-container/50 mb-4 flex h-12 w-12 items-center justify-center rounded-full text-secondary"
						>
							<span class="material-symbols-outlined text-2xl">security</span>
						</div>
						<h4 class="font-headline text-on-surface text-xl font-bold">Kitchen Security</h4>
						<p class="text-on-surface-variant text-sm leading-relaxed">
							Your kitchen is safe and warm. Score: 92/100. Update password soon.
						</p>
						<button
							class="group flex items-center text-xs font-bold tracking-widest text-secondary uppercase"
						>
							Secure Now
							<span
								class="material-symbols-outlined ml-1.5 text-sm transition-transform group-hover:translate-x-1"
								>chevron_right</span
							>
						</button>
					</div>

					<!-- Preferences Card -->
					<div class="soft-shadow space-y-4 rounded-xl border border-stone-100 bg-white p-7">
						<div
							class="mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-[#f0ede8] text-[#703210]"
						>
							<span class="material-symbols-outlined text-2xl">auto_awesome</span>
						</div>
						<h4 class="font-headline text-on-surface text-xl font-bold">Bakery Mood</h4>
						<p class="text-on-surface-variant text-sm leading-relaxed">
							Personalize your dashboard theme, notifications, and oven units.
						</p>
						<button
							class="group flex items-center text-xs font-bold tracking-widest text-primary uppercase"
						>
							Set Mood
							<span
								class="material-symbols-outlined ml-1.5 text-sm transition-transform group-hover:translate-x-1"
								>chevron_right</span
							>
						</button>
					</div>

					<!-- Data & Privacy Card -->
					<div class="soft-shadow space-y-4 rounded-xl border border-stone-100 bg-white p-7">
						<div
							class="bg-primary-container mb-4 flex h-12 w-12 items-center justify-center rounded-full text-primary"
						>
							<span class="material-symbols-outlined text-2xl">cookie</span>
						</div>
						<h4 class="font-headline text-on-surface text-xl font-bold">Recipe Privacy</h4>
						<p class="text-on-surface-variant text-sm leading-relaxed">
							Manage how we use your data to recommend new seasonal treats.
						</p>
						<button
							class="group flex items-center text-xs font-bold tracking-widest text-primary uppercase"
						>
							Privacy Settings
							<span
								class="material-symbols-outlined ml-1.5 text-sm transition-transform group-hover:translate-x-1"
								>chevron_right</span
							>
						</button>
					</div>
				</div>
			</div>
		</div>
	</main>
</div>
