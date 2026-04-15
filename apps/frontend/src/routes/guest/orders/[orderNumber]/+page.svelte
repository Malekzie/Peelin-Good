<script lang="ts">
	import { page } from '$app/state';
	import OrderTracking from '$lib/components/orders/OrderTracking.svelte';

	const orderNumber = page.params.orderNumber ?? '';
	// Second factor for guest order lookup: first try sessionStorage (set during checkout),
	// then fall back to the `?email=` query param (e.g. the confirmation page's Track link).
	const storedGuestEmail =
		typeof sessionStorage !== 'undefined' && orderNumber
			? sessionStorage.getItem(`guestOrderEmail:${orderNumber}`)
			: null;
	const guestEmail = storedGuestEmail ?? page.url.searchParams.get('email') ?? '';
</script>

<OrderTracking {orderNumber} {guestEmail} />
