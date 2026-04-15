<script lang="ts">
	import { onMount } from 'svelte';
	import { api } from '$lib/utils/apiClient';
	import { formatPriceCad, formatDiscountCad } from '$lib/utils/money';

	interface Props {
		orderNumber: string;
		orderId: string;
		publishableKey: string;
		clientSecret: string;
		paymentIntentId: string;
		subtotal: number;
		discount: number | null;
		deliveryFee: number | null;
		taxAmount: number | null;
		grandTotal: number | null;
		orderMethod: 'pickup' | 'delivery';
		onSuccess: (orderNumber: string) => void;
		onBack: () => void;
	}

	let {
		orderNumber,
		orderId,
		publishableKey,
		clientSecret,
		paymentIntentId,
		subtotal,
		discount,
		deliveryFee,
		taxAmount,
		grandTotal,
		orderMethod,
		onSuccess,
		onBack
	}: Props = $props();

	let paymentContainer = $state<HTMLDivElement | undefined>(undefined);
	let loading = $state(true);
	let error = $state('');

	// eslint-disable-next-line @typescript-eslint/no-explicit-any
	let stripeInstance: any = null;
	// eslint-disable-next-line @typescript-eslint/no-explicit-any
	let stripeElements: any = null;

	const discountAmount = $derived(Number(discount ?? 0));
	const deliveryAmount = $derived(Number(deliveryFee ?? 0));
	const taxAmountDisplay = $derived(Number(taxAmount ?? subtotal * 0.05));
	const totalDisplay = $derived(Number(grandTotal ?? subtotal + deliveryAmount + taxAmountDisplay));

	onMount(async () => {
		// eslint-disable-next-line @typescript-eslint/no-explicit-any
		if (!(window as any).Stripe) {
			await new Promise<void>((res, rej) => {
				const s = document.createElement('script');
				s.src = 'https://js.stripe.com/v3/';
				s.onload = () => res();
				s.onerror = () => rej(new Error('Failed to load Stripe.js'));
				document.head.appendChild(s);
			});
		}
		// eslint-disable-next-line @typescript-eslint/no-explicit-any
		stripeInstance = (window as any).Stripe(publishableKey);
		stripeElements = stripeInstance.elements({ clientSecret });
		stripeElements.create('payment').mount(paymentContainer);
		loading = false;
	});

	async function pay() {
		if (!stripeInstance || !stripeElements) return;
		loading = true;
		error = '';

		// If Stripe redirects the browser (some payment methods), the confirmation page
		// needs the guest email as a second-factor for the order lookup. sessionStorage
		// usually survives the round-trip, but we also pass it via the return URL as a
		// belt-and-suspenders fallback for stricter browser storage policies.
		const guestEmailForReturn =
			typeof sessionStorage !== 'undefined'
				? sessionStorage.getItem(`guestOrderEmail:${orderNumber}`)
				: null;
		const returnUrl = guestEmailForReturn
			? `${window.location.origin}/guest/orders/${orderNumber}/confirmation?email=${encodeURIComponent(guestEmailForReturn)}`
			: `${window.location.origin}/guest/orders/${orderNumber}/confirmation`;
		const { error: stripeError } = await stripeInstance.confirmPayment({
			elements: stripeElements,
			redirect: 'if_required',
			confirmParams: { return_url: returnUrl }
		});

		if (stripeError) {
			error = stripeError.message ?? 'Payment failed. Please try again.';
			loading = false;
			return;
		}

		try {
			await api.post(`/orders/${orderId}/confirm-stripe-payment`, { paymentIntentId });
		} catch (err) {
			error =
				err instanceof Error ? err.message : 'Could not confirm payment. Please contact support.';
			loading = false;
			return;
		}

		onSuccess(orderNumber);
	}
</script>

<div class="flex flex-col gap-6">
	<div class="rounded-xl border border-border bg-card p-6 shadow-sm">
		<p class="mb-1 text-xs font-semibold tracking-wider text-muted-foreground uppercase">Order</p>
		<p class="mb-4 font-semibold text-foreground">#{orderNumber}</p>
		<div class="flex flex-col gap-1 text-sm">
			<div class="flex justify-between text-muted-foreground">
				<span>Subtotal</span>
				<span>{formatPriceCad(subtotal + discountAmount)}</span>
			</div>
			{#if discountAmount > 0}
				<div class="flex justify-between text-muted-foreground">
					<span>Discount</span>
					<span>{formatDiscountCad(discount)}</span>
				</div>
			{/if}
			{#if orderMethod === 'delivery'}
				<div class="flex justify-between text-muted-foreground">
					<span>Delivery fee</span>
					{#if deliveryAmount === 0}
						<span class="font-medium text-green-600">Free</span>
					{:else}
						<span>{formatPriceCad(deliveryAmount)}</span>
					{/if}
				</div>
			{/if}
			<div class="flex justify-between text-muted-foreground">
				<span>Tax (5%)</span>
				<span>{formatPriceCad(taxAmountDisplay)}</span>
			</div>
			<hr class="my-1 border-border" />
			<div class="flex justify-between font-bold text-foreground">
				<span>Total</span>
				<span>{formatPriceCad(totalDisplay)}</span>
			</div>
		</div>
	</div>

	<section class="rounded-xl border border-border bg-card p-6 shadow-sm">
		<h2 class="mb-4 text-lg font-semibold text-foreground">Payment</h2>

		{#if loading}
			<p class="text-center text-sm text-muted-foreground">Loading payment form...</p>
		{/if}

		<div bind:this={paymentContainer} class="min-h-16"></div>

		{#if error}
			<p
				class="mt-3 rounded-lg border border-destructive bg-destructive/10 px-4 py-3 text-sm text-destructive"
			>
				{error}
			</p>
		{/if}

		<button
			type="button"
			onclick={pay}
			disabled={loading}
			class="mt-6 w-full rounded-lg bg-primary py-4 text-sm font-semibold text-primary-foreground transition-colors hover:opacity-90 disabled:opacity-60"
		>
			{loading ? 'Processing...' : 'Pay Now'}
		</button>
	</section>

	<button
		type="button"
		onclick={onBack}
		class="text-sm text-muted-foreground hover:text-foreground hover:underline"
	>
		← Back to order details
	</button>
</div>
