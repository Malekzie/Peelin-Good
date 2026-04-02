<script>
	export let profile;
</script>

<!-- TODO Add an edit button for profile, have a "create new password" option -->

<!-- Profile Info -->
<div
	class="bg-surface-container-lowest soft-shadow space-y-10 rounded-xl border border-stone-100 p-8 md:col-span-8"
>
	<div>
		<h3 class="font-headline text-on-surface text-2xl font-bold">Profile Info</h3>
		<p class="text-on-surface-variant mt-1 text-sm">Personal details and subscription status</p>
	</div>

	<div class="grid grid-cols-1 gap-x-12 gap-y-8 sm:grid-cols-2">
		<div>
			<label class="text-outline text-[11px] font-bold uppercase">Full Name</label>
			<p class="text-on-surface text-lg font-semibold">
				{profile.firstName}
				{profile.middleInitial ? profile.middleInitial + '. ' : ''}{profile.lastName}
			</p>
		</div>
		<div>
			<label class="text-outline text-[11px] font-bold uppercase">Phone</label>
			<p class="text-on-surface text-lg font-semibold">{profile.phone ?? '-'}</p>
		</div>
		<div>
			<label class="text-outline text-[11px] font-bold uppercase">Email</label>
			<p class="text-on-surface text-lg font-semibold">{profile.email}</p>
		</div>
		<div>
			<label class="text-outline text-[11px] font-bold uppercase">Username</label>
			<p class="text-on-surface text-lg font-semibold">{profile.username}</p>
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
				<p class="text-sm text-stone-800">{profile.rewardBalance.toLocaleString()}</p>
			</div>

			<button
				class="text-sm font-bold text-primary underline-offset-4 hover:cursor-pointer hover:underline"
			>
				View your points
			</button>
		</div>
	</div>
</div>
