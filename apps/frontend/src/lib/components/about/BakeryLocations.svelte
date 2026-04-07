<script lang="ts">
	export let locations = [
		{
			id: 1,
			name: 'Downtown Bakery',
			address: '123 Main St, Cityville',
			hours: 'Mon-Sat: 7am - 6pm',
			phone: '(123) 456-7890'
		},
		{
			id: 2,
			name: 'Uptown Bakery',
			address: '456 Elm St, Cityville',
			hours: 'Mon-Sun: 8am - 5pm',
			phone: '(123) 555-0123'
		},
		{
			id: 3,
			name: 'Suburban Bakery',
			address: '789 Oak St, Townsville',
			hours: 'Tue-Sat: 7am - 4pm',
			phone: '(123) 987-6543'
		}
	];
</script>

<div class="mx-auto max-w-5xl px-4 py-12">
	<h2 class="mb-8 text-center text-3xl font-bold">Our Bakery Locations</h2>

	<div class="grid gap-6 sm:grid-cols-1 md:grid-cols-2 lg:grid-cols-3">
		{#each locations as loc (loc.name)}
			<div
				class="rounded-xl border border-gray-200 bg-white p-6 shadow-sm transition hover:scale-105 hover:shadow-lg"
			>
				<h3 class="mb-2 text-xl font-semibold">{loc.name}</h3>
				<p class="mb-1 text-sm text-gray-600">{loc.address}</p>
				<p class="mb-1 text-sm text-gray-600">
					<span class="font-medium">Hours:</span>
					{loc.hours}
				</p>
				<p class="text-sm text-gray-600"><span class="font-medium">Phone:</span> {loc.phone}</p>
			</div>
		{/each}
	</div>
</div>
