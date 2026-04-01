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
		<a
			class="flex items-center gap-3 rounded-full bg-[#703210] px-4 py-2.5 text-white"
			href="/profile"
		>
			<span>Profile</span>
		</a>
		<a
			class="flex items-center gap-3 rounded-full px-4 py-2.5 text-stone-600 hover:bg-[#8e4e14]/10"
			href="/order/1" // TODO: Link to actual orders page
		>
			<span>Orders</span>
		</a>
		<a
			class="flex items-center gap-3 rounded-full px-4 py-2.5 text-stone-600 hover:bg-[#8e4e14]/10"
			href="/profile/preferences"
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
