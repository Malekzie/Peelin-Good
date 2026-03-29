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
					<span>Bakery</span>
				</div>
				<div>
					<h2 class="text-lg font-bold text-[#703210]">My Profile</h2>
					<p class="text-xs text-stone-500">Manage your bakery account</p>
				</div>
			</div>
		</div>

		<nav class="mt-4 flex-1 space-y-1">
			<a class="flex items-center gap-3 rounded-full bg-[#703210] px-4 py-2.5 text-white" href="#">
				<span>Profile</span>
			</a>
			<a
				class="flex items-center gap-3 rounded-full px-4 py-2.5 text-stone-600 hover:bg-[#8e4e14]/10"
				href="#"
			>
				<span>Orders</span>
			</a>
			<a
				class="flex items-center gap-3 rounded-full px-4 py-2.5 text-stone-600 hover:bg-[#8e4e14]/10"
				href="#"
			>
				<span>Preferences</span>
			</a>
		</nav>

		<div class="mt-auto space-y-2 border-t pt-4">
			<button
				class="w-full rounded-full bg-primary py-2.5 text-sm font-semibold text-white hover:cursor-pointer hover:bg-primary/90"
			>
				New Order
			</button>
			<a
				class="flex items-center space-x-3 rounded-full px-4 py-2 text-stone-500 hover:bg-[#8e4e14]/10"
				href="#"
			>
				<span>Support</span>
			</a>
			<a
				class="flex items-center space-x-3 rounded-full px-4 py-2 text-stone-500 hover:bg-[#8e4e14]/10"
				href="#"
			>
				<span>Logout</span>
			</a>
		</div>
	</aside>

	<!-- Main Content -->
	<main class="flex-1 overflow-y-auto p-10">
		<div class="mx-auto max-w-5xl space-y-12">
			<!-- Header -->
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
								alt="Customer profile picture"
								src="https://via.placeholder.com/150"
							/>
						</div>
						<button
							class="absolute right-1 bottom-1 rounded-full border border-stone-100 bg-white p-2.5 text-primary shadow-md"
						>
							<span>Edit</span>
						</button>
					</div>

					<div class="space-y-2">
						<h1 class="font-headline text-on-surface text-4xl font-black tracking-tight">
							John Chungus
						</h1>
						<p class="text-on-surface-variant font-medium">example@example.com</p>
						<div class="mt-3 flex flex-wrap gap-3">
							<span class="rounded-full bg-[#ffdbcd] px-4 py-1 text-[10px] font-bold uppercase">
								Loyalty Tier: Silver
							</span>
							<span
								class="bg-surface-container-highest rounded-full px-4 py-1 text-[10px] font-bold uppercase"
							>
								Frequent Buyer
							</span>
						</div>
					</div>
				</div>
			</section>

			<!-- Grid -->
			<div class="grid grid-cols-1 gap-8 md:grid-cols-12">
				<!-- Profile Info -->
				<div
					class="bg-surface-container-lowest soft-shadow space-y-10 rounded-xl border border-stone-100 p-8 md:col-span-8"
				>
					<div>
						<h3 class="font-headline text-on-surface text-2xl font-bold">Profile Info</h3>
						<p class="text-on-surface-variant mt-1 text-sm">
							Personal details and subscription status
						</p>
					</div>

					<div class="grid grid-cols-1 gap-x-12 gap-y-8 sm:grid-cols-2">
						<div>
							<label class="text-outline text-[11px] font-bold uppercase">Full Name</label>
							<p class="text-on-surface text-lg font-semibold">Alexander James Thompson</p>
						</div>
						<div>
							<label class="text-outline text-[11px] font-bold uppercase">Country</label>
							<p class="text-on-surface text-lg font-semibold">United Kingdom</p>
						</div>
						<div>
							<label class="text-outline text-[11px] font-bold uppercase">Joined</label>
							<p class="text-on-surface text-lg font-semibold">October 14, 2021</p>
						</div>
						<div>
							<label class="text-outline text-[11px] font-bold uppercase">Last Order</label>
							<p class="text-on-surface text-lg font-semibold">Today, 09:42 AM</p>
						</div>
					</div>

					<!-- Points -->
					<div class="rounded-xl border border-stone-200 bg-[#fcf9f4] px-6 pt-2 pb-6">
						<div class="mb-2 flex justify-center">
							<div class="flex h-16 w-16 items-center justify-center rounded-full text-secondary">
								<h2 class="text-2xl whitespace-nowrap text-black">Loyalty Points</h2>
							</div>
						</div>

						<div class="mt-1 flex items-center justify-between">
							<div class="flex flex-col">
								<p class="text-on-surface text-lg font-bold">Your Points</p>
								<p class="text-sm text-stone-800">1,250 pts</p>
							</div>

							<button
								class="text-sm font-bold text-primary underline-offset-4 hover:cursor-pointer hover:underline"
							>
								View your points
							</button>
						</div>
					</div>
				</div>

				<!-- Recommended -->
				<div
					class="soft-shadow flex flex-col space-y-6 rounded-xl border border-stone-100 bg-white p-8 md:col-span-4"
				>
					<h3 class="font-headline text-on-surface mb-4 text-xl font-bold">Recommended for You</h3>

					<div class="flex-1 space-y-4">
						<div class="rounded-xl p-4">Croissant</div>
						<div class="rounded-xl p-4">Sourdough Bread</div>
						<div class="rounded-xl p-4">Chocolate Tart</div>
					</div>

					<button class="mt-6 w-full rounded-full border py-3.5 text-xs font-bold uppercase">
						View All Products
					</button>
				</div>

				<!-- Bottom Cards -->
				<div class="grid grid-cols-1 gap-8 md:col-span-12 md:grid-cols-3">
					<div class="soft-shadow space-y-4 rounded-xl border p-7">
						<div class="flex h-12 w-12 items-center justify-center rounded-full">
							<span>Security</span>
						</div>
						<h4 class="text-xl font-bold">Account Security</h4>
						<p class="text-sm">Your account is secure. Score: 92/100.</p>
						<button class="text-xs font-bold uppercase">Update Now →</button>
					</div>

					<div class="soft-shadow space-y-4 rounded-xl border p-7">
						<div class="flex h-12 w-12 items-center justify-center rounded-full">
							<span>Preferences</span>
						</div>
						<h4 class="text-xl font-bold">Preferences</h4>
						<p class="text-sm">Set your favorite baked goods and delivery options.</p>
						<button class="text-xs font-bold uppercase">Manage →</button>
					</div>

					<div class="soft-shadow space-y-4 rounded-xl border p-7">
						<div class="flex h-12 w-12 items-center justify-center rounded-full">
							<span>Privacy</span>
						</div>
						<h4 class="text-xl font-bold">Privacy Settings</h4>
						<p class="text-sm">Manage how we use your data.</p>
						<button class="text-xs font-bold uppercase">Update Privacy →</button>
					</div>
				</div>
			</div>
		</div>
	</main>
</div>
